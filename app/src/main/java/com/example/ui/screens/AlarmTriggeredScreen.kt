package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.MetroWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmTriggeredScreen(
  alarmId: Long,
  alarmName: String,
  hour: Int,
  minute: Int,
  snoozeMinutes: Int,
  onSnooze: (minutes: Int) -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler { onDismiss() }

  val accentColor = LocalAccentColor.current

  val formatted12Hour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
  val amPm = if (hour >= 12) "PM" else "AM"
  val timeDisplay = "$formatted12Hour:${minute.toString().padStart(2, '0')}"

  val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
  val todayFormatted = dateFormat.format(Date())

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(accentColor)
      .statusBarsPadding()
      .padding(horizontal = 24.dp, vertical = 24.dp)
      .testTag("alarm_triggered_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Small uppercase "ALARM"
      Text(
        text = "ALARM",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MetroWhite,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
      )

      // Huge Time
      Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(bottom = 4.dp)
      ) {
        Text(
          text = timeDisplay,
          fontSize = 86.sp,
          fontWeight = FontWeight.Light,
          color = MetroWhite,
          letterSpacing = (-2).sp
        )
        Text(
          text = amPm,
          fontSize = 36.sp,
          fontWeight = FontWeight.Light,
          color = MetroWhite.copy(alpha = 0.85f),
          modifier = Modifier.padding(start = 6.dp, bottom = 12.dp)
        )
      }

      // Alarm Name
      Text(
        text = alarmName,
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        color = MetroWhite,
        modifier = Modifier.padding(bottom = 2.dp)
      )

      // Date: e.g. "Monday, October 5"
      Text(
        text = todayFormatted,
        fontSize = 16.sp,
        fontWeight = FontWeight.Light,
        color = MetroWhite.copy(alpha = 0.9f)
      )

      Spacer(modifier = Modifier.weight(1f))

      // Large ringing alarm clock icon in center
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(2f),
        contentAlignment = Alignment.Center
      ) {
        RingingAlarmClockGraphic(modifier = Modifier.size(140.dp))
      }

      Spacer(modifier = Modifier.weight(1f))

      // Bottom Buttons
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Full width outlined button: "snooze for X minutes"
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(2.dp, MetroWhite)
            .clickable { onSnooze(snoozeMinutes) }
            .padding(horizontal = 16.dp)
            .testTag("btn_alarm_snooze_full"),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = "snooze for $snoozeMinutes minutes",
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = MetroWhite
          )
        }

        // Side-by-side buttons: [ snooze ] and [ dismiss ]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .border(2.dp, MetroWhite)
              .clickable { onSnooze(snoozeMinutes) }
              .testTag("btn_alarm_snooze"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "snooze",
              fontSize = 18.sp,
              fontWeight = FontWeight.Normal,
              color = MetroWhite
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .background(MetroWhite)
              .clickable { onDismiss() }
              .testTag("btn_alarm_dismiss"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "dismiss",
              fontSize = 18.sp,
              fontWeight = FontWeight.Normal,
              color = accentColor
            )
          }
        }
      }
    }
  }
}

/**
 * Windows Phone authentic outlined ringing alarm clock with bell ringing waves.
 */
@Composable
fun RingingAlarmClockGraphic(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "AlarmRingingAnimation")
  val rotation by infiniteTransition.animateFloat(
    initialValue = -14f,
    targetValue = 14f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 80, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "ClockRingingRotation"
  )
  val wavePulse by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 180, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "ClockWavePulse"
  )

  Canvas(
    modifier = modifier.graphicsLayer {
      rotationZ = rotation
    }
  ) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.32f
    val strokeWidth = 4.dp.toPx()

    // Clock circle
    drawCircle(
      color = MetroWhite,
      radius = radius,
      center = center,
      style = Stroke(width = strokeWidth)
    )

    // Clock hands
    // Hour hand (pointing to ~10 o'clock)
    drawLine(
      color = MetroWhite,
      start = center,
      end = Offset(center.x - radius * 0.45f, center.y - radius * 0.35f),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
    // Minute hand (pointing to ~2 o'clock)
    drawLine(
      color = MetroWhite,
      start = center,
      end = Offset(center.x + radius * 0.25f, center.y - radius * 0.65f),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )

    // Ringing waves on top-left
    drawArc(
      color = MetroWhite.copy(alpha = wavePulse),
      startAngle = 200f,
      sweepAngle = 45f,
      useCenter = false,
      topLeft = Offset(center.x - radius * 1.5f, center.y - radius * 1.5f),
      size = androidx.compose.ui.geometry.Size(radius * 3f, radius * 3f),
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Ringing waves on top-right
    drawArc(
      color = MetroWhite.copy(alpha = wavePulse),
      startAngle = 295f,
      sweepAngle = 45f,
      useCenter = false,
      topLeft = Offset(center.x - radius * 1.5f, center.y - radius * 1.5f),
      size = androidx.compose.ui.geometry.Size(radius * 3f, radius * 3f),
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
  }
}
