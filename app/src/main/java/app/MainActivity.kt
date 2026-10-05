package app.metroclock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.metroclock.receiver.AlarmReceiver
import app.metroclock.ui.ClockViewModel
import app.metroclock.ui.Screen
import app.metroclock.ui.animation.metroTurnstileTransition
import app.metroclock.ui.screens.AddEditAlarmScreen
import app.metroclock.ui.screens.AlarmTriggeredScreen
import app.metroclock.ui.screens.MainClockScreen
import app.metroclock.ui.screens.SettingsScreen
import app.metroclock.ui.screens.TimerFinishedScreen
import app.metroclock.ui.theme.MetroBlack
import app.metroclock.ui.theme.MetroClockTheme

class MainActivity : ComponentActivity() {

  companion object {
    const val EXTRA_DESTINATION = "extra_destination"
    const val DEST_ALARM_TRIGGERED = "dest_alarm_triggered"
    const val DEST_TIMER_FINISHED = "dest_timer_finished"
    const val EXTRA_TIMER_SECONDS = "extra_timer_seconds"
  }

  private val viewModel: ClockViewModel by viewModels {
    val app = application as ClockApplication
    ClockViewModel.provideFactory(app.container)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Safety net: if the app process is recreated while no alarm/timer screen is shown,
    // make sure nothing keeps ringing or vibrating in the background.
    val app = application as ClockApplication
    if (intent?.getStringExtra(EXTRA_DESTINATION) == null) {
      app.container.soundPlayer.stopSound()
    }

    // Keep screen on and show over lock screen if triggered
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      // Allow the app to draw over other apps (for full screen alarm/timer)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.setDecorFitsSystemWindows(false)
      }
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
      )
    }
