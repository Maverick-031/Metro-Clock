package app.metroclock.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.metroclock.MainActivity
import app.metroclock.data.AlarmEntity
import app.metroclock.receiver.AlarmReceiver
import java.util.Calendar

/**
 * Schedules, cancels, and snoozes MetroClock alarms.
 *
 * This class stores only the application context and creates explicit
 * PendingIntents targeting AlarmReceiver.
 */
class AlarmScheduler(context: Context) {

  companion object {

    private const val DAYS_IN_WEEK = 7

    /*
     * Request-code namespaces prevent the main alarm, snooze alarm, and
     * show-alarm Activity PendingIntents from replacing one another.
     */
    private const val REQUEST_NAMESPACE_MAIN_ALARM = 0x10000000
    private const val REQUEST_NAMESPACE_SNOOZE = 0x20000000
    private const val REQUEST_NAMESPACE_SHOW_ALARMS = 0x30000000

    /**
     * Calculates the next occurrence of an alarm.
     *
     * [repeatDays] uses this bitmask:
     *
     * 1   = Sunday
     * 2   = Monday
     * 4   = Tuesday
     * 8   = Wednesday
     * 16  = Thursday
     * 32  = Friday
     * 64  = Saturday
     *
     * A value of zero represents a one-time alarm. If today's selected time
     * has passed, the one-time alarm is scheduled for tomorrow.
     */
    fun calculateNextTriggerTime(
      hour: Int,
      minute: Int,
      repeatDays: Int,
      nowMillis: Long = System.currentTimeMillis()
    ): Long {
      val safeHour = hour.coerceIn(0, 23)
      val safeMinute = minute.coerceIn(0, 59)

      /*
       * Only the lowest seven bits represent weekdays.
       */
      val safeRepeatDays =
        repeatDays and 0b01111111

      val now = Calendar.getInstance().apply {
        timeInMillis = nowMillis
      }

      val candidate = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, safeHour)
        set(Calendar.MINUTE, safeMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }

      if (safeRepeatDays == 0) {
        if (candidate.timeInMillis <= nowMillis) {
          candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candidate.timeInMillis
      }

      /*
       * Search today through the next seven days.
       *
       * Today is accepted only when the selected time is still in the future.
       */
      repeat(DAYS_IN_WEEK + 1) { dayOffset ->
        if (dayOffset > 0) {
          candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        val candidateInFuture =
          candidate.timeInMillis > nowMillis

        if (
          candidateInFuture &&
          dayMatches(
            calendar = candidate,
            repeatDays = safeRepeatDays
          )
        ) {
          return candidate.timeInMillis
        }
      }

      /*
       * The seven-bit repeat mask guarantees that a selected day is found.
       * This fallback is defensive for malformed input.
       */
      candidate.add(Calendar.DAY_OF_YEAR, 1)
      return candidate.timeInMillis
    }

    private fun dayMatches(
      calendar: Calendar,
      repeatDays: Int
    ): Boolean {
      val calendarDay =
        calendar.get(Calendar.DAY_OF_WEEK)

      val dayBit =
        1 shl (calendarDay - Calendar.SUNDAY)

      return repeatDays and dayBit != 0
    }
  }

  private val appContext: Context =
    context.applicationContext

  private val alarmManager: AlarmManager? =
    appContext.getSystemService(
      Context.ALARM_SERVICE
    ) as? AlarmManager

  // ---------------------------------------------------------------------------
  // Main alarm scheduling
  // ---------------------------------------------------------------------------

  /**
   * Schedules the next occurrence of [alarm].
   *
   * Disabled or invalid alarms are cancelled instead of scheduled.
   *
   * @return true if Android accepted an alarm request, otherwise false.
   */
  fun schedule(alarm: AlarmEntity): Boolean {
    if (!alarm.isEnabled || alarm.id <= 0L) {
      cancel(alarm)
      return false
    }

    val manager = alarmManager ?: return false

    val triggerAtMillis =
      calculateNextTriggerTime(
        hour = alarm.hour,
        minute = alarm.minute,
        repeatDays = alarm.repeatDays
      )

    val alarmPendingIntent =
      createMainAlarmPendingIntent(
        alarm = alarm,
        flags =
          PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE
      )

    val showPendingIntent =
      createShowAlarmsPendingIntent(
        alarmId = alarm.id
      )

    return scheduleAlarmClock(
      manager = manager,
      triggerAtMillis = triggerAtMillis,
      operation = alarmPendingIntent,
      showIntent = showPendingIntent
    )
  }

  /**
   * Cancels the main scheduled occurrence and any outstanding snooze
   * occurrence for [alarm].
   */
  fun cancel(alarm: AlarmEntity) {
    cancel(alarm.id)
  }

  /**
   * Cancels the main scheduled occurrence and any snooze occurrence for an
   * alarm ID.
   */
  fun cancel(alarmId: Long) {
    if (alarmId <= 0L) {
      return
    }

    cancelPendingIntent(
      createMainAlarmPendingIntentForCancellation(
        alarmId = alarmId
      )
    )

    cancelPendingIntent(
      createSnoozePendingIntentForCancellation(
        alarmId = alarmId
      )
    )
  }

  // ---------------------------------------------------------------------------
  // Snooze scheduling
  // ---------------------------------------------------------------------------

  /**
   * Schedules a snoozed alarm.
   *
   * The optional properties ensure a snoozed alarm can retain the original
   * alarm's sound and Smart Skip settings. Existing callers that only provide
   * ID, name, and duration remain source-compatible.
   *
   * @return true if Android accepted the snooze request.
   */
  fun snooze(
    alarmId: Long,
    name: String,
    snoozeMinutes: Int = 10,
    hour: Int = 0,
    minute: Int = 0,
    soundUri: String = "",
    skipIfCalendarEvent: Boolean = false,
    smartSkipLocation: Boolean = false
  ): Boolean {
    if (alarmId <= 0L) {
      return false
    }

    val manager = alarmManager ?: return false

    val safeSnoozeMinutes =
      snoozeMinutes.coerceAtLeast(1)

    val triggerAtMillis =
      calculateSnoozeTriggerTime(
        safeSnoozeMinutes
      )

    /*
     * Cancel an existing snooze for this alarm before replacing it.
     */
    cancelPendingIntent(
      createSnoozePendingIntentForCancellation(
        alarmId = alarmId
      )
    )

    val snoozeIntent =
      Intent(
        appContext,
        AlarmReceiver::class.java
      ).apply {
        action = AlarmReceiver.ACTION_ALARM_TRIGGER

        putExtra(
          AlarmReceiver.EXTRA_ALARM_ID,
          alarmId
        )

        putExtra(
          AlarmReceiver.EXTRA_ALARM_NAME,
          name.trim().ifBlank { "Alarm" }
        )

        putExtra(
          AlarmReceiver.EXTRA_ALARM_HOUR,
          hour.coerceIn(0, 23)
        )

        putExtra(
          AlarmReceiver.EXTRA_ALARM_MINUTE,
          minute.coerceIn(0, 59)
        )

        putExtra(
          AlarmReceiver.EXTRA_SNOOZE_MINUTES,
          safeSnoozeMinutes
        )

        putExtra(
          AlarmReceiver.EXTRA_SOUND_URI,
          soundUri.trim()
        )

        putExtra(
          AlarmReceiver.EXTRA_SKIP_CALENDAR,
          skipIfCalendarEvent
        )

        putExtra(
          AlarmReceiver.EXTRA_SMART_SKIP,
          smartSkipLocation
        )
      }

    val snoozePendingIntent =
      PendingIntent.getBroadcast(
        appContext,
        snoozeRequestCode(alarmId),
        snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    return scheduleExactAlarm(
      manager = manager,
      triggerAtMillis = triggerAtMillis,
      operation = snoozePendingIntent
    )
  }

  // ---------------------------------------------------------------------------
  // PendingIntent creation
  // ---------------------------------------------------------------------------

  private fun createMainAlarmPendingIntent(
    alarm: AlarmEntity,
    flags: Int
  ): PendingIntent {
    val alarmIntent =
      Intent(
        appContext,
        AlarmReceiver::class.java
      ).apply {
        action = AlarmReceiver.ACTION_ALARM_TRIGGER

        putExtra(
          AlarmReceiver.EXTRA_ALARM_ID,
          alarm.id
        )

        putExtra(
          AlarmReceiver.EXTRA_ALARM_NAME,
          alarm.name.trim().ifBlank { "Alarm" }
        )

        putExtra(
          AlarmReceiver.EXTRA_ALARM_HOUR,
          alarm.hour.coerceIn(0, 23)
        )

        putExtra(
          AlarmReceiver.EXTRA_ALARM_MINUTE,
          alarm.minute.coerceIn(0, 59)
        )

        putExtra(
          AlarmReceiver.EXTRA_SNOOZE_MINUTES,
          alarm.snoozeMinutes.coerceAtLeast(1)
        )

        putExtra(
          AlarmReceiver.EXTRA_SKIP_CALENDAR,
          alarm.skipIfCalendarEvent
        )

        putExtra(
          AlarmReceiver.EXTRA_SMART_SKIP,
          alarm.smartSkipLocation
        )

        putExtra(
          AlarmReceiver.EXTRA_SOUND_URI,
          alarm.soundUri.trim()
        )
      }

    return PendingIntent.getBroadcast(
      appContext,
      mainAlarmRequestCode(alarm.id),
      alarmIntent,
      flags
    )
  }

  private fun createMainAlarmPendingIntentForCancellation(
    alarmId: Long
  ): PendingIntent? {
    val alarmIntent =
      Intent(
        appContext,
        AlarmReceiver::class.java
      ).apply {
        action = AlarmReceiver.ACTION_ALARM_TRIGGER
      }

    return PendingIntent.getBroadcast(
      appContext,
      mainAlarmRequestCode(alarmId),
      alarmIntent,
      PendingIntent.FLAG_NO_CREATE or
        PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun createSnoozePendingIntentForCancellation(
    alarmId: Long
  ): PendingIntent? {
    val snoozeIntent =
      Intent(
        appContext,
        AlarmReceiver::class.java
      ).apply {
        action = AlarmReceiver.ACTION_ALARM_TRIGGER
      }

    return PendingIntent.getBroadcast(
      appContext,
      snoozeRequestCode(alarmId),
      snoozeIntent,
      PendingIntent.FLAG_NO_CREATE or
        PendingIntent.FLAG_IMMUTABLE
    )
  }

  /**
   * Creates the system alarm-clock icon destination.
   *
   * This intent opens the normal application UI. It intentionally does not
   * open AlarmTriggeredScreen because the alarm has not fired yet.
   */
  private fun createShowAlarmsPendingIntent(
    alarmId: Long
  ): PendingIntent {
    val showIntent =
      Intent(
        appContext,
        MainActivity::class.java
      ).apply {
        flags =
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

    return PendingIntent.getActivity(
      appContext,
      showAlarmRequestCode(alarmId),
      showIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or
        PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun cancelPendingIntent(
    pendingIntent: PendingIntent?
  ) {
    if (pendingIntent == null) {
      return
    }

    try {
      alarmManager?.cancel(pendingIntent)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    try {
      pendingIntent.cancel()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // AlarmManager operations
  // ---------------------------------------------------------------------------

  /**
   * Schedules a user-visible alarm-clock alarm.
   *
   * setAlarmClock() is preferred because this application represents a real
   * alarm clock and Android can expose the next alarm in system UI.
   */
  private fun scheduleAlarmClock(
    manager: AlarmManager,
    triggerAtMillis: Long,
    operation: PendingIntent,
    showIntent: PendingIntent
  ): Boolean {
    return try {
      if (canScheduleExactAlarms(manager)) {
        val alarmClockInfo =
          AlarmManager.AlarmClockInfo(
            triggerAtMillis,
            showIntent
          )

        manager.setAlarmClock(
          alarmClockInfo,
          operation
        )
      } else {
        /*
         * Exact-alarm access is unavailable. Use a permitted inexact alarm
         * instead of throwing SecurityException and losing the alarm entirely.
         */
        scheduleInexactFallback(
          manager = manager,
          triggerAtMillis = triggerAtMillis,
          operation = operation
        )
      }

      true
    } catch (e: SecurityException) {
      e.printStackTrace()

      scheduleInexactFallback(
        manager = manager,
        triggerAtMillis = triggerAtMillis,
        operation = operation
      )
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Schedules an exact snooze when access is available.
   */
  private fun scheduleExactAlarm(
    manager: AlarmManager,
    triggerAtMillis: Long,
    operation: PendingIntent
  ): Boolean {
    return try {
      if (canScheduleExactAlarms(manager)) {
        if (
          Build.VERSION.SDK_INT >=
          Build.VERSION_CODES.M
        ) {
          manager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            operation
          )
        } else {
          manager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            operation
          )
        }

        true
      } else {
        scheduleInexactFallback(
          manager = manager,
          triggerAtMillis = triggerAtMillis,
          operation = operation
        )
      }
    } catch (e: SecurityException) {
      e.printStackTrace()

      scheduleInexactFallback(
        manager = manager,
        triggerAtMillis = triggerAtMillis,
        operation = operation
      )
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Returns whether the application can use exact-alarm APIs.
   *
   * canScheduleExactAlarms() is available from Android 12. Earlier Android
   * versions do not require this runtime check.
   */
  private fun canScheduleExactAlarms(
    manager: AlarmManager
  ): Boolean {
    return if (
      Build.VERSION.SDK_INT >=
      Build.VERSION_CODES.S
    ) {
      try {
        manager.canScheduleExactAlarms()
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }
    } else {
      true
    }
  }

  /**
   * Last-resort scheduling when exact-alarm access is unavailable.
   *
   * This preserves alarm delivery, but Android may delay the alarm because it
   * is no longer exact.
   */
  private fun scheduleInexactFallback(
    manager: AlarmManager,
    triggerAtMillis: Long,
    operation: PendingIntent
  ): Boolean {
    return try {
      if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.M
      ) {
        manager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          operation
        )
      } else {
        manager.set(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          operation
        )
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  // ---------------------------------------------------------------------------
  // Request-code helpers
  // ---------------------------------------------------------------------------

  /**
   * Produces a stable non-negative value without converting the raw Long ID
   * directly to Int.
   */
  private fun stableAlarmCode(
    alarmId: Long
  ): Int {
    return alarmId.hashCode() and 0x0FFFFFFF
  }

  private fun mainAlarmRequestCode(
    alarmId: Long
  ): Int {
    return REQUEST_NAMESPACE_MAIN_ALARM or
      stableAlarmCode(alarmId)
  }

  private fun snoozeRequestCode(
    alarmId: Long
  ): Int {
    return REQUEST_NAMESPACE_SNOOZE or
      stableAlarmCode(alarmId)
  }

  private fun showAlarmRequestCode(
    alarmId: Long
  ): Int {
    return REQUEST_NAMESPACE_SHOW_ALARMS or
      stableAlarmCode(alarmId)
  }

  private fun calculateSnoozeTriggerTime(
    snoozeMinutes: Int
  ): Long {
    val safeMinutes =
      snoozeMinutes.coerceAtLeast(1).toLong()

    val maximumSafeMinutes =
      (Long.MAX_VALUE -
        System.currentTimeMillis()) / 60_000L

    val clampedMinutes =
      safeMinutes.coerceAtMost(
        maximumSafeMinutes
      )

    return System.currentTimeMillis() +
      clampedMinutes * 60_000L
  }
}
