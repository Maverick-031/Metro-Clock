package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar

object CalendarHelper {
  /**
   * Queries Android Calendar Provider to check if there is an all-day event or holiday
   * on the specified alarm trigger time.
   */
  fun hasAllDayEvent(context: Context, triggerMillis: Long): Boolean {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }

    return try {
      val targetCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
      val startOfDay = targetCal.clone() as Calendar
      startOfDay.set(Calendar.HOUR_OF_DAY, 0)
      startOfDay.set(Calendar.MINUTE, 0)
      startOfDay.set(Calendar.SECOND, 0)
      startOfDay.set(Calendar.MILLISECOND, 0)

      val endOfDay = targetCal.clone() as Calendar
      endOfDay.set(Calendar.HOUR_OF_DAY, 23)
      endOfDay.set(Calendar.MINUTE, 59)
      endOfDay.set(Calendar.SECOND, 59)
      endOfDay.set(Calendar.MILLISECOND, 999)

      val projection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.ALL_DAY,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND
      )

      // Query for all-day events overlapping the day
      val selection = "(${CalendarContract.Events.ALL_DAY} = 1) AND " +
          "(${CalendarContract.Events.DTSTART} <= ?) AND " +
          "(${CalendarContract.Events.DTEND} >= ?)"
      val selectionArgs = arrayOf(
        endOfDay.timeInMillis.toString(),
        startOfDay.timeInMillis.toString()
      )

      context.contentResolver.query(
        CalendarContract.Events.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
      )?.use { cursor ->
        cursor.count > 0
      } ?: false
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
