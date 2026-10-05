package app.metroclock.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.alarm.AlarmScheduler
import app.metroclock.data.AlarmEntity
import app.metroclock.ui.animation.MetroTurnstileEntrance
import app.metroclock.ui.components.MetroToggle
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.LocalMetroBackground
import app.metroclock.ui.theme.LocalMetroSubtextColor
import app.metroclock.ui.theme.LocalMetroTextColor
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.util.rememberMetroHaptic

@Composable
fun AlarmsTab(
  alarms: List<AlarmEntity>,
  allAlarmsDisabled: Boolean,
  onAlarmToggle: (AlarmEntity, Boolean) -> Unit,
  onAlarmClick: (AlarmEntity) -> Unit,
  onTurnAlarmsBackOn: () -> Unit,
  modifier: Modifier = Modifier
) {
  val accentColor = LocalAccentColor.current
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val haptic = rememberMetroHaptic()

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("alarms_tab")
  ) {
    if (alarms.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 32.dp)
      ) {
        Text(
          text = "no alarms set",
          fontSize = 24.sp,
          fontWeight = FontWeight.Light,
          color = subtextColor
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
      ) {
        // If all alarms are disabled via 3-dot menu, display banner
        if (allAlarmsDisabled) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(accentColor)
                .clickable {
                  haptic()
                  onTurnAlarmsBackOn()
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("banner_all_alarms_disabled")
            ) {
              Text(
                text = "All alarms are turned off. Tap to re-enable.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MetroBlack
              )
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(4.dp))
        }

        itemsIndexed(alarms, key = { _, alarm -> alarm.id }) { index, alarm ->
          val isVisuallyEnabled = !allAlarmsDisabled && alarm.isEnabled

          MetroTurnstileEntrance(delayMillis = (index * 40).coerceAtMost(300)) {
            AlarmItemRow(
              alarm = alarm,
              isVisuallyEnabled = isVisuallyEnabled,
              onToggle = { isChecked ->
                haptic()
                onAlarmToggle(alarm, isChecked)
              },
              onClick = {
                haptic()
                onAlarmClick(alarm)
              }
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
fun AlarmItemRow(
  alarm: AlarmEntity,
  isVisuallyEnabled: Boolean,
  onToggle: (Boolean) -> Unit,
  onClick: () -> Unit
) {
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val accentColor = LocalAccentColor.current

  // Time-to-Alarm Countdown
  val countdownText = if (isVisuallyEnabled) {
    val nextMillis = AlarmScheduler.calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.repeatDays)
    val diffMillis = (nextMillis - System.currentTimeMillis()).coerceAtLeast(0)
    val diffMinutes = (diffMillis / (1000 * 60)).toInt()
    val hours = diffMinutes / 60
    val minutes = diffMinutes % 60
    when {
      hours > 0 && minutes > 0 -> "Rings in $hours hours $minutes minutes"
      hours > 0 -> "Rings in $hours hours"
      minutes > 0 -> "Rings in $minutes minutes"
      else -> "Rings in less than a minute"
    }
  } else null

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("alarm_item_${alarm.id}"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Row(
        verticalAlignment = Alignment.Bottom
      ) {
        Text(
          text = alarm.formattedTime,
          fontSize = 46.sp,
          fontWeight = FontWeight.Light,
          color = if (isVisuallyEnabled) textColor else subtextColor,
          letterSpacing = (-0.5).sp
        )
        Text(
          text = alarm.amPm,
          fontSize = 20.sp,
          fontWeight = FontWeight.Light,
          color = if (isVisuallyEnabled) textColor.copy(alpha = 0.75f) else subtextColor,
          modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
      }

      // Subtle live countdown ticker directly under time entry
      if (countdownText != null) {
        Text(
          text = countdownText,
          fontSize = 12.sp,
          fontWeight = FontWeight.Light,
          color = accentColor,
          modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
        )
      }

      Text(
        text = alarm.name,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = if (isVisuallyEnabled) textColor.copy(alpha = 0.9f) else subtextColor,
        modifier = Modifier.padding(top = 1.dp)
      )

      Text(
        text = alarm.repeatDescription,
        fontSize = 13.sp,
        fontWeight = FontWeight.Light,
        color = subtextColor,
        modifier = Modifier.padding(top = 1.dp)
      )
    }

    MetroToggle(
      checked = isVisuallyEnabled,
      onCheckedChange = onToggle,
      modifier = Modifier.padding(start = 16.dp),
      testTag = "toggle_alarm_${alarm.id}"
    )
  }
}
