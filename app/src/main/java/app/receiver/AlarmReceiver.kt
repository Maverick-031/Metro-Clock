package app.metroclock.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.metroclock.ClockApplication
import app.metroclock.MainActivity
import app.metroclock.data.AlarmEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

  companion object {

    /*
     * The versioned channel ID ensures that users who already have an older
     * alarm channel do not retain its previous sound or vibration settings.
     */
    const val CHANNEL_ID_ALARM =
      "metro_alarm_channel_v3"

    const val ACTION_ALARM_TRIGGER =
      "app.metroclock.ALARM_TRIGGER"

    const val ACTION_ALARM_SNOOZE =
      "app.metroclock.ALARM_SNOOZE"

    const val ACTION_ALARM_DISMISS =
      "app.metroclock.ALARM_DISMISS"

    const val EXTRA_ALARM_ID =
      "extra_alarm_id"

    const val EXTRA_ALARM_NAME =
      "extra_alarm_name"

    const val EXTRA_ALARM_HOUR =
      "extra_alarm_hour"

    const val EXTRA_ALARM_MINUTE =
      "extra_alarm_minute"

    const val EXTRA_SNOOZE_MINUTES =
      "extra_snooze_minutes"

    const val EXTRA_SKIP_CALENDAR =
      "extra_skip_calendar"

    const val EXTRA_SMART_SKIP =
      "extra_smart_skip"

    const val EXTRA_SOUND_URI =
      "extra_sound_uri"

    /**
     * Distinguishes the original scheduled alarm from a temporary snooze.
     *
     * Repeating alarms must be rescheduled when their normal occurrence fires,
     * but they must not be rescheduled again when a snooze occurrence fires.
     */
    const val EXTRA_IS_SNOOZE =
      "extra_is_snooze"

    private const val NOTIFICATION_ID_BASE =
      0x40000000

    private const val REQUEST_NAMESPACE_OPEN =
      0x10000000

    private const val REQUEST_NAMESPACE_SNOOZE =
      0x20000000

    private const val REQUEST_NAMESPACE_DISMISS =
      0x30000000
  }

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    val appContext = context.applicationContext

    val application =
      appContext as? ClockApplication ?: return

    /*
     * Trigger handling reads Room and DataStore and may execute Smart Skip
     * checks. goAsync() allows that suspend work to finish without blocking
     * the BroadcastReceiver main thread.
     */
    val pendingResult = goAsync()

    application.container.applicationScope.launch {
      try {
        when (intent.action) {
          ACTION_ALARM_TRIGGER -> {
            handleAlarmTrigger(
              context = appContext,
              application = application,
              intent = intent
            )
          }

          ACTION_ALARM_SNOOZE -> {
            handleAlarmSnooze(
              context = appContext,
              application = application,
              intent = intent
            )
          }

          ACTION_ALARM_DISMISS -> {
            handleAlarmDismiss(
              context = appContext,
              application = application,
              intent = intent
            )
          }

          else -> {
            /*
             * Ignore unknown internal actions.
             */
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        pendingResult.finish()
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Alarm trigger
  // ---------------------------------------------------------------------------

  private suspend fun handleAlarmTrigger(
    context: Context,
    application: ClockApplication,
    intent: Intent
  ) {
    val container = application.container

    val alarmId = intent.getLongExtra(
      EXTRA_ALARM_ID,
      0L
    )

    if (alarmId <= 0L) {
      return
    }

    val isSnooze =
      intent.getBooleanExtra(
        EXTRA_IS_SNOOZE,
        false
      )

    /*
     * Try to obtain the current Room entity. The entity is the authoritative
     * source because an alarm may have been edited after its PendingIntent was
     * originally created.
     */
    val storedAlarm =
      try {
        container.alarmRepository.allAlarms
          .first()
          .firstOrNull { alarm ->
            alarm.id == alarmId
          }
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }

    /*
     * A deleted or disabled alarm must not ring when an old PendingIntent
     * remains in the system.
     *
     * Snoozed alarms are allowed even if the original one-time alarm was marked
     * disabled after its normal occurrence fired.
     */
    if (
      !isSnooze &&
      (
        storedAlarm == null ||
          !storedAlarm.isEnabled
        )
    ) {
      cancelAlarmNotification(
        context = context,
        alarmId = alarmId
      )
      return
    }

    val alarmName =
      storedAlarm?.name
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: intent.getStringExtra(EXTRA_ALARM_NAME)
          ?.trim()
          ?.takeIf { it.isNotEmpty() }
        ?: "Alarm"

    val hour =
      (
        storedAlarm?.hour
          ?: intent.getIntExtra(
            EXTRA_ALARM_HOUR,
            0
          )
        ).coerceIn(0, 23)

    val minute =
      (
        storedAlarm?.minute
          ?: intent.getIntExtra(
            EXTRA_ALARM_MINUTE,
            0
          )
        ).coerceIn(0, 59)

    val snoozeMinutes =
      (
        storedAlarm?.snoozeMinutes
          ?: intent.getIntExtra(
            EXTRA_SNOOZE_MINUTES,
            10
          )
        ).coerceAtLeast(1)

    val shouldCheckCalendar =
      storedAlarm?.skipIfCalendarEvent
        ?: intent.getBooleanExtra(
          EXTRA_SKIP_CALENDAR,
          false
        )

    val shouldCheckSmartSkip =
      storedAlarm?.smartSkipLocation
        ?: intent.getBooleanExtra(
          EXTRA_SMART_SKIP,
          false
        )

    val soundUri =
      storedAlarm?.soundUri
        ?.trim()
        ?: intent.getStringExtra(EXTRA_SOUND_URI)
          ?.trim()
          .orEmpty()

    /*
     * Handle the original scheduled occurrence before Smart Skip evaluation.
     *
     * A skipped repeating occurrence must still schedule the next selected
     * weekday. Snooze occurrences must not create another normal repeat.
     */
    if (!isSnooze && storedAlarm != null) {
      handleNextAlarmOccurrence(
        application = application,
        alarm = storedAlarm
      )
    }

    val settings =
      container.settingsDataStore

    val allAlarmsDisabled =
      try {
        settings.allAlarmsDisabled.first()
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }

    if (allAlarmsDisabled) {
      container.soundPlayer.stopSound()

      cancelAlarmNotification(
        context = context,
        alarmId = alarmId
      )

      return
    }

    // -------------------------------------------------------------------------
    // Calendar Smart Skip
    // -------------------------------------------------------------------------

    if (shouldCheckCalendar) {
      val shouldSkipForCalendar =
        try {
          container.calendarHelper.hasAllDayEvent(
            context,
            System.currentTimeMillis()
          )
        } catch (e: SecurityException) {
          /*
           * Missing calendar permission must not cause a real alarm to be
           * silently skipped.
           */
          e.printStackTrace()
          false
        } catch (e: Exception) {
          e.printStackTrace()
          false
        }

      if (shouldSkipForCalendar) {
        container.soundPlayer.stopSound()

        cancelAlarmNotification(
          context = context,
          alarmId = alarmId
        )

        return
      }
    }

    // -------------------------------------------------------------------------
    // Location or Wi-Fi Smart Skip
    // -------------------------------------------------------------------------

    if (shouldCheckSmartSkip) {
      val shouldSkipForLocation =
        try {
          container.smartSkipManager
            .shouldSkipAlarmDueToLocation()
        } catch (e: SecurityException) {
          /*
           * Missing location permission must not silently suppress the alarm.
           */
          e.printStackTrace()
          false
        } catch (e: Exception) {
          e.printStackTrace()
          false
        }

      if (shouldSkipForLocation) {
        container.soundPlayer.stopSound()

        cancelAlarmNotification(
          context = context,
          alarmId = alarmId
        )

        return
      }
    }

    // -------------------------------------------------------------------------
    // Playback settings
    // -------------------------------------------------------------------------

    val shouldVibrate =
      try {
        settings.alarmVibrate.first()
      } catch (e: Exception) {
        e.printStackTrace()
        true
      }

    val silenceAfter =
      try {
        settings.alarmSilenceAfter.first()
      } catch (e: Exception) {
        e.printStackTrace()
        "10"
      }

    val gradualVolume =
      try {
        settings.alarmGradualVolume.first()
      } catch (e: Exception) {
        e.printStackTrace()
        "never"
      }

    /*
     * SoundPlayer is shared through ClockApplication -> AppContainer.
     *
     * AlarmReceiver, MainActivity, ClockViewModel, and TimerService therefore
     * all control the same playback instance.
     */
    container.soundPlayer.playAlarmSound(
      customUri = soundUri,
      vibrate = shouldVibrate,
      silenceAfterMinutes = silenceAfter,
      gradualVolumeSeconds = gradualVolume
    )

    createNotificationChannel(context)

    showAlarmNotification(
      context = context,
      alarmId = alarmId,
      alarmName = alarmName,
      hour = hour,
      minute = minute,
      snoozeMinutes = snoozeMinutes,
      soundUri = soundUri,
      skipIfCalendarEvent = shouldCheckCalendar,
      smartSkipLocation = shouldCheckSmartSkip
    )
  }

  /**
   * Updates scheduling state after a normal alarm occurrence fires.
   *
   * Repeating alarms schedule their next selected weekday.
   * One-time alarms are disabled after firing.
   */
  private suspend fun handleNextAlarmOccurrence(
    application: ClockApplication,
    alarm: AlarmEntity
  ) {
    val container = application.container

    if (alarm.repeatDays != 0) {
      try {
        container.alarmScheduler.schedule(alarm)
      } catch (e: Exception) {
        e.printStackTrace()
      }

      return
    }

    /*
     * One-time alarms remain in the database but become disabled. This allows
     * the user to enable the same alarm again later.
     */
    try {
      container.alarmRepository.updateAlarm(
        alarm.copy(isEnabled = false)
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // Snooze action
  // ---------------------------------------------------------------------------

  private suspend fun handleAlarmSnooze(
    context: Context,
    application: ClockApplication,
    intent: Intent
  ) {
    val alarmId = intent.getLongExtra(
      EXTRA_ALARM_ID,
      0L
    )

    if (alarmId <= 0L) {
      return
    }

    val container = application.container

    /*
     * Stop the same shared player used by the trigger path.
     */
    container.soundPlayer.stopSound()

    cancelAlarmNotification(
      context = context,
      alarmId = alarmId
    )

    val storedAlarm =
      try {
        container.alarmRepository.allAlarms
          .first()
          .firstOrNull { alarm ->
            alarm.id == alarmId
          }
      } catch (e: Exception) {
        e.printStackTrace()
        null
      }

    val alarmName =
      storedAlarm?.name
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: intent.getStringExtra(EXTRA_ALARM_NAME)
          ?.trim()
          ?.takeIf { it.isNotEmpty() }
        ?: "Alarm"

    val snoozeMinutes =
      intent.getIntExtra(
        EXTRA_SNOOZE_MINUTES,
        storedAlarm?.snoozeMinutes ?: 10
      ).coerceAtLeast(1)

    val hour =
      storedAlarm?.hour
        ?: intent.getIntExtra(
          EXTRA_ALARM_HOUR,
          0
        )

    val minute =
      storedAlarm?.minute
        ?: intent.getIntExtra(
          EXTRA_ALARM_MINUTE,
          0
        )

    val soundUri =
      storedAlarm?.soundUri
        ?: intent.getStringExtra(EXTRA_SOUND_URI)
          .orEmpty()

    val skipCalendar =
      storedAlarm?.skipIfCalendarEvent
        ?: intent.getBooleanExtra(
          EXTRA_SKIP_CALENDAR,
          false
        )

    val smartSkip =
      storedAlarm?.smartSkipLocation
        ?: intent.getBooleanExtra(
          EXTRA_SMART_SKIP,
          false
        )

    try {
      container.alarmScheduler.snooze(
        alarmId = alarmId,
        name = alarmName,
        snoozeMinutes = snoozeMinutes,
        hour = hour,
        minute = minute,
        soundUri = soundUri,
        skipIfCalendarEvent = skipCalendar,
        smartSkipLocation = smartSkip
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // Dismiss action
  // ---------------------------------------------------------------------------

  private fun handleAlarmDismiss(
    context: Context,
    application: ClockApplication,
    intent: Intent
  ) {
    val alarmId = intent.getLongExtra(
      EXTRA_ALARM_ID,
      0L
    )

    /*
     * The user explicitly selected Dismiss, so stop playback even if the
     * supplied alarm ID is malformed.
     */
    application.container.soundPlayer.stopSound()

    if (alarmId > 0L) {
      cancelAlarmNotification(
        context = context,
        alarmId = alarmId
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Notification
  // ---------------------------------------------------------------------------

  private fun showAlarmNotification(
    context: Context,
    alarmId: Long,
    alarmName: String,
    hour: Int,
    minute: Int,
    snoozeMinutes: Int,
    soundUri: String,
    skipIfCalendarEvent: Boolean,
    smartSkipLocation: Boolean
  ) {
    /*
     * Android 13 and newer require POST_NOTIFICATIONS. Playback has already
     * started, so missing notification permission must not crash the alarm.
     */
    if (
      Build.VERSION.SDK_INT >=
      Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val fullScreenIntent =
      Intent(
        context,
        MainActivity::class.java
      ).apply {
        flags =
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

        putExtra(
          MainActivity.EXTRA_DESTINATION,
          MainActivity.DEST_ALARM_TRIGGERED
        )

        putExtra(EXTRA_ALARM_ID, alarmId)
        putExtra(EXTRA_ALARM_NAME, alarmName)
        putExtra(EXTRA_ALARM_HOUR, hour)
        putExtra(EXTRA_ALARM_MINUTE, minute)
        putExtra(
          EXTRA_SNOOZE_MINUTES,
          snoozeMinutes
        )
      }

    val fullScreenPendingIntent =
      PendingIntent.getActivity(
        context,
        openRequestCode(alarmId),
        fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    /*
     * Include complete alarm data in the notification snooze action. Room is
     * still preferred, but these extras provide a safe fallback.
     */
    val snoozeIntent =
      Intent(
        context,
        AlarmReceiver::class.java
      ).apply {
        action = ACTION_ALARM_SNOOZE

        putExtra(EXTRA_ALARM_ID, alarmId)
        putExtra(EXTRA_ALARM_NAME, alarmName)
        putExtra(EXTRA_ALARM_HOUR, hour)
        putExtra(EXTRA_ALARM_MINUTE, minute)

        putExtra(
          EXTRA_SNOOZE_MINUTES,
          snoozeMinutes
        )

        putExtra(EXTRA_SOUND_URI, soundUri)

        putExtra(
          EXTRA_SKIP_CALENDAR,
          skipIfCalendarEvent
        )

        putExtra(
          EXTRA_SMART_SKIP,
          smartSkipLocation
        )
      }

    val snoozePendingIntent =
      PendingIntent.getBroadcast(
        context,
        snoozeActionRequestCode(alarmId),
        snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    val dismissIntent =
      Intent(
        context,
        AlarmReceiver::class.java
      ).apply {
        action = ACTION_ALARM_DISMISS
        putExtra(EXTRA_ALARM_ID, alarmId)
      }

    val dismissPendingIntent =
      PendingIntent.getBroadcast(
        context,
        dismissRequestCode(alarmId),
        dismissIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    val notification =
      NotificationCompat.Builder(
        context,
        CHANNEL_ID_ALARM
      )
        .setSmallIcon(
          android.R.drawable.ic_lock_idle_alarm
        )
        .setContentTitle(alarmName)
        .setContentText(
          formatAlarmNotificationText(
            hour = hour,
            minute = minute
          )
        )
        .setPriority(
          NotificationCompat.PRIORITY_MAX
        )
        .setCategory(
          NotificationCompat.CATEGORY_ALARM
        )
        .setVisibility(
          NotificationCompat.VISIBILITY_PUBLIC
        )
        .setFullScreenIntent(
          fullScreenPendingIntent,
          true
        )
        .setContentIntent(
          fullScreenPendingIntent
        )
        .setDeleteIntent(
          dismissPendingIntent
        )
        .setAutoCancel(false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .addAction(
          android.R.drawable.ic_popup_reminder,
          "Snooze",
          snoozePendingIntent
        )
        .addAction(
          android.R.drawable.ic_menu_close_clear_cancel,
          "Dismiss",
          dismissPendingIntent
        )
        .build()

    val notificationManager =
      context.getSystemService(
        Context.NOTIFICATION_SERVICE
      ) as NotificationManager

    try {
      notificationManager.notify(
        notificationIdFor(alarmId),
        notification
      )
    } catch (e: SecurityException) {
      /*
       * Notification or full-screen-intent access may be denied. Audio remains
       * active even when Android rejects the notification operation.
       */
      e.printStackTrace()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun createNotificationChannel(
    context: Context
  ) {
    if (
      Build.VERSION.SDK_INT <
      Build.VERSION_CODES.O
    ) {
      return
    }

    val notificationManager =
      context.getSystemService(
        Context.NOTIFICATION_SERVICE
      ) as NotificationManager

    /*
     * SoundPlayer owns all alarm audio and vibration. The notification channel
     * remains silent to prevent duplicate sound or competing vibration.
     */
    val channel =
      NotificationChannel(
        CHANNEL_ID_ALARM,
        "Metro alarms",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description =
          "Alarm alerts from MetroClock"

        lockscreenVisibility =
          NotificationCompat.VISIBILITY_PUBLIC

        enableLights(true)
        enableVibration(false)
        setSound(null, null)
        setShowBadge(true)
      }

    try {
      notificationManager.createNotificationChannel(
        channel
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun cancelAlarmNotification(
    context: Context,
    alarmId: Long
  ) {
    val notificationManager =
      context.getSystemService(
        Context.NOTIFICATION_SERVICE
      ) as NotificationManager

    try {
      notificationManager.cancel(
        notificationIdFor(alarmId)
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // Identity helpers
  // ---------------------------------------------------------------------------

  private fun stableAlarmCode(
    alarmId: Long
  ): Int {
    return alarmId.hashCode() and
      0x0FFFFFFF
  }

  private fun notificationIdFor(
    alarmId: Long
  ): Int {
    return NOTIFICATION_ID_BASE or
      stableAlarmCode(alarmId)
  }

  private fun openRequestCode(
    alarmId: Long
  ): Int {
    return REQUEST_NAMESPACE_OPEN or
      stableAlarmCode(alarmId)
  }

  private fun snoozeActionRequestCode(
    alarmId: Long
  ): Int {
    return REQUEST_NAMESPACE_SNOOZE or
      stableAlarmCode(alarmId)
  }

  private fun dismissRequestCode(
    alarmId: Long
  ): Int {
    return REQUEST_NAMESPACE_DISMISS or
      stableAlarmCode(alarmId)
  }

  private fun formatAlarmNotificationText(
    hour: Int,
    minute: Int
  ): String {
    return String.format(
      Locale.getDefault(),
      "Alarm ringing at %02d:%02d",
      hour.coerceIn(0, 23),
      minute.coerceIn(0, 59)
    )
  }
}
