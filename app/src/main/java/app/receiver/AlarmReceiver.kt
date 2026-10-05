package app.metroclock.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.metroclock.ClockApplication
import app.metroclock.MainActivity

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {
  companion object {
    const val CHANNEL_ID_ALARM = "metro_alarm_channel"
    const val ACTION_ALARM_TRIGGER = "app.metroclock.ALARM_TRIGGER"
    const val ACTION_ALARM_SNOOZE = "app.metroclock.ALARM_SNOOZE"
    const val ACTION_ALARM_DISMISS = "app.metroclock.ALARM_DISMISS"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_NAME = "extra_alarm_name"
    const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
    const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val EXTRA_SKIP_CALENDAR = "extra_skip_calendar"
    const val EXTRA_SMART_SKIP = "extra_smart_skip"
    const val EXTRA_SOUND_URI = "extra_sound_uri"
    const val NOTIFICATION_ID_BASE = 5000
  }

  override fun onReceive(context: Context, intent: Intent) {
    val app = context.applicationContext as? ClockApplication
    val soundPlayer = app?.container?.soundPlayer
    val settingsDataStore = app?.container?.settingsDataStore

    when (intent.action) {
      ACTION_ALARM_TRIGGER -> {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val alarmName = intent.getStringExtra(EXTRA_ALARM_NAME) ?: "Alarm"
        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, 12)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
        val skipCalendar = intent.getBooleanExtra(EXTRA_SKIP_CALENDAR, false)
        val smartSkip = intent.getBooleanExtra(EXTRA_SMART_SKIP, false)
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""

        // Check if all alarms are currently turned off in Settings/3-dot menu
        var allAlarmsDisabled = false
        var vibrate = true
        var silenceAfter = "10"
        var gradualVolume = "never"

        if (settingsDataStore != null) {
          runBlocking {
            allAlarmsDisabled = settingsDataStore.allAlarmsDisabled.firstOrNull() ?: false
            vibrate = settingsDataStore.alarmVibrate.firstOrNull() ?: true
            silenceAfter = settingsDataStore.alarmSilenceAfter.firstOrNull() ?: "10"
            gradualVolume = settingsDataStore.alarmGradualVolume.firstOrNull() ?: "never"
          }
        }

        if (allAlarmsDisabled) {
          return // Do not ring or show notification if all alarms disabled
        }

        // Feature: Skip Holidays / Calendar Events
        if (skipCalendar && app?.container?.calendarHelper?.hasAllDayEvent(context, System.currentTimeMillis()) == true) {
          return
        }

        // Feature: Location Aware / Smart Skip
        if (smartSkip) {
          val skipDueToLocation = runBlocking {
            app?.container?.smartSkipManager?.shouldSkipAlarmDueToLocation() ?: false
          }
          if (skipDueToLocation) {
            return
          }
        }

        // Notify sound player with user preferences
        soundPlayer?.playAlarmSound(
          customUri = soundUri,
          vibrate = vibrate,
          silenceAfterMinutes = silenceAfter,
          gradualVolumeSeconds = gradualVolume
        )

        // Create notification channel
        createNotificationChannel(context)

        // Intent to open MainActivity directly on the Alarm Triggered full-screen
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
          putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DEST_ALARM_TRIGGERED)
          putExtra(EXTRA_ALARM_ID, alarmId)
          putExtra(EXTRA_ALARM_NAME, alarmName)
          putExtra(EXTRA_ALARM_HOUR, hour)
          putExtra(EXTRA_ALARM_MINUTE, minute)
          putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
          // Ensure this intent can show over lock screen
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
          context,
          alarmId.toInt(),
          fullScreenIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze intent
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
          action = ACTION_ALARM_SNOOZE
          putExtra(EXTRA_ALARM_ID, alarmId)
          putExtra(EXTRA_ALARM_NAME, alarmName)
          putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
          context,
          (alarmId + 100).toInt(),
          snoozeIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss intent
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
          action = ACTION_ALARM_DISMISS
          putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
          context,
          (alarmId + 200).toInt(),
          dismissIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
          .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
          .setContentTitle(alarmName)
          .setContentText("Alarm ringing")
          .setPriority(NotificationCompat.PRIORITY_MAX)
          .setCategory(NotificationCompat.CATEGORY_ALARM)
          .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
          .setFullScreenIntent(fullScreenPendingIntent, true)
          .setContentIntent(fullScreenPendingIntent)
          .setAutoCancel(true)
          .setOngoing(true)
          .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
          .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
          .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((NOTIFICATION_ID_BASE + alarmId).toInt(), notification)

        // Start activity if allowed
        try {
          context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      ACTION_ALARM_SNOOZE -> {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val alarmName = intent.getStringExtra(EXTRA_ALARM_NAME) ?: "Alarm"
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

        soundPlayer?.stopSound()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel((NOTIFICATION_ID_BASE + alarmId).toInt())

        app?.container?.alarmScheduler?.snooze(alarmId, alarmName, snoozeMinutes)
      }

      ACTION_ALARM_DISMISS -> {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        soundPlayer?.stopSound()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel((NOTIFICATION_ID_BASE + alarmId).toInt())
      }
    }
  }

  private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID_ALARM,
        "Metro Alarms",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Windows Phone Metro Alarm Alerts"
        enableVibration(true)
        setBypassDnd(true)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }
}
