package app.metroclock.ui.screens

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.metroclock.ui.animation.MetroTurnstileEntrance
import app.metroclock.ui.components.MetroAppBar
import app.metroclock.ui.components.MetroAppBarAction
import app.metroclock.ui.components.MetroSoundPickerDialog
import app.metroclock.ui.components.MetroToggle
import app.metroclock.ui.components.buildMetroSoundOptions
import app.metroclock.ui.theme.AccentColor
import app.metroclock.ui.theme.LocalAccentColor
import app.metroclock.ui.theme.LocalMetroBackground
import app.metroclock.ui.theme.LocalMetroDivider
import app.metroclock.ui.theme.LocalMetroSubtextColor
import app.metroclock.ui.theme.LocalMetroTextColor
import app.metroclock.ui.theme.LocalMetroTileBg
import app.metroclock.ui.theme.MetroAccentsList
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroWhite
import app.metroclock.util.rememberMetroHaptic
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  selectedAccent: AccentColor,
  useDynamicColor: Boolean,
  isLightTheme: Boolean,
  onSelectAccent: (String) -> Unit,
  onToggleDynamicColor: (Boolean) -> Unit,
  onToggleLightTheme: (Boolean) -> Unit,
  // Alarm settings state & callbacks
  alarmVibrate: Boolean,
  onToggleAlarmVibrate: (Boolean) -> Unit,
  alarmSilenceAfter: String,
  onSelectSilenceAfter: (String) -> Unit,
  alarmSnoozeLength: Int,
  onUpdateSnoozeLength: (Int) -> Unit,
  alarmGradualVolume: String,
  onSelectGradualVolume: (String) -> Unit,
  alarmVolumeButtons: String,
  onSelectVolumeButtons: (String) -> Unit,
  // Smart Skip settings
  smartSkipEnabled: Boolean = false,
  onToggleSmartSkip: (Boolean) -> Unit = {},
  smartSkipMode: String = "wifi",
  onSelectSmartSkipMode: (String) -> Unit = {},
  smartSkipHomeWifi: String = "",
  onUpdateHomeWifi: (String) -> Unit = {},
  // Timer settings state & callbacks
  timerSoundTitle: String,
  onSelectTimerSound: (uri: String, title: String) -> Unit,
  timerGradualVolume: Boolean,
  onToggleTimerGradualVolume: (Boolean) -> Unit,
  timerVibrate: Boolean,
  onToggleTimerVibrate: (Boolean) -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val haptic = rememberMetroHaptic()

  // Requirement: Rename titles to "alarm", "timer", "colors"
  val tabs = listOf("alarm", "timer", "colors")
  val pagerState = rememberPagerState(initialPage = 0) { tabs.size }

  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val bgColor = LocalMetroBackground.current

  var showTimerSoundPicker by remember { mutableStateOf(false) }
  val soundOptions = remember { buildMetroSoundOptions(context) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
      .statusBarsPadding()
      .testTag("settings_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 72.dp)
    ) {
      // Top Category Header: "SETTINGS"
      Text(
        text = "SETTINGS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = textColor,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
      )

      // Tab Headers Row: "alarm", "timer", "colors"
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, bottom = 16.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        tabs.forEachIndexed { index, title ->
          val isSelected = pagerState.currentPage == index
          Text(
            text = title,
            fontSize = if (isSelected) 40.sp else 28.sp,
            fontWeight = FontWeight.Light,
            color = if (isSelected) textColor else subtextColor,
            letterSpacing = (-0.5).sp,
            modifier = Modifier
              .clickable {
                haptic()
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
              }
              .testTag("settings_tab_$title")
          )
        }
      }

      // HorizontalPager for the 3 tabs
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) { page ->
        when (page) {
          0 -> AlarmSettingsTab(
            alarmVibrate = alarmVibrate,
            onToggleAlarmVibrate = onToggleAlarmVibrate,
            alarmSilenceAfter = alarmSilenceAfter,
            onSelectSilenceAfter = onSelectSilenceAfter,
            alarmSnoozeLength = alarmSnoozeLength,
            onUpdateSnoozeLength = onUpdateSnoozeLength,
            alarmGradualVolume = alarmGradualVolume,
            onSelectGradualVolume = onSelectGradualVolume,
            alarmVolumeButtons = alarmVolumeButtons,
            onSelectVolumeButtons = onSelectVolumeButtons,
            smartSkipEnabled = smartSkipEnabled,
            onToggleSmartSkip = onToggleSmartSkip,
            smartSkipMode = smartSkipMode,
            onSelectSmartSkipMode = onSelectSmartSkipMode,
            smartSkipHomeWifi = smartSkipHomeWifi,
            onUpdateHomeWifi = onUpdateHomeWifi
          )
          1 -> TimerSettingsTab(
            timerSoundTitle = timerSoundTitle,
            onOpenSoundPicker = {
              haptic()
              showTimerSoundPicker = true
            },
            timerGradualVolume = timerGradualVolume,
            onToggleTimerGradualVolume = onToggleTimerGradualVolume,
            timerVibrate = timerVibrate,
            onToggleTimerVibrate = onToggleTimerVibrate
          )
          2 -> ColorSettingsTab(
            selectedAccent = selectedAccent,
            useDynamicColor = useDynamicColor,
            isLightTheme = isLightTheme,
            onSelectAccent = onSelectAccent,
            onToggleDynamicColor = onToggleDynamicColor,
            onToggleLightTheme = onToggleLightTheme
          )
        }
      }
    }

    // Bottom App Bar with Back Action
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      MetroAppBar(
        actions = listOf(
          MetroAppBarAction(
            icon = Icons.Default.ArrowBack,
            label = "Back",
            testTag = "settings_back",
            onClick = {
              haptic()
              onBack()
            }
          )
        )
      )
    }
  }

    // In-app sound picker dialog for timer
    if (showTimerSoundPicker) {
      val currentTimerUri = if (timerSoundTitle == "Silent") "" else if (timerSoundTitle == "Default alarm sound") "default" else timerSoundTitle
      MetroSoundPickerDialog(
        title = "timer sound",
        options = soundOptions,
        selectedId = currentTimerUri,
        onSelect = { option ->
          onSelectTimerSound(option.uri ?: "", option.title)
          showTimerSoundPicker = false
        },
        onDismiss = { showTimerSoundPicker = false }
      )
    }

}

