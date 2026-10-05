package app.metroclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved alarm in the Windows Phone Clock app.
 *
 * [repeatDays] is a bitmask where:
 * 0 = only once
 * 1 = Sunday, 2 = Monday, 4 = Tuesday, 8 = Wednesday, 16 = Thursday, 32 = Friday, 64 = Saturday
 * 127 = every day
 * 62 = weekdays (Mon-Fri)
 * 65 = weekends (Sat-Sun)
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val hour: Int, // 0..23
  val minute: Int, // 0..59
  val name: String = "Alarm",
  val isEnabled: Boolean = true,
  val repeatDays: Int = 0, // 0 = only once, 127 = every day
  val soundName: String = "Alarm Classic",
  val soundUri: String = "",
  val snoozeMinutes: Int = 10,
  val lastTriggeredEpoch: Long = 0L,
  val skipIfCalendarEvent: Boolean = false,
  val smartSkipLocation: Boolean = false
) {
  val formattedTime: String
    get() {
      val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
      val m = minute.toString().padStart(2, '0')
      return "$h:$m"
    }

  val amPm: String
    get() = if (hour >= 12) "PM" else "AM"

  val repeatDescription: String
    get() {
      return when (repeatDays) {
        0 -> "only once"
        127 -> "every day"
        62 -> "weekdays"
        65 -> "weekends"
        else -> {
          val days = mutableListOf<String>()
          if ((repeatDays and 2) != 0) days.add("Mon")
          if ((repeatDays and 4) != 0) days.add("Tue")
          if ((repeatDays and 8) != 0) days.add("Wed")
          if ((repeatDays and 16) != 0) days.add("Thu")
          if ((repeatDays and 32) != 0) days.add("Fri")
          if ((repeatDays and 64) != 0) days.add("Sat")
          if ((repeatDays and 1) != 0) days.add("Sun")
          days.joinToString(", ")
        }
      }
    }
}
