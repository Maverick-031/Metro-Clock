package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmScheduler
import com.example.data.AlarmEntity
import com.example.ui.animation.MetroTurnstileEntrance
import com.example.ui.components.MetroAppBar
import com.example.ui.components.MetroAppBarAction
import com.example.ui.components.MetroSoundPickerDialog
import com.example.ui.components.MetroTimePicker
import com.example.ui.components.MetroToggle
import com.example.ui.components.SoundOption
import com.example.ui.components.buildMetroSoundOptions
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalMetroBackground
import com.example.ui.theme.LocalMetroDivider
import com.example.ui.theme.LocalMetroSubtextColor
import com.example.ui.theme.LocalMetroTextColor
import com.example.ui.theme.LocalMetroTileBg
import com.example.util.rememberMetroHaptic
import java.util.Calendar

@Composable
fun AddEditAlarmScreen(
  existingAlarm: AlarmEntity?,
  onSave: (
    hour: Int,
    minute: Int,
    name: String,
    repeatDays: Int,
    soundName: String,
    soundUri: String,
    snoozeMinutes: Int,
    skipIfCalendarEvent: Boolean,
    smartSkipLocation: Boolean
  ) -> Unit,
  onDelete: (() -> Unit)?,
  onCancel: () -> Unit,
  onPreviewSound: (String) -> Unit
) {
  BackHandler { onCancel() }

  val context = LocalContext.current
  val haptic = rememberMetroHaptic()

  val calendar = Calendar.getInstance()
  val defaultHour = existingAlarm?.hour ?: (calendar.get(Calendar.HOUR_OF_DAY) + 1) % 24
  val defaultMinute = existingAlarm?.minute ?: 0

  var hour by remember { mutableIntStateOf(defaultHour) }
  var minute by remember { mutableIntStateOf(defaultMinute) }
  var name by remember { mutableStateOf(existingAlarm?.name ?: "Alarm") }
  var repeatDays by remember { mutableIntStateOf(existingAlarm?.repeatDays ?: 0) }
  var soundName by remember { mutableStateOf(existingAlarm?.soundName ?: "Default Alarm") }
  var soundUri by remember { mutableStateOf(existingAlarm?.soundUri ?: "") }
  var snoozeMinutes by remember { mutableIntStateOf(existingAlarm?.snoozeMinutes ?: 10) }
  var skipIfCalendarEvent by remember { mutableStateOf(existingAlarm?.skipIfCalendarEvent ?: false) }
  var smartSkipLocation by remember { mutableStateOf(existingAlarm?.smartSkipLocation ?: false) }

  var showTimePicker by remember { mutableStateOf(false) }
  var showRepeatsScreen by remember { mutableStateOf(false) }
  var showSnoozeMenu by remember { mutableStateOf(false) }
  // In-app Metro sound picker (replaces the crashing system ringtone picker)
  var showSoundPicker by remember { mutableStateOf(false) }

  val accentColor = LocalAccentColor.current
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val bgColor = LocalMetroBackground.current
  val tileBg = LocalMetroTileBg.current
  val dividerColor = LocalMetroDivider.current

  // Calendar permission launcher
  val calendarPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    skipIfCalendarEvent = isGranted
  }

  // Sound options for the in-app picker: Default, every alarm/ringtone/notification
  // tone installed on the device, and Silent.
  val soundOptions = remember(context) { buildMetroSoundOptions(context) }

  // Formatted time string
  val formatted12Hour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
  val amPm = if (hour >= 12) "PM" else "AM"
  val timeDisplay = "$formatted12Hour:${minute.toString().padStart(2, '0')} $amPm"

  // Calculate rings in X hours Y minutes
  val nextTriggerMillis = AlarmScheduler.calculateNextTriggerTime(hour, minute, repeatDays)
  val diffMillis = (nextTriggerMillis - System.currentTimeMillis()).coerceAtLeast(0)
  val diffMinutes = (diffMillis / (1000 * 60)).toInt()
  val diffHours = diffMinutes / 60
  val remainingMins = diffMinutes % 60
  val ringsInText = when {
    diffHours > 0 && remainingMins > 0 -> "rings in $diffHours hours $remainingMins minutes"
    diffHours > 0 -> "rings in $diffHours hours"
    remainingMins > 0 -> "rings in $remainingMins minutes"
    else -> "rings in less than a minute"
  }

  val repeatDescription = when (repeatDays) {
    0 -> "only once"
    127 -> "every day"
    62 -> "weekdays"
    65 -> "weekends"
    else -> {
      val days = mutableListOf<String>()
      if ((repeatDays and 2) != 0) days.add("Mon")
      if ((repeatDays and 4) != 0) days.add("Tue")
      if ((repeatDays and 8) != 0) days.add("Wed")
      if ((repeatDays and 16) != 0) days.add("Thu")
      if ((repeatDays and 32) != 0) days.add("Fri")
      if ((repeatDays and 64) != 0) days.add("Sat")
      if ((repeatDays and 1) != 0) days.add("Sun")
      days.joinToString(", ")
    }
  }

  val snoozeOptions = listOf(5, 10, 15, 20, 30)

  // Full screen Time Picker
  if (showTimePicker) {
    MetroTimePicker(
      initialHour = hour,
      initialMinute = minute,
      onTimeSelected = { selectedH, selectedM ->
        hour = selectedH
        minute = selectedM
        showTimePicker = false
      },
      onDismiss = { showTimePicker = false }
    )
    return
  }

  // Full screen Repeats Picker
  if (showRepeatsScreen) {
    RepeatsScreen(
      initialRepeatDays = repeatDays,
      onSave = { updatedDays ->
        repeatDays = updatedDays
        showRepeatsScreen = false
      },
      onCancel = { showRepeatsScreen = false }
    )
    return
  }

  // In-app Metro Sound Picker (no system intent — avoids the post-selection crash)
  if (showSoundPicker) {
    val currentSelectedId = when {
      soundUri.isNotBlank() -> soundUri
      soundName == "Silent" -> ""
      else -> "default"
    }
    MetroSoundPickerDialog(
      title = "alarm sound",
      options = soundOptions,
      selectedId = currentSelectedId,
      onSelect = { option ->
        haptic()
        when {
          option.id.isEmpty() -> {
            soundUri = ""
            soundName = "Silent"
          }
          option.id == "default" -> {
            soundUri = ""
            soundName = "Default Alarm"
          }
          else -> {
            soundUri = option.uri ?: ""
            soundName = option.title
          }
        }
        showSoundPicker = false
      },
      onDismiss = { showSoundPicker = false }
    )
    return
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
      .statusBarsPadding()
      .testTag("add_edit_alarm_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 80.dp)
    ) {
      // Top Category Header: "METRO CLOCK" (all caps, matching the app's category style)
      Text(
        text = "METRO CLOCK",
        style = com.example.ui.theme.MetroTextStyle.Category,
        color = textColor,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
      )

      Text(
        text = if (existingAlarm == null) "new alarm" else "edit alarm",
        style = com.example.ui.theme.MetroTextStyle.PageTitle,
        color = textColor,
        modifier = Modifier.padding(start = 24.dp, bottom = 24.dp)
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        // 1. Name field
        MetroTurnstileEntrance(delayMillis = 0) {
          Column {
            Text(
              text = "Name",
              fontSize = 13.sp,
              color = subtextColor,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(tileBg)
                .border(1.dp, dividerColor)
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(
                  color = textColor,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_alarm_name")
              )
            }
          }
        }

        // 2. Time field
        MetroTurnstileEntrance(delayMillis = 30) {
          Column {
            Text(
              text = "Time",
              fontSize = 13.sp,
              color = subtextColor,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, subtextColor.copy(alpha = 0.5f))
                .clickable {
                  haptic()
                  showTimePicker = true
                }
                .padding(horizontal = 12.dp)
                .testTag("btn_select_alarm_time"),
              contentAlignment = Alignment.CenterStart
            ) {
              Text(
                text = timeDisplay,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
              )
            }
            Text(
              text = ringsInText,
              fontSize = 13.sp,
              color = accentColor,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }

        // 3. Repeats field
        MetroTurnstileEntrance(delayMillis = 60) {
          Column {
            Text(
              text = "Repeats",
              fontSize = 13.sp,
              color = subtextColor,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, subtextColor.copy(alpha = 0.5f))
                .clickable {
                  haptic()
                  showRepeatsScreen = true
                }
                .padding(horizontal = 12.dp)
                .testTag("btn_select_alarm_repeats"),
              contentAlignment = Alignment.CenterStart
            ) {
              Text(
                text = repeatDescription,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
              )
            }
          }
        }

        // 4. Sound field with In-App Metro Sound Picker
        MetroTurnstileEntrance(delayMillis = 90) {
          Column {
            Text(
              text = "Sound",
              fontSize = 13.sp,
              color = subtextColor,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .border(1.dp, subtextColor.copy(alpha = 0.5f))
                  .clickable {
                    haptic()
                    showSoundPicker = true
                  }
                  .padding(horizontal = 12.dp)
                  .testTag("btn_select_alarm_sound"),
                contentAlignment = Alignment.CenterStart
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = soundName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor
                  )
                  Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Pick sound",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              // Preview sound square button — plays the currently selected sound
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .border(1.dp, subtextColor.copy(alpha = 0.5f))
                  .clickable {
                    haptic()
                    val playableUri = when {
                      soundUri.isNotBlank() -> soundUri
                      soundName == "Silent" -> ""
                      else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""
                    }
                    onPreviewSound(playableUri)
                  }
                  .testTag("btn_preview_alarm_sound"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Preview Sound",
                  tint = textColor,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }
        }

        // 5. Snooze time field
        MetroTurnstileEntrance(delayMillis = 120) {
          Column {
            Text(
              text = "Snooze time",
              fontSize = 13.sp,
              color = subtextColor,
              modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, subtextColor.copy(alpha = 0.5f))
                .clickable {
                  haptic()
                  showSnoozeMenu = true
                }
                .padding(horizontal = 12.dp)
                .testTag("btn_select_snooze_time"),
              contentAlignment = Alignment.CenterStart
            ) {
              Text(
                text = "$snoozeMinutes minutes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
              )

              DropdownMenu(
                expanded = showSnoozeMenu,
                onDismissRequest = { showSnoozeMenu = false },
                modifier = Modifier
                  .background(tileBg)
                  .border(1.dp, dividerColor)
              ) {
                snoozeOptions.forEach { mins ->
                  DropdownMenuItem(
                    text = {
                      Text(
                        "$mins minutes",
                        color = if (snoozeMinutes == mins) accentColor else textColor,
                        fontSize = 16.sp
                      )
                    },
                    onClick = {
                      haptic()
                      snoozeMinutes = mins
                      showSnoozeMenu = false
                    }
                  )
                }
              }
            }
          }
        }

        // 6. Calendar Integration (Skip Holidays / All-day events)
        MetroTurnstileEntrance(delayMillis = 140) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Skip if calendar event exists",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
              )
              Text(
                text = "Automatically skips alarm on holidays and all-day events",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = subtextColor
              )
            }

            MetroToggle(
              checked = skipIfCalendarEvent,
              onCheckedChange = { checked ->
                haptic()
                if (checked) {
                  if (ContextCompat.checkSelfPermission(
                      context,
                      Manifest.permission.READ_CALENDAR
                    ) != PackageManager.PERMISSION_GRANTED
                  ) {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                  } else {
                    skipIfCalendarEvent = true
                  }
                } else {
                  skipIfCalendarEvent = false
                }
              },
              modifier = Modifier.padding(start = 16.dp),
              testTag = "toggle_skip_calendar"
            )
          }
        }

        // 7. Smart Skip / Location Aware
        MetroTurnstileEntrance(delayMillis = 160) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Smart Skip (away from home)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
              )
              Text(
                text = "Pauses alarm when vacationing or away based on Wi-Fi / GPS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = subtextColor
              )
            }

            MetroToggle(
              checked = smartSkipLocation,
              onCheckedChange = {
                haptic()
                smartSkipLocation = it
              },
              modifier = Modifier.padding(start = 16.dp),
              testTag = "toggle_smart_skip"
            )
          }
        }
      }
    }

    // Bottom Action App Bar: Save (Floppy disk) and Cancel (X)
    val actions = mutableListOf(
      MetroAppBarAction(
        icon = Icons.Default.Save,
        label = "Save",
        testTag = "app_bar_save_alarm",
        onClick = {
          haptic()
          onSave(
            hour,
            minute,
            name,
            repeatDays,
            soundName,
            soundUri,
            snoozeMinutes,
            skipIfCalendarEvent,
            smartSkipLocation
          )
        }
      ),
      MetroAppBarAction(
        icon = Icons.Default.Close,
        label = "Cancel",
        testTag = "app_bar_cancel_alarm",
        onClick = {
          haptic()
          onCancel()
        }
      )
    )

    if (onDelete != null) {
      actions.add(
        MetroAppBarAction(
          icon = Icons.Default.Delete,
          label = "Delete",
          testTag = "app_bar_delete_alarm",
          onClick = {
            haptic()
            onDelete()
          }
        )
      )
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      MetroAppBar(actions = actions)
    }
  }
}