// -------------------------------------------------------------------------
// Tab 1: Alarm Settings
// -------------------------------------------------------------------------
@Composable
fun AlarmSettingsTab(
  alarmVibrate: Boolean,
  onToggleAlarmVibrate: (Boolean) -> Unit,
  alarmSilenceAfter: String,
  onSelectSilenceAfter: (String) -> Unit,
  alarmSnoozeLength: Int,
  onUpdateSnoozeLength: (Int) -> Unit,
  alarmGradualVolume: String,
  onSelectGradualVolume: (String) -> Unit,
  alarmVolumeButtons: String,
  onSelectVolumeButtons: (String) -> Unit,
  smartSkipEnabled: Boolean,
  onToggleSmartSkip: (Boolean) -> Unit,
  smartSkipMode: String,
  onSelectSmartSkipMode: (String) -> Unit,
  smartSkipHomeWifi: String,
  onUpdateHomeWifi: (String) -> Unit
) {
  val accentColor = LocalAccentColor.current
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val tileBg = LocalMetroTileBg.current
  val dividerColor = LocalMetroDivider.current
  val haptic = rememberMetroHaptic()

  var showSilenceMenu by remember { mutableStateOf(false) }
  var showGradualMenu by remember { mutableStateOf(false) }
  var showVolumeButtonsMenu by remember { mutableStateOf(false) }

  val silenceOptions = listOf("1", "5", "10", "15", "20", "25", "never")
  val gradualOptions = listOf("never", "5", "10", "15", "30", "45", "60")
  val volumeButtonOptions = listOf("control volume", "snooze", "stop", "do nothing")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp)
  ) {
    // 1. Vibrate Toggle
    MetroTurnstileEntrance(delayMillis = 0) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Vibrate",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
          )
          Text(
            text = "Vibrate on alarm ringing",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = subtextColor
          )
        }
        MetroToggle(
          checked = alarmVibrate,
          onCheckedChange = {
            haptic()
            onToggleAlarmVibrate(it)
          },
          modifier = Modifier.padding(start = 16.dp),
          testTag = "toggle_alarm_vibrate"
        )
      }
    }

    // 2. Silence After Dropdown
    MetroTurnstileEntrance(delayMillis = 40) {
      Column {
        Text(
          text = "Silence after",
          fontSize = 18.sp,
          fontWeight = FontWeight.Normal,
          color = textColor
        )
        Text(
          text = "Automatically stops alarm after duration",
          fontSize = 13.sp,
          fontWeight = FontWeight.Light,
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
              showSilenceMenu = true
            }
            .padding(horizontal = 12.dp)
            .testTag("dropdown_silence_after"),
          contentAlignment = Alignment.CenterStart
        ) {
          val label = if (alarmSilenceAfter == "never") "never minutes" else "$alarmSilenceAfter minutes"
          Text(
            text = label,
            fontSize = 16.sp,
            color = textColor
          )
          DropdownMenu(
            expanded = showSilenceMenu,
            onDismissRequest = { showSilenceMenu = false },
            modifier = Modifier.background(tileBg).border(1.dp, dividerColor)
          ) {
            silenceOptions.forEach { opt ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = if (opt == "never") "never" else "$opt minutes",
                    color = if (alarmSilenceAfter == opt) accentColor else textColor,
                    fontSize = 16.sp
                  )
                },
                onClick = {
                  haptic()
                  onSelectSilenceAfter(opt)
                  showSilenceMenu = false
                }
              )
            }
          }
        }
      }
    }

    // 3. Snooze Length Input Field
    MetroTurnstileEntrance(delayMillis = 80) {
      Column {
        Text(
          text = "Snooze length",
          fontSize = 18.sp,
          fontWeight = FontWeight.Normal,
          color = textColor
        )
        Text(
          text = "Duration in minutes",
          fontSize = 13.sp,
          fontWeight = FontWeight.Light,
          color = subtextColor,
          modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(tileBg)
            .border(1.dp, dividerColor)
            .padding(horizontal = 12.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          BasicTextField(
            value = alarmSnoozeLength.toString(),
            onValueChange = { str ->
              val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
              onUpdateSnoozeLength(num.coerceIn(1, 60))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
              color = textColor,
              fontSize = 16.sp,
              fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_snooze_length")
          )
        }
      }
    }

    // 4. Gradually Increase Volume Dropdown
    MetroTurnstileEntrance(delayMillis = 120) {
      Column {
        Text(
          text = "Gradually increase volume",
          fontSize = 18.sp,
          fontWeight = FontWeight.Normal,
          color = textColor
        )
        Text(
          text = "Fade in alarm sound over time",
          fontSize = 13.sp,
          fontWeight = FontWeight.Light,
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
              showGradualMenu = true
            }
            .padding(horizontal = 12.dp)
            .testTag("dropdown_gradual_volume"),
          contentAlignment = Alignment.CenterStart
        ) {
          val label = if (alarmGradualVolume == "never") "never" else "$alarmGradualVolume seconds"
          Text(
            text = label,
            fontSize = 16.sp,
            color = textColor
          )
          DropdownMenu(
            expanded = showGradualMenu,
            onDismissRequest = { showGradualMenu = false },
            modifier = Modifier.background(tileBg).border(1.dp, dividerColor)
          ) {
            gradualOptions.forEach { opt ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = if (opt == "never") "never" else "$opt seconds",
                    color = if (alarmGradualVolume == opt) accentColor else textColor,
                    fontSize = 16.sp
                  )
                },
                onClick = {
                  haptic()
                  onSelectGradualVolume(opt)
                  showGradualMenu = false
                }
              )
            }
          }
        }
      }
    }

    // 5. Volume Buttons During Alarm Dropdown
    MetroTurnstileEntrance(delayMillis = 160) {
      Column {
        Text(
          text = "Volume buttons during alarm",
          fontSize = 18.sp,
          fontWeight = FontWeight.Normal,
          color = textColor
        )
        Text(
          text = "Action when pressing physical volume keys",
          fontSize = 13.sp,
          fontWeight = FontWeight.Light,
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
              showVolumeButtonsMenu = true
            }
            .padding(horizontal = 12.dp)
            .testTag("dropdown_volume_buttons"),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = alarmVolumeButtons,
            fontSize = 16.sp,
            color = textColor
          )
          DropdownMenu(
            expanded = showVolumeButtonsMenu,
            onDismissRequest = { showVolumeButtonsMenu = false },
            modifier = Modifier.background(tileBg).border(1.dp, dividerColor)
          ) {
            volumeButtonOptions.forEach { opt ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = opt,
                    color = if (alarmVolumeButtons == opt) accentColor else textColor,
                    fontSize = 16.sp
                  )
                },
                onClick = {
                  haptic()
                  onSelectVolumeButtons(opt)
                  showVolumeButtonsMenu = false
                }
              )
            }
          }
        }
      }
    }

    // 6. Smart Skip Section
    MetroTurnstileEntrance(delayMillis = 200) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Smart Skip (Vacation Mode)",
              fontSize = 18.sp,
              fontWeight = FontWeight.Normal,
              color = textColor
            )
            Text(
              text = "Automatically pause alarms when off-site or away",
              fontSize = 13.sp,
              fontWeight = FontWeight.Light,
              color = subtextColor
            )
          }
          MetroToggle(
            checked = smartSkipEnabled,
            onCheckedChange = {
              haptic()
              onToggleSmartSkip(it)
            },
            modifier = Modifier.padding(start = 16.dp),
            testTag = "toggle_smart_skip_settings"
          )
        }

        if (smartSkipEnabled) {
          // Home Wi-Fi SSID
          Column {
            Text(
              text = "Home Wi-Fi network name (SSID)",
              fontSize = 14.sp,
              fontWeight = FontWeight.Normal,
              color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                value = smartSkipHomeWifi,
                onValueChange = onUpdateHomeWifi,
                singleLine = true,
                textStyle = TextStyle(
                  color = textColor,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_home_wifi")
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------------------
// Tab 2: Timer Settings
// -------------------------------------------------------------------------
@Composable
fun TimerSettingsTab(
  timerSoundTitle: String,
  onOpenSoundPicker: () -> Unit,
  timerGradualVolume: Boolean,
  onToggleTimerGradualVolume: (Boolean) -> Unit,
  timerVibrate: Boolean,
  onToggleTimerVibrate: (Boolean) -> Unit
) {
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val haptic = rememberMetroHaptic()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // 1. Timer Sound Picker Button
    MetroTurnstileEntrance(delayMillis = 0) {
      Column {
        Text(
          text = "Timer sound",
          fontSize = 18.sp,
          fontWeight = FontWeight.Normal,
          color = textColor
        )
        Text(
          text = "Select system ringtone or alert sound",
          fontSize = 13.sp,
          fontWeight = FontWeight.Light,
          color = subtextColor,
          modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, subtextColor.copy(alpha = 0.5f))
            .clickable(onClick = onOpenSoundPicker)
            .padding(horizontal = 12.dp)
            .testTag("btn_timer_sound_picker"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = timerSoundTitle,
            fontSize = 16.sp,
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
    }

    // 2. Gradually Increase Volume Toggle
    MetroTurnstileEntrance(delayMillis = 40) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Gradually increase volume",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
          )
          Text(
            text = "Gentle fade-in when timer completes",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = subtextColor
          )
        }
        MetroToggle(
          checked = timerGradualVolume,
          onCheckedChange = {
            haptic()
            onToggleTimerGradualVolume(it)
          },
          modifier = Modifier.padding(start = 16.dp),
          testTag = "toggle_timer_gradual_volume"
        )
      }
    }

    // 3. Timer Vibrate Toggle
    MetroTurnstileEntrance(delayMillis = 80) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Timer vibrate",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
          )
          Text(
            text = "Vibrate when timer completes",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = subtextColor
          )
        }
        MetroToggle(
          checked = timerVibrate,
          onCheckedChange = {
            haptic()
            onToggleTimerVibrate(it)
          },
          modifier = Modifier.padding(start = 16.dp),
          testTag = "toggle_timer_vibrate"
        )
      }
    }
  }
}

