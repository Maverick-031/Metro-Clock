package app.metroclock.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroDarkGray
import app.metroclock.ui.theme.MetroWhite

@Composable
fun MetroTimePicker(
  initialHour: Int, // 0..23
  initialMinute: Int, // 0..59
  onTimeSelected: (hour: Int, minute: Int) -> Unit,
  onDismiss: () -> Unit
) {
  val accentColor = LocalAccentColor.current

  val initial12Hour = if (initialHour == 0) 12 else if (initialHour > 12) initialHour - 12 else initialHour
  val initialAmPm = if (initialHour >= 12) "PM" else "AM"

  var selectedHour by remember { mutableIntStateOf(initial12Hour) }
  var selectedMinute by remember { mutableIntStateOf(initialMinute) }
  var selectedAmPm by remember { mutableStateOf(initialAmPm) }

  val hoursList = (1..12).toList()
  val minutesList = (0..59).toList()
  val amPmList = listOf("AM", "PM")

  val hourListState = rememberLazyListState()
  val minuteListState = rememberLazyListState()

  LaunchedEffect(Unit) {
    val hIndex = (selectedHour - 1).coerceAtLeast(0)
    val mIndex = selectedMinute.coerceAtLeast(0)
    hourListState.scrollToItem((hIndex - 2).coerceAtLeast(0))
    minuteListState.scrollToItem((mIndex - 2).coerceAtLeast(0))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MetroBlack)
      .testTag("metro_time_picker")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp)
        .padding(top = 32.dp, bottom = 80.dp)
    ) {
      Text(
        text = "CHOOSE TIME",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = MetroWhite,
        modifier = Modifier.padding(bottom = 24.dp)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Hours Column
        Column(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MetroDarkGray)
        ) {
          LazyColumn(
            state = hourListState,
            modifier = Modifier.fillMaxSize()
          ) {
            items(hoursList) { hour ->
              val isSelected = hour == selectedHour
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(68.dp)
                  .background(if (isSelected) accentColor else MetroBlack)
                  .border(1.dp, if (isSelected) accentColor else MetroDarkGray)
                  .clickable { selectedHour = hour }
                  .testTag("picker_hour_$hour"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = hour.toString(),
                  fontSize = 32.sp,
                  fontWeight = FontWeight.Light,
                  color = if (isSelected) MetroWhite else MetroWhite.copy(alpha = 0.5f),
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }

        // Minutes Column
        Column(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MetroDarkGray)
        ) {
          LazyColumn(
            state = minuteListState,
            modifier = Modifier.fillMaxSize()
          ) {
            items(minutesList) { minute ->
              val isSelected = minute == selectedMinute
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(68.dp)
                  .background(if (isSelected) accentColor else MetroBlack)
                  .border(1.dp, if (isSelected) accentColor else MetroDarkGray)
                  .clickable { selectedMinute = minute }
                  .testTag("picker_minute_$minute"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = minute.toString().padStart(2, '0'),
                  fontSize = 32.sp,
                  fontWeight = FontWeight.Light,
                  color = if (isSelected) MetroWhite else MetroWhite.copy(alpha = 0.5f),
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }

        // AM / PM Column
        Column(
          modifier = Modifier
            .width(84.dp)
            .border(1.dp, MetroDarkGray)
        ) {
          amPmList.forEach { amPm ->
            val isSelected = amPm == selectedAmPm
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(if (isSelected) accentColor else MetroBlack)
                .border(1.dp, if (isSelected) accentColor else MetroDarkGray)
                .clickable { selectedAmPm = amPm }
                .testTag("picker_ampm_$amPm"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = amPm,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = if (isSelected) MetroWhite else MetroWhite.copy(alpha = 0.5f)
              )
            }
          }
        }
      }
    }

    // Bottom Action Buttons
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      MetroAppBar(
        actions = listOf(
          MetroAppBarAction(
            icon = Icons.Default.Check,
            label = "Done",
            testTag = "time_picker_done",
            onClick = {
              val calculatedHour = when {
                selectedAmPm == "AM" && selectedHour == 12 -> 0
                selectedAmPm == "AM" -> selectedHour
                selectedAmPm == "PM" && selectedHour == 12 -> 12
                else -> selectedHour + 12
              }
              onTimeSelected(calculatedHour, selectedMinute)
            }
          ),
          MetroAppBarAction(
            icon = Icons.Default.Close,
            label = "Cancel",
            testTag = "time_picker_cancel",
            onClick = onDismiss
          )
        )
      )
    }
  }
}
