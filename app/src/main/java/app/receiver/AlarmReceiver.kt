package app.metroclock.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.metroclock.ClockApplication
import app.metroclock.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

  companion object {

    /*
     * Versioned channel ID.
     *
     * Notification-channel sound and vibration behavior cannot always be
     * changed after the channel has already been created. A new ID ensures
     * that installations using the old channel do not keep its duplicate
     * notification vibration or sound behavior.
     */
    const val CHANNEL_ID_ALARM =
      "metro_alarm_channel_v2"

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

    private const val NOTIFICATION_ID_BASE = 5000

    private const val REQUEST_CODE_OPEN_BASE = 10_000
    private const val REQUEST_CODE_SNOOZE_BASE = 20_000
    private const val REQUEST_CODE_DISMISS_BASE = 30_000
  }

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    val appContext = context.applicationContext

    val application =
      appContext as? ClockApplication ?: return

    /*
     * Trigger handling reads DataStore and may call suspend Smart Skip logic.
     * goAsync() keeps the BroadcastReceiver pending while that asynchronous
     * work finishes.
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
             * Ignore unknown actions. The receiver is private to this
             * application, but validating the action prevents accidental
             * execution from malformed internal Intents.
             */
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        /*
         * Always complete the asynchronous broadcast, including error and
         * cancellation paths.
         */
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

    /*
     * Room-generated alarm IDs should be positive. Ignore malformed Intents
     * instead of playing an unidentifiable alarm.
     */
    if (alarmId <= 0L) {
      return
    }

    val alarmName =
      intent.getStringExtra(EXTRA_ALARM_NAME)
        ?.trim()
        ?.takeIf { name -> name.isNotEmpty() }
        ?: "Alarm"

    val hour = intent.getIntExtra(
      EXTRA_ALARM_HOUR,
      0
    ).coerceIn(0, 23)

    val minute = intent.getIntExtra(
      EXTRA_ALARM_MINUTE,
      0
    ).coerceIn(0, 59)

    val snoozeMinutes = intent.getIntExtra(
      EXTRA_SNOOZE_MINUTES,
      10
    ).coerceAtLeast(1)

    val shouldCheckCalendar = intent.getBooleanExtra(
      EXTRA_SKIP_CALENDAR,
      false
    )

    val shouldCheckSmartSkip = intent.getBooleanExtra(
      EXTRA_SMART_SKIP,
      false
    )

    val soundUri =
      intent.getStringExtra(EXTRA_SOUND_URI)
        ?.trim()
        .orEmpty()

    val settings = container.settingsDataStore

    /*
     * Read the actual current DataStore values. Do not use runBlocking in a
     * BroadcastReceiver because it blocks the receiver's main thread.
     */
    val allAlarmsDisabled =
      settings.allAlarmsDisabled.first()

    if (allAlarmsDisabled) {
      cancelAlarmNotification(
        context = context,
        alarmId = alarmId
      )
      return
    }

    /*
     * Calendar-based Smart Skip.
     */
    if (shouldCheckCalendar) {
      val hasAllDayEvent = try {
        container.calendarHelper.hasAllDayEvent(
          context,
          System.currentTimeMillis()
        )
      } catch (e: SecurityException) {
        /*
         * READ_CALENDAR may not have been granted. In that case, do not skip
         * the alarm.
         */
        e.printStackTrace()
        false
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }

      if (hasAllDayEvent) {
        cancelAlarmNotification(
          context = context,
          alarmId = alarmId
        )
        return
      }
    }

    /*
     * Location or Wi-Fi based Smart Skip.
     */
    if (shouldCheckSmartSkip) {
      val shouldSkipAlarm = try {
        container.smartSkipManager
          .shouldSkipAlarmDueToLocation()
      } catch (e: SecurityException) {
        /*
         * Location permission may be unavailable. Failure to evaluate Smart
         * Skip must not silently suppress a real alarm.
         */
        e.printStackTrace()
        false
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }

      if (shouldSkipAlarm) {
        cancelAlarmNotification(
          context = context,
          alarmId = alarmId
        )
        return
      }
    }

    val shouldVibrate =
      settings.alarmVibrate.first()

    val silenceAfter =
      settings.alarmSilenceAfter.first()

    val gradualVolume =
      settings.alarmGradualVolume.first()

    /*
     * Use the shared SoundPlayer from ClockApplication -> AppContainer.
     *
     * MainActivity and ClockViewModel receive this same instance, so
     * isSoundPlaying() can detect the audio started here.
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
      snoozeMinutes = snoozeMinutes
    )
  }

  // ---------------------------------------------------------------------------
  // Alarm snooze
  // ---------------------------------------------------------------------------

  private fun handleAlarmSnooze(
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

    val alarmName =
      intent.getStringExtra(EXTRA_ALARM_NAME)
        ?.trim()
        ?.takeIf { name -> name.isNotEmpty() }
        ?: "Alarm"

    val snoozeMinutes = intent.getIntExtra(
      EXTRA_SNOOZE_MINUTES,
      10
    ).coerceAtLeast(1)

    val container = application.container

    /*
     * Stop the exact same shared SoundPlayer used by the trigger path.
     */
    container.soundPlayer.stopSound()

    cancelAlarmNotification(
      context = context,
      alarmId = alarmId
    )

    try {
      container.alarmScheduler.snooze(
        alarmId,
        alarmName,
        snoozeMinutes
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // Alarm dismiss
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
     * Stop sound even if the alarm ID is malformed. The user explicitly
     * pressed Dismiss, so stopping active audio is the safest behavior.
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
    snoozeMinutes: Int
  ) {
    /*
     * Android 13 and newer require notification permission. Sound has already
     * started, so missing notification permission must not crash the receiver.
     */
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return
    }

    val fullScreenIntent =
      Intent(context, MainActivity::class.java).apply {
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
        requestCodeFor(
          base = REQUEST_CODE_OPEN_BASE,
          alarmId = alarmId
        ),
        fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    val snoozeIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_ALARM_SNOOZE

        putExtra(EXTRA_ALARM_ID, alarmId)
        putExtra(EXTRA_ALARM_NAME, alarmName)
        putExtra(
          EXTRA_SNOOZE_MINUTES,
          snoozeMinutes
        )
      }

    val snoozePendingIntent =
      PendingIntent.getBroadcast(
        context,
        requestCodeFor(
          base = REQUEST_CODE_SNOOZE_BASE,
          alarmId = alarmId
        ),
        snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    val dismissIntent =
      Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_ALARM_DISMISS
        putExtra(EXTRA_ALARM_ID, alarmId)
      }

    val dismissPendingIntent =
      PendingIntent.getBroadcast(
        context,
        requestCodeFor(
          base = REQUEST_CODE_DISMISS_BASE,
          alarmId = alarmId
        ),
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
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(
          NotificationCompat.VISIBILITY_PUBLIC
        )
        .setFullScreenIntent(
          fullScreenPendingIntent,
          true
        )
        .setContentIntent(fullScreenPendingIntent)
        .setDeleteIntent(dismissPendingIntent)
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
       * Notification permission or full-screen access can be denied by the
       * operating system. Audio must continue even if notification display
       * fails.
       */
      e.printStackTrace()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    val notificationManager =
      context.getSystemService(
        Context.NOTIFICATION_SERVICE
      ) as NotificationManager

    /*
     * SoundPlayer owns alarm audio and vibration. The notification channel is
     * intentionally silent so it does not produce a second sound or a second,
     * unsynchronized vibration pattern.
     */
    val channel = NotificationChannel(
      CHANNEL_ID_ALARM,
      "Metro Alarms",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description =
        "Alarm alerts from MetroClock"

      lockscreenVisibility =
        NotificationCompat.VISIBILITY_PUBLIC

      enableLights(true)
      enableVibration(false)
      setSound(null, null)

      /*
       * Do not call setBypassDnd(true) here. DND bypass is user-controlled and
       * depends on notification policy access. Alarm audio already uses alarm
       * audio attributes through SoundPlayer.
       */
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
  // Helpers
  // ---------------------------------------------------------------------------

  private fun notificationIdFor(alarmId: Long): Int {
    val safeHash = alarmId.hashCode() and 0x0FFFFFFF
    return NOTIFICATION_ID_BASE + safeHash
  }

  private fun requestCodeFor(
    base: Int,
    alarmId: Long
  ): Int {
    val safeHash = alarmId.hashCode() and 0x0FFFFFFF
    return base + safeHash
  }

  private fun formatAlarmNotificationText(
    hour: Int,
    minute: Int
  ): String {
    return String.format(
      java.util.Locale.getDefault(),
      "Alarm ringing · %02d:%02d",
      hour.coerceIn(0, 23),
      minute.coerceIn(0, 59)
    )
  }
}
