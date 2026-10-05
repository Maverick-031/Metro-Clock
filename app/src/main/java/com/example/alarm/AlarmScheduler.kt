package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AlarmEntity
import com.example.receiver.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  fun schedule(alarm: AlarmEntity) {
    if (!alarm.isEnabled) {
      cancel(alarm)
      return
    }

    val triggerAtMillis = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDays)
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_ALARM_TRIGGER
      putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
      putExtra(AlarmReceiver.EXTRA_ALARM_NAME, alarm.name)
      putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hour)
      putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
      putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
      putExtra(AlarmReceiver.EXTRA_SKIP_CALENDAR, alarm.skipIfCalendarEvent)
      putExtra(AlarmReceiver.EXTRA_SMART_SKIP, alarm.smartSkipLocation)
      putExtra(AlarmReceiver.EXTRA_SOUND_URI, alarm.soundUri)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      alarm.id.toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Show intent opened when user clicks the alarm clock icon in system status bar
    val showIntent = Intent(context, com.example.MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val showPendingIntent = PendingIntent.getActivity(
      context,
      (alarm.id + 20000).toInt(),
      showIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager?.let { am ->
      try {
        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        am.setAlarmClock(clockInfo, pendingIntent)
      } catch (e: SecurityException) {
        e.printStackTrace()
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
          } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
          }
        } catch (e2: Exception) {
          am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun cancel(alarm: AlarmEntity) {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_ALARM_TRIGGER
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      alarm.id.toInt(),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager?.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  fun snooze(alarmId: Long, name: String, snoozeMinutes: Int = 10) {
    val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_ALARM_TRIGGER
      putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
      putExtra(AlarmReceiver.EXTRA_ALARM_NAME, name)
      putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      (alarmId + 10000).toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager?.let { am ->
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
          am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
          am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  companion object {
    fun calculateNextTriggerTime(hour: Int, minute: Int, repeatDays: Int): Long {
      val now = Calendar.getInstance()
      val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

      if (repeatDays == 0) {
        // Only once
        if (target.timeInMillis <= now.timeInMillis) {
          target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
      }

      // Bitmask repeat check
      // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
      // Bitmask: 1 = Sun, 2 = Mon, 4 = Tue, 8 = Wed, 16 = Thu, 32 = Fri, 64 = Sat
      fun dayMatches(cal: Calendar): Boolean {
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        val bit = 1 shl (calDay - 1)
        return (repeatDays and bit) != 0
      }

      if (target.timeInMillis > now.timeInMillis && dayMatches(target)) {
        return target.timeInMillis
      }

      for (i in 1..7) {
        target.add(Calendar.DAY_OF_YEAR, 1)
        if (dayMatches(target)) {
          return target.timeInMillis
        }
      }

      return target.timeInMillis
    }
  }
}
