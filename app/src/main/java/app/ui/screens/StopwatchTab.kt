package app.metroclock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.service.StopwatchUiState
import app.metroclock.ui.theme.LocalMetroDivider
import app.metroclock.ui.theme.LocalMetroSubtextColor
import app.metroclock.ui.theme.LocalMetroTextColor

@Composable
fun StopwatchTab(
  stopwatchState: StopwatchUiState,
  modifier: Modifier = Modifier
) {
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val dividerColor = LocalMetroDivider.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp)
      .testTag("stopwatch_tab")
  ) {
    // Large timer display
    Text(
      text = stopwatchState.formattedTime,
      fontSize = 80.sp,
      fontWeight = FontWeight.Light,
      color = textColor,
      letterSpacing = (-1.5).sp,
      modifier = Modifier.testTag("stopwatch_time_display")
    )

    // Current lap subtext: "lap X 00:00.00"
    val lapIndex = stopwatchState.laps.size + 1
    val currentLapText = if (stopwatchState.elapsedMillis > 0) {
      "lap $lapIndex  ${stopwatchState.formattedCurrentLap}"
    } else {
      ""
    }

    Text(
      text = currentLapText,
      fontSize = 18.sp,
      fontWeight = FontWeight.Light,
      color = subtextColor,
      modifier = Modifier
        .height(26.dp)
        .padding(bottom = 6.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Table Header: "lap", "lap time", "total"
    if (stopwatchState.laps.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "lap",
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
          color = subtextColor,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = "lap time",
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
          color = subtextColor,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(2f)
        )
        Text(
          text = "total",
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
          color = subtextColor,
          textAlign = TextAlign.End,
          modifier = Modifier.weight(2f)
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(dividerColor)
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(stopwatchState.laps, key = { it.lapNumber }) { lap ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("lap_row_${lap.lapNumber}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = lap.lapNumber.toString(),
              fontSize = 18.sp,
              fontWeight = FontWeight.Light,
              color = textColor,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = lap.formattedLapTime,
              fontSize = 18.sp,
              fontWeight = FontWeight.Light,
              color = textColor,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(2f)
            )
            Text(
              text = lap.formattedTotalTime,
              fontSize = 18.sp,
              fontWeight = FontWeight.Light,
              color = textColor,
              textAlign = TextAlign.End,
              modifier = Modifier.weight(2f)
            )
          }
        }
      }
    } else {
      Spacer(modifier = Modifier.weight(1f))
    }
  }
}
