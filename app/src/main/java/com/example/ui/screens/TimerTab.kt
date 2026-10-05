package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TimerUiState
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalMetroDivider
import com.example.ui.theme.LocalMetroSubtextColor
import com.example.ui.theme.LocalMetroTextColor
import com.example.ui.theme.LocalMetroTileBg
import com.example.util.rememberMetroHaptic

data class QuickSetOption(val labelNumber: String, val labelUnit: String, val totalSeconds: Int)

val QuickSetOptions = listOf(
  QuickSetOption("1", "min", 60),
  QuickSetOption("3", "min", 180),
  QuickSetOption("5", "min", 300),
  QuickSetOption("10", "min", 600),
  QuickSetOption("15", "min", 900),
  QuickSetOption("30", "min", 1800),
  QuickSetOption("45", "min", 2700),
  QuickSetOption("1", "hour", 3600)
)

@Composable
fun TimerTab(
  timerState: TimerUiState,
  onTapTime: () -> Unit,
  onSelectQuickSet: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val accentColor = LocalAccentColor.current
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val dividerColor = LocalMetroDivider.current
  val haptic = rememberMetroHaptic()

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp)
      .testTag("timer_tab")
  ) {
    // Large countdown text
    Text(
      text = timerState.formattedTime,
      fontSize = 92.sp,
      fontWeight = FontWeight.Light,
      color = textColor,
      letterSpacing = (-2).sp,
      modifier = Modifier
        .clickable {
          haptic()
          onTapTime()
        }
        .testTag("timer_time_display")
    )

    // Flat rectangular progress bar directly underneath time
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .background(dividerColor)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(timerState.progress)
          .height(5.dp)
          .background(accentColor)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Subtitle: ends at [time] or tap the time to change it
    val subText = when {
      timerState.isRunning && timerState.formattedEndTime.isNotEmpty() ->
        "ends at ${timerState.formattedEndTime}"
      timerState.isPaused ->
        "paused (${timerState.formattedTime} left)"
      else ->
        "tap the time to change it"
    }

    Text(
      text = subText,
      fontSize = 16.sp,
      fontWeight = FontWeight.Normal,
      color = subtextColor,
      modifier = Modifier.padding(bottom = 20.dp)
    )

    // "quick set" header in accent color
    Text(
      text = "quick set",
      fontSize = 18.sp,
      fontWeight = FontWeight.Normal,
      color = accentColor,
      modifier = Modifier.padding(bottom = 12.dp)
    )

    // 4 columns x 2 rows quick set grid
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        QuickSetOptions.subList(0, 4).forEach { option ->
          QuickSetTile(
            option = option,
            modifier = Modifier.weight(1f),
            onClick = {
              haptic()
              onSelectQuickSet(option.totalSeconds)
            }
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        QuickSetOptions.subList(4, 8).forEach { option ->
          QuickSetTile(
            option = option,
            modifier = Modifier.weight(1f),
            onClick = {
              haptic()
              onSelectQuickSet(option.totalSeconds)
            }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Footnote
    Text(
      text = "Tap the time to set any length. The timer keeps running when you leave the app.",
      fontSize = 13.sp,
      fontWeight = FontWeight.Light,
      color = subtextColor.copy(alpha = 0.8f),
      lineHeight = 18.sp
    )
  }
}

@Composable
fun QuickSetTile(
  option: QuickSetOption,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val tileBg = LocalMetroTileBg.current
  val subtextColor = LocalMetroSubtextColor.current

  Box(
    modifier = modifier
      .aspectRatio(1f)
      .background(tileBg)
      .clickable(onClick = onClick)
      .padding(8.dp)
      .testTag("quick_set_${option.labelNumber}_${option.labelUnit}"),
    contentAlignment = Alignment.TopStart
  ) {
    Column {
      Text(
        text = option.labelNumber,
        fontSize = 28.sp,
        fontWeight = FontWeight.Light,
        color = subtextColor.copy(alpha = 0.9f),
        lineHeight = 30.sp
      )
      Text(
        text = option.labelUnit,
        fontSize = 12.sp,
        fontWeight = FontWeight.Light,
        color = subtextColor.copy(alpha = 0.7f)
      )
    }
  }
}
