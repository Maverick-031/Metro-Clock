package app.metroclock.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class WorldClockCity(
  val id: String,
  val cityName: String,
  val country: String,
  val timeZoneId: String
) {
  fun getCurrentTime(): String {
    val tz = TimeZone.getTimeZone(timeZoneId)
    val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
    sdf.timeZone = tz
    return sdf.format(Calendar.getInstance().time)
  }

  fun getAmPm(): String {
    val tz = TimeZone.getTimeZone(timeZoneId)
    val sdf = SimpleDateFormat("a", Locale.getDefault())
    sdf.timeZone = tz
    return sdf.format(Calendar.getInstance().time).uppercase(Locale.getDefault())
  }

  fun getTimeDifference(): String {
    val tz = TimeZone.getTimeZone(timeZoneId)
    val localTz = TimeZone.getDefault()
    val now = System.currentTimeMillis()
    val diffMillis = (tz.getOffset(now) - localTz.getOffset(now)).toLong()
    val diffHours = (diffMillis / (1000 * 60 * 60)).toInt()
    val diffMins = ((Math.abs(diffMillis) / (1000 * 60)) % 60).toInt()

    val localCal = Calendar.getInstance()
    val cityCal = Calendar.getInstance(tz)

    val dayText = when {
      cityCal.get(Calendar.DAY_OF_YEAR) > localCal.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
      cityCal.get(Calendar.DAY_OF_YEAR) < localCal.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
      else -> "Today"
    }

    val hoursStr = if (diffHours == 0 && diffMins == 0) {
      "Same time"
    } else {
      val sign = if (diffMillis >= 0) "+" else "-"
      val absHours = Math.abs(diffHours)
      if (diffMins > 0) "$sign$absHours hrs $diffMins mins" else "$sign$absHours hrs"
    }

    return "$dayText, $hoursStr"
  }

  val isNight: Boolean
    get() {
      val tz = TimeZone.getTimeZone(timeZoneId)
      val cal = Calendar.getInstance(tz)
      val hour = cal.get(Calendar.HOUR_OF_DAY)
      return hour < 6 || hour >= 20
    }
}

val DefaultWorldCities = listOf(
  WorldClockCity("lon", "London", "United Kingdom", "Europe/London"),
  WorldClockCity("nyc", "New York", "United States", "America/New_York"),
  WorldClockCity("tok", "Tokyo", "Japan", "Asia/Tokyo"),
  WorldClockCity("par", "Paris", "France", "Europe/Paris"),
  WorldClockCity("syd", "Sydney", "Australia", "Australia/Sydney"),
  WorldClockCity("dxb", "Dubai", "United Arab Emirates", "Asia/Dubai")
)

val AvailableWorldCities = listOf(
  WorldClockCity("lon", "London", "United Kingdom", "Europe/London"),
  WorldClockCity("nyc", "New York", "United States", "America/New_York"),
  WorldClockCity("tok", "Tokyo", "Japan", "Asia/Tokyo"),
  WorldClockCity("par", "Paris", "France", "Europe/Paris"),
  WorldClockCity("syd", "Sydney", "Australia", "Australia/Sydney"),
  WorldClockCity("dxb", "Dubai", "United Arab Emirates", "Asia/Dubai"),
  WorldClockCity("lax", "Los Angeles", "United States", "America/Los_Angeles"),
  WorldClockCity("sin", "Singapore", "Singapore", "Asia/Singapore"),
  WorldClockCity("ber", "Berlin", "Germany", "Europe/Berlin"),
  WorldClockCity("hkg", "Hong Kong", "China", "Asia/Hong_Kong"),
  WorldClockCity("cai", "Cairo", "Egypt", "Africa/Cairo"),
  WorldClockCity("sao", "São Paulo", "Brazil", "America/Sao_Paulo"),
  WorldClockCity("del", "New Delhi", "India", "Asia/Kolkata"),
  WorldClockCity("tor", "Toronto", "Canada", "America/Toronto"),
  WorldClockCity("rom", "Rome", "Italy", "Europe/Rome"),
  WorldClockCity("ams", "Amsterdam", "Netherlands", "Europe/Amsterdam"),
  WorldClockCity("sel", "Seoul", "South Korea", "Asia/Seoul"),
  WorldClockCity("mex", "Mexico City", "Mexico", "America/Mexico_City"),
  WorldClockCity("jnb", "Johannesburg", "South Africa", "Africa/Johannesburg"),
  WorldClockCity("akl", "Auckland", "New Zealand", "Pacific/Auckland")
)
