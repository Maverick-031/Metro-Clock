package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animation.MetroTurnstileEntrance
import com.example.ui.components.MetroAppBar
import com.example.ui.components.MetroAppBarAction
import com.example.ui.components.MetroCheckbox
import com.example.ui.theme.MetroBlack
import com.example.ui.theme.MetroWhite

data class RepeatDayItem(
  val name: String,
  val bitmask: Int
)

val DaysOfWeekList = listOf(
  RepeatDayItem("sunday", 1),
  RepeatDayItem("monday", 2),
  RepeatDayItem("tuesday", 4),
  RepeatDayItem("wednesday", 8),
  RepeatDayItem("thursday", 16),
  RepeatDayItem("friday", 32),
  RepeatDayItem("saturday", 64)
)

@Composable
fun RepeatsScreen(
  initialRepeatDays: Int,
  onSave: (Int) -> Unit,
  onCancel: () -> Unit
) {
  BackHandler { onCancel() }

  var currentRepeatDays by remember { mutableIntStateOf(initialRepeatDays) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MetroBlack)
      .statusBarsPadding()
      .testTag("repeats_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 72.dp)
    ) {
      // Top Category Header: "REPEATS ON" (as shown in image.png)
      Text(
        text = "REPEATS ON",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = MetroWhite,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 28.dp)
      )

      // Vertical list of checkboxes with turnstile entrance
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
        itemsIndexed(DaysOfWeekList) { index, dayItem ->
          val isChecked = (currentRepeatDays and dayItem.bitmask) != 0

          MetroTurnstileEntrance(delayMillis = index * 30) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  currentRepeatDays = if (isChecked) {
                    currentRepeatDays and dayItem.bitmask.inv()
                  } else {
                    currentRepeatDays or dayItem.bitmask
                  }
                }
                .testTag("repeat_row_${dayItem.name}"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              MetroCheckbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                  currentRepeatDays = if (checked) {
                    currentRepeatDays or dayItem.bitmask
                  } else {
                    currentRepeatDays and dayItem.bitmask.inv()
                  }
                },
                testTag = "checkbox_${dayItem.name}"
              )

              Text(
                text = dayItem.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = MetroWhite,
                modifier = Modifier.padding(start = 20.dp)
              )
            }
          }
        }
      }
    }

    // Bottom Action App Bar: ✓ (Check) and ✕ (Close)
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
            testTag = "repeats_done",
            onClick = { onSave(currentRepeatDays) }
          ),
          MetroAppBarAction(
            icon = Icons.Default.Close,
            label = "Cancel",
            testTag = "repeats_cancel",
            onClick = onCancel
          )
        )
      )
    }
  }
}
