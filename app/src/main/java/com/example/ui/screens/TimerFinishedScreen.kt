package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun TimerFinishedScreen(
  totalSeconds: Int,
  onRestart: () -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler { onDismiss() }

  val accentColor = LocalAccentColor.current

  val durationMinutes = totalSeconds / 60
  val durationLabel = when {
    totalSeconds < 60 -> "<1 minute timer finished"
    durationMinutes == 1 -> "1 minute timer finished"
    durationMinutes < 60 -> "$durationMinutes minute timer finished"
    else -> "${durationMinutes / 60} hour timer finished"
  }

  val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.US)
  val todayFormatted = dateFormat.format(Date())

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(accentColor)
      .statusBarsPadding()
      .padding(horizontal = 24.dp, vertical = 24.dp)
      .testTag("timer_finished_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Header: small alarm icon + "Timer  Time's up!"
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Alarm,
          contentDescription = "Timer",
          tint = MetroWhite,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "  Timer  Time’s up!",
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
          color = MetroWhite
        )
      }

      // Huge text: "time's up"
      Text(
        text = "time’s up",
        fontSize = 86.sp,
        fontWeight = FontWeight.Light,
        color = MetroWhite,
        letterSpacing = (-2).sp,
        modifier = Modifier.padding(bottom = 2.dp)
      )

      // Subtext: "<1 minute timer finished"
      Text(
        text = durationLabel,
        fontSize = 26.sp,
        fontWeight = FontWeight.Light,
        color = MetroWhite,
        modifier = Modifier.padding(bottom = 6.dp)
      )

      // Date: "Monday, October 5"
      Text(
        text = todayFormatted,
        fontSize = 16.sp,
        fontWeight = FontWeight.Light,
        color = MetroWhite.copy(alpha = 0.9f)
      )

      Spacer(modifier = Modifier.weight(1f))

      // Center: large ringing alarm clock — same icon and animation as when an alarm rings
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(2f),
        contentAlignment = Alignment.Center
      ) {
        RingingAlarmClockGraphic(modifier = Modifier.size(140.dp))
      }

      Spacer(modifier = Modifier.weight(1f))

      // Bottom buttons: [ restart ] and [ dismiss ]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .border(2.dp, MetroWhite)
            .clickable { onRestart() }
            .testTag("btn_timer_restart"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "restart",
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
            .testTag("btn_timer_dismiss"),
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

/**
 * Windows Phone authentic outlined clock icon with hands at 12 and 3.
 */
@Composable
fun TimerClockGraphic(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.42f
    val strokeWidth = 4.dp.toPx()

    // Outer circle
    drawCircle(
      color = MetroWhite,
      radius = radius,
      center = center,
      style = Stroke(width = strokeWidth)
    )

    // Hand pointing to 12
    drawLine(
      color = MetroWhite,
      start = center,
      end = Offset(center.x, center.y - radius * 0.65f),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )

    // Hand pointing to 3
    drawLine(
      color = MetroWhite,
      start = center,
      end = Offset(center.x + radius * 0.5f, center.y),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round
    )
  }
}
