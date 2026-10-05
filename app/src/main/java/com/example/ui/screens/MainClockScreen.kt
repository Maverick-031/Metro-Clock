package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ClockViewModel
import com.example.ui.components.MetroAppBar
import com.example.ui.components.MetroAppBarAction
import com.example.ui.components.MetroMenuItem
import com.example.ui.components.TimerLengthDialog
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.LocalMetroBackground
import com.example.ui.theme.LocalMetroDivider
import com.example.ui.theme.LocalMetroSubtextColor
import com.example.ui.theme.LocalMetroTextColor
import com.example.ui.theme.LocalMetroTileBg
import com.example.util.rememberMetroHaptic
import kotlinx.coroutines.launch

@Composable
fun MainClockScreen(
  viewModel: ClockViewModel,
  onNavigateToSettings: () -> Unit,
  onNavigateToAddAlarm: () -> Unit,
  onNavigateToEditAlarm: (Long) -> Unit
) {
  val context = LocalContext.current
  val alarms by viewModel.alarms.collectAsStateWithLifecycle()
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val stopwatchState by viewModel.stopwatchState.collectAsStateWithLifecycle()
  val showTimerLengthDialog by viewModel.showTimerLengthDialog.collectAsStateWithLifecycle()
  val allAlarmsDisabled by viewModel.allAlarmsDisabled.collectAsStateWithLifecycle()
  val worldCities by viewModel.worldCities.collectAsStateWithLifecycle()

  var showAddWorldCityDialog by remember { mutableStateOf(false) }
  var showBatteryDialog by remember { mutableStateOf(false) }
  val haptic = rememberMetroHaptic()

  // Requirement: Check if app is ignoring battery optimizations
  LaunchedEffect(Unit) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
      showBatteryDialog = true
    }
  }

  val tabs = listOf("alarms", "timer", "stopwatch", "world clock")
  val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
  val coroutineScope = rememberCoroutineScope()

  // Requirement: Use small 't' in the beginning: "turn all alarms off"
  val turnAlarmsLabel = if (allAlarmsDisabled) "turn alarms on" else "turn all alarms off"

  val overflowMenuItems = listOf(
    MetroMenuItem(
      label = turnAlarmsLabel,
      testTag = "menu_toggle_all_alarms",
      onClick = {
        haptic()
        viewModel.toggleAllAlarmsDisabled()
      }
    ),
    MetroMenuItem(
      label = "settings",
      testTag = "menu_settings",
      onClick = {
        haptic()
        onNavigateToSettings()
      }
    )
  )

  val textColor = LocalMetroTextColor.current
  val subtextColor = LocalMetroSubtextColor.current
  val bgColor = LocalMetroBackground.current
  val tileBg = LocalMetroTileBg.current
  val dividerColor = LocalMetroDivider.current
  val accentColor = LocalAccentColor.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor)
      .statusBarsPadding()
      .testTag("main_clock_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Top Bar: Replaced "ALARMS" with "Metro Clock", removed the top 3-dot menu
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Metro Clock",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp,
          color = textColor
        )
      }

      // Windows Phone Pivot Headers: "alarms", "timer", "stopwatch", "world clock"
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, bottom = 12.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        tabs.forEachIndexed { index, title ->
          val isSelected = pagerState.currentPage == index
          Text(
            text = title,
            fontSize = if (isSelected) 44.sp else 30.sp,
            fontWeight = FontWeight.Light,
            color = if (isSelected) textColor else subtextColor,
            letterSpacing = (-0.5).sp,
            modifier = Modifier
              .clickable {
                haptic()
                coroutineScope.launch {
                  pagerState.animateScrollToPage(index)
                }
              }
              .testTag("tab_header_$title")
          )
        }
      }

      // Swipable Content Pager with 4 Tabs
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) { page ->
        when (page) {
          0 -> AlarmsTab(
            alarms = alarms,
            allAlarmsDisabled = allAlarmsDisabled,
            onAlarmToggle = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
            onAlarmClick = { alarm -> onNavigateToEditAlarm(alarm.id) },
            onTurnAlarmsBackOn = { viewModel.toggleAllAlarmsDisabled() }
          )
          1 -> TimerTab(
            timerState = timerState,
            onTapTime = {
              haptic()
              viewModel.showTimerLength(true)
            },
            onSelectQuickSet = { seconds ->
              haptic()
              viewModel.setTimerDuration(seconds)
            }
          )
          2 -> StopwatchTab(
            stopwatchState = stopwatchState
          )
          3 -> WorldClockTab(
            cities = worldCities,
            onAddCity = { city -> viewModel.addWorldCity(city) },
            onRemoveCity = { city -> viewModel.removeWorldCity(city) }
          )
        }
      }

      // Bottom Adaptive Application Bar (with bottom 3-dot overflow menu)
      val currentActions = when (pagerState.currentPage) {
        0 -> listOf(
          MetroAppBarAction(
            icon = Icons.Default.Add,
            label = "Add Alarm",
            testTag = "app_bar_add_alarm",
            onClick = {
              haptic()
              onNavigateToAddAlarm()
            }
          )
        )
        1 -> listOf(
          if (timerState.isRunning) {
            MetroAppBarAction(
              icon = Icons.Default.Pause,
              label = "Pause",
              testTag = "app_bar_pause_timer",
              onClick = {
                haptic()
                viewModel.pauseTimer()
              }
            )
          } else {
            MetroAppBarAction(
              icon = Icons.Default.PlayArrow,
              label = "Start",
              testTag = "app_bar_start_timer",
              onClick = {
                haptic()
                viewModel.startTimer()
              }
            )
          },
          MetroAppBarAction(
            icon = Icons.Default.Refresh,
            label = "Reset",
            testTag = "app_bar_reset_timer",
            onClick = {
              haptic()
              viewModel.resetTimer()
            }
          )
        )
        2 -> listOf(
          if (stopwatchState.isRunning) {
            MetroAppBarAction(
              icon = Icons.Default.Pause,
              label = "Pause",
              testTag = "app_bar_pause_stopwatch",
              onClick = {
                haptic()
                viewModel.pauseStopwatch()
              }
            )
          } else {
            MetroAppBarAction(
              icon = Icons.Default.PlayArrow,
              label = "Start",
              testTag = "app_bar_start_stopwatch",
              onClick = {
                haptic()
                viewModel.startStopwatch()
              }
            )
          },
          MetroAppBarAction(
            icon = Icons.Default.Flag,
            label = "Lap",
            testTag = "app_bar_lap_stopwatch",
            enabled = stopwatchState.isRunning,
            onClick = {
              haptic()
              viewModel.recordStopwatchLap()
            }
          ),
          MetroAppBarAction(
            icon = Icons.Default.Refresh,
            label = "Reset",
            testTag = "app_bar_reset_stopwatch",
            onClick = {
              haptic()
              viewModel.resetStopwatch()
            }
          )
        )
        else -> listOf(
          MetroAppBarAction(
            icon = Icons.Default.Add,
            label = "Add City",
            testTag = "app_bar_add_city",
            onClick = {
              haptic()
              showAddWorldCityDialog = true
            }
          )
        )
      }

      MetroAppBar(
        actions = currentActions,
        menuItems = overflowMenuItems
      )
    }

    // Timer length overlay dialog
    if (showTimerLengthDialog) {
      TimerLengthDialog(
        initialTotalSeconds = timerState.totalSeconds,
        onConfirm = { seconds ->
          haptic()
          viewModel.setTimerDuration(seconds)
          viewModel.showTimerLength(false)
        },
        onDismiss = {
          haptic()
          viewModel.showTimerLength(false)
        }
      )
    }

    // World city dialog
    if (showAddWorldCityDialog) {
      AddCityDialog(
        currentCities = worldCities,
        onSelectCity = { city ->
          viewModel.addWorldCity(city)
          showAddWorldCityDialog = false
        },
        onDismiss = { showAddWorldCityDialog = false }
      )
    }

    // Battery Optimization Dialog
    if (showBatteryDialog) {
      Dialog(onDismissRequest = { showBatteryDialog = false }) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(tileBg)
            .border(2.dp, accentColor)
            .padding(24.dp)
            .testTag("dialog_battery_optimizations")
        ) {
          Column {
            Text(
              text = "BATTERY OPTIMIZATION",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp,
              color = accentColor
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "disable battery optimization",
              fontSize = 24.sp,
              fontWeight = FontWeight.Light,
              color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "To guarantee alarms ring exactly on time even when your phone is asleep, please exclude Metro Clock from battery restrictions.",
              fontSize = 14.sp,
              fontWeight = FontWeight.Normal,
              color = subtextColor,
              lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .background(accentColor)
                  .clickable {
                    haptic()
                    showBatteryDialog = false
                    try {
                      val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                      }
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      try {
                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(fallback)
                      } catch (e2: Exception) {
                        e2.printStackTrace()
                      }
                    }
                  }
                  .testTag("btn_battery_allow"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "allow",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Normal,
                  color = androidx.compose.ui.graphics.Color.Black
                )
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .border(1.dp, subtextColor)
                  .clickable {
                    haptic()
                    showBatteryDialog = false
                  }
                  .testTag("btn_battery_dismiss"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "later",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Normal,
                  color = textColor
                )
              }
            }
          }
        }
      }
    }
  }
}