​
    // Ensure window stays on for alarm/timer screens
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    handleIncomingIntent(intent)

    setContent {
      val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
      val useDynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
      val isLightTheme by viewModel.isLightTheme.collectAsStateWithLifecycle()
      val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
      val alarms by viewModel.alarms.collectAsStateWithLifecycle()

      // Alarm Settings state
      val alarmVibrate by viewModel.alarmVibrate.collectAsStateWithLifecycle()
      val alarmSilenceAfter by viewModel.alarmSilenceAfter.collectAsStateWithLifecycle()
      val alarmSnoozeLength by viewModel.alarmSnoozeLength.collectAsStateWithLifecycle()
      val alarmGradualVolume by viewModel.alarmGradualVolume.collectAsStateWithLifecycle()
      val alarmVolumeButtons by viewModel.alarmVolumeButtons.collectAsStateWithLifecycle()

      // Timer Settings state
      val timerSoundTitle by viewModel.timerSoundTitle.collectAsStateWithLifecycle()
      val timerGradualVolume by viewModel.timerGradualVolume.collectAsStateWithLifecycle()
      val timerVibrate by viewModel.timerVibrate.collectAsStateWithLifecycle()

      // Smart Skip state
      val smartSkipEnabled by viewModel.smartSkipEnabled.collectAsStateWithLifecycle()
      val smartSkipMode by viewModel.smartSkipMode.collectAsStateWithLifecycle()
      val smartSkipHomeWifi by viewModel.smartSkipHomeWifi.collectAsStateWithLifecycle()

      // Runtime permission for notifications on Android 13+ (TIRAMISU)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
          ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied handled gracefully */ }

        LaunchedEffect(Unit) {
          if (ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      MetroClockTheme(
        accentColor = selectedAccent.color,
        useDynamicColor = useDynamicColor,
        isLightTheme = isLightTheme
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = if (isLightTheme) androidx.compose.ui.graphics.Color.White else MetroBlack
        ) {
          // Classic Windows Phone Turnstile Animated Transitions
          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = metroTurnstileTransition(),
            label = "MetroScreenTurnstile"
          ) { screen ->
            when (screen) {
              is Screen.Main -> {
                MainClockScreen(
                  viewModel = viewModel,
                  onNavigateToSettings = { viewModel.navigateTo(Screen.Settings) },
                  onNavigateToAddAlarm = { viewModel.navigateTo(Screen.AddEditAlarm(null)) },
                  onNavigateToEditAlarm = { alarmId -> viewModel.navigateTo(Screen.AddEditAlarm(alarmId)) }
                )
              }

              is Screen.AddEditAlarm -> {
                val existing = alarms.firstOrNull { it.id == screen.alarmId }
                AddEditAlarmScreen(
                  existingAlarm = existing,
                  onSave = { hour, minute, name, repeats, sound, soundUri, snooze, skipCalendar, smartSkip ->
                    viewModel.saveAlarm(
                      id = screen.alarmId,
                      hour = hour,
                      minute = minute,
                      name = name,
                      repeatDays = repeats,
                      soundName = sound,
                      soundUri = soundUri,
                      snoozeMinutes = snooze,
                      skipIfCalendarEvent = skipCalendar,
                      smartSkipLocation = smartSkip
                    )
                  },
                  onDelete = if (existing != null) {
                    { viewModel.deleteAlarm(existing) }
                  } else null,
                  onCancel = { viewModel.navigateBack() },
                  onPreviewSound = { uri -> viewModel.previewSoundUri(uri) }
                )
              }

              is Screen.Settings -> {
                SettingsScreen(
                  selectedAccent = selectedAccent,
                  useDynamicColor = useDynamicColor,
                  isLightTheme = isLightTheme,
                  onSelectAccent = { id -> viewModel.selectAccentColor(id) },
                  onToggleDynamicColor = { useDyn -> viewModel.setUseDynamicColor(useDyn) },
                  onToggleLightTheme = { isLight -> viewModel.setLightTheme(isLight) },
                  alarmVibrate = alarmVibrate,
                  onToggleAlarmVibrate = { viewModel.setAlarmVibrate(it) },
                  alarmSilenceAfter = alarmSilenceAfter,
                  onSelectSilenceAfter = { viewModel.setAlarmSilenceAfter(it) },
                  alarmSnoozeLength = alarmSnoozeLength,
                  onUpdateSnoozeLength = { viewModel.setAlarmSnoozeLength(it) },
                  alarmGradualVolume = alarmGradualVolume,
                  onSelectGradualVolume = { viewModel.setAlarmGradualVolume(it) },
                  alarmVolumeButtons = alarmVolumeButtons,
                  onSelectVolumeButtons = { viewModel.setAlarmVolumeButtons(it) },
                  smartSkipEnabled = smartSkipEnabled,
                  onToggleSmartSkip = { viewModel.setSmartSkipEnabled(it) },
                  smartSkipMode = smartSkipMode,
                  onSelectSmartSkipMode = { viewModel.setSmartSkipMode(it) },
                  smartSkipHomeWifi = smartSkipHomeWifi,
                  onUpdateHomeWifi = { viewModel.setSmartSkipHomeWifi(it) },
                  timerSoundTitle = timerSoundTitle,
                  onSelectTimerSound = { uri, title -> viewModel.setTimerSound(uri, title) },
                  timerGradualVolume = timerGradualVolume,
                  onToggleTimerGradualVolume = { viewModel.setTimerGradualVolume(it) },
                  timerVibrate = timerVibrate,
                  onToggleTimerVibrate = { viewModel.setTimerVibrate(it) },
                  onBack = { viewModel.navigateBack() }
                )
              }

              is Screen.AlarmTriggered -> {
                AlarmTriggeredScreen(
                  alarmId = screen.alarmId,
                  alarmName = screen.alarmName,
                  hour = screen.hour,
                  minute = screen.minute,
                  snoozeMinutes = screen.snoozeMinutes,
                  onSnooze = { mins -> viewModel.snoozeAlarm(screen.alarmId, screen.alarmName, mins) },
                  onDismiss = { viewModel.dismissAlarm() }
                )
              }

              is Screen.TimerFinished -> {
                TimerFinishedScreen(
                  totalSeconds = screen.totalSeconds,
                  onRestart = { viewModel.restartTimer() },
                  onDismiss = { viewModel.dismissTimerFinished() }
                )
              }
            }
          }
        }
      }
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    val currentScreen = viewModel.currentScreen.value
    if (currentScreen is Screen.AlarmTriggered) {
      if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
        val action = viewModel.alarmVolumeButtons.value
        when (action) {
          "snooze" -> {
            viewModel.snoozeAlarm(
              currentScreen.alarmId,
              currentScreen.alarmName,
              currentScreen.snoozeMinutes
            )
            return true
          }
          "stop" -> {
            viewModel.dismissAlarm()
            return true
          }
          "do nothing" -> {
            return true
          }
          else -> {
            // "control volume"
            return super.onKeyDown(keyCode, event)
          }
        }
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent == null) return
    val destination = intent.getStringExtra(EXTRA_DESTINATION)
    when (destination) {
      DEST_ALARM_TRIGGERED -> {
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
        val alarmName = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_NAME) ?: "Alarm"
        val hour = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, 12)
        val minute = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, 0)
        val snooze = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 10)
        viewModel.navigateTo(
          Screen.AlarmTriggered(
            alarmId = alarmId,
            alarmName = alarmName,
            hour = hour,
            minute = minute,
            snoozeMinutes = snooze
          )
        )
      }
      DEST_TIMER_FINISHED -> {
        val seconds = intent.getIntExtra(EXTRA_TIMER_SECONDS, 60)
        viewModel.navigateTo(Screen.TimerFinished(seconds))
      }
    }
  }
}
