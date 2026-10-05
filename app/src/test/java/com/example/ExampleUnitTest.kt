package com.example

import com.example.alarm.AlarmScheduler
import com.example.data.AlarmEntity
import com.example.data.WorldClockCity
import com.example.service.StopwatchLap
import com.example.service.TimerUiState
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun testAlarmFormatting() {
    val alarm1 = AlarmEntity(hour = 7, minute = 30, repeatDays = 127)
    assertEquals("7:30", alarm1.formattedTime)
    assertEquals("AM", alarm1.amPm)
    assertEquals("every day", alarm1.repeatDescription)

    val alarm2 = AlarmEntity(hour = 15, minute = 45, repeatDays = 0)
    assertEquals("3:45", alarm2.formattedTime)
    assertEquals("PM", alarm2.amPm)
    assertEquals("only once", alarm2.repeatDescription)

    val alarm3 = AlarmEntity(hour = 0, minute = 5, repeatDays = 62)
    assertEquals("12:05", alarm3.formattedTime)
    assertEquals("AM", alarm3.amPm)
    assertEquals("weekdays", alarm3.repeatDescription)
  }

  @Test
  fun testAlarmTriggerCalculation() {
    val cal = Calendar.getInstance()
    val futureHour = (cal.get(Calendar.HOUR_OF_DAY) + 2) % 24
    val trigger = AlarmScheduler.calculateNextTriggerTime(futureHour, 0, 0)
    assertTrue("Trigger should be in the future", trigger > System.currentTimeMillis())
  }

  @Test
  fun testTimerFormatting() {
    val timer1 = TimerUiState(totalSeconds = 180, remainingSeconds = 71)
    assertEquals("01:11", timer1.formattedTime)

    val timer2 = TimerUiState(totalSeconds = 3600, remainingSeconds = 3665)
    assertEquals("1:01:05", timer2.formattedTime)
  }

  @Test
  fun testStopwatchFormatting() {
    val formatted = StopwatchLap.formatStopwatchMillis(63110)
    assertEquals("01:03.11", formatted)
  }

  @Test
  fun testWorldClockCity() {
    val city = WorldClockCity("lon", "London", "United Kingdom", "Europe/London")
    assertNotNull(city.getCurrentTime())
    assertNotNull(city.getAmPm())
    assertTrue(city.getTimeDifference().isNotEmpty())
  }
}