// -------------------------------------------------------------------------
// Tab 3: Color Settings ("colors")
// -------------------------------------------------------------------------
@Composable
fun ColorSettingsTab(
  selectedAccent: AccentColor,
  useDynamicColor: Boolean,
  isLightTheme: Boolean,
  onSelectAccent: (String) -> Unit,
  onToggleDynamicColor: (Boolean) -> Unit,
  onToggleLightTheme: (Boolean) -> Unit
) {
  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val haptic = rememberMetroHaptic()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp)
  ) {
    // 1. Light Theme Toggle (Requirement from Section 1)
    MetroTurnstileEntrance(delayMillis = 0) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Light Theme",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
          )
          Text(
            text = if (isLightTheme) "White background with dark text" else "Pure black background (#000000)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = subtextColor
          )
        }

        MetroToggle(
          checked = isLightTheme,
          onCheckedChange = {
            haptic()
            onToggleLightTheme(it)
          },
          modifier = Modifier.padding(start = 16.dp),
          testTag = "toggle_light_theme"
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Dynamic color toggle (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      MetroTurnstileEntrance(delayMillis = 30) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Material You Dynamic Colors",
              fontSize = 18.sp,
              fontWeight = FontWeight.Normal,
              color = textColor
            )
            Text(
              text = "Match system wallpaper palette",
              fontSize = 13.sp,
              fontWeight = FontWeight.Light,
              color = subtextColor
            )
          }

          MetroToggle(
            checked = useDynamicColor,
            onCheckedChange = {
              haptic()
              onToggleDynamicColor(it)
            },
            modifier = Modifier.padding(start = 16.dp),
            testTag = "toggle_dynamic_colors"
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }

    Text(
      text = "ACCENT COLOR",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
      color = subtextColor,
      modifier = Modifier.padding(vertical = 8.dp)
    )

    // Grid of color swatches: Yellow, Orange, Blue, Green, Purple, Red, White, Amber, etc.
    LazyVerticalGrid(
      columns = GridCells.Fixed(4),
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(MetroAccentsList, key = { it.id }) { accent ->
        val isSelected = !useDynamicColor && accent.id == selectedAccent.id
        AccentSwatchTile(
          accent = accent,
          isSelected = isSelected,
          onClick = {
            haptic()
            if (useDynamicColor) {
              onToggleDynamicColor(false)
            }
            onSelectAccent(accent.id)
          }
        )
      }
    }
  }
}

@Composable
fun AccentSwatchTile(
  accent: AccentColor,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val textColor = if (accent.id == "white" || accent.id == "yellow") MetroBlack else MetroWhite
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .background(accent.color)
      .border(
        width = if (isSelected) 3.5.dp else 0.dp,
        color = if (isSelected) (if (accent.id == "white") MetroBlack else MetroWhite) else Color.Transparent
      )
      .clickable(onClick = onClick)
      .padding(6.dp)
      .testTag("swatch_${accent.id}"),
    contentAlignment = Alignment.BottomStart
  ) {
    Text(
      text = accent.name,
      fontSize = 11.sp,
      fontWeight = FontWeight.Normal,
      color = textColor
    )
  }
}
