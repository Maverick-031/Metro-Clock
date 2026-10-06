package app.metroclock

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    const val EXTRA_DESTINATION =
      "extra_destination"

    const val DEST_ALARM_TRIGGERED =
      "dest_alarm_triggered"

    const val DEST_TIMER_FINISHED =
      "dest_timer_finished"

    const val EXTRA_TIMER_SECONDS =
      "extra_timer_seconds"
  }

  private val appContainer: AppContainer
    get() = (application as ClockApplication).container

  private val viewModel: ClockViewModel by viewModels {
    ClockViewModel.provideFactory(appContainer)
  }

  private val keyguardManager: KeyguardManager by lazy {
    getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
  }

  // ---------------------------------------------------------------------------
  // Activity lifecycle
  // ---------------------------------------------------------------------------

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    val launchedForTriggeredScreen =
      isAlarmOrTimerIntent(intent)

    /*
     * If the app was opened normally, clear any stale playback that may have
     * survived an earlier Activity or UI state.
     *
     * Do not stop sound when the Activity was launched by an alarm or timer.
     */
    if (!launchedForTriggeredScreen) {
      appContainer.soundPlayer.stopSound()
      clearTriggeredWindowFlags()
    } else {
      /*
       * Apply these flags before the first Compose frame so the alarm or timer
       * UI can appear above the lock screen without flashing the keyguard.
       */
      showTriggeredWindow()
    }

    /*
     * Process the Activity intent before creating the Compose UI. This ensures
     * the first composition receives the correct destination.
     *
     * handleIncomingIntent() also starts self-heal playback if the receiver or
     * service did not already start it.
     */
    handleIncomingIntent(intent)

    setContent {
      val selectedAccent by
        viewModel.selectedAccent.collectAsStateWithLifecycle()

      val useDynamicColor by
        viewModel.useDynamicColor.collectAsStateWithLifecycle()

      val isLightTheme by
        viewModel.isLightTheme.collectAsStateWithLifecycle()

      val currentScreen by
        viewModel.currentScreen.collectAsStateWithLifecycle()

      val alarms by
        viewModel.alarms.collectAsStateWithLifecycle()

      // -----------------------------------------------------------------------
      // Alarm settings
      // -----------------------------------------------------------------------

      val alarmVibrate by
        viewModel.alarmVibrate.collectAsStateWithLifecycle()

      val alarmSilenceAfter by
        viewModel.alarmSilenceAfter.collectAsStateWithLifecycle()

      val alarmSnoozeLength by
        viewModel.alarmSnoozeLength.collectAsStateWithLifecycle()

      val alarmGradualVolume by
        viewModel.alarmGradualVolume.collectAsStateWithLifecycle()

      val alarmVolumeButtons by
        viewModel.alarmVolumeButtons.collectAsStateWithLifecycle()

      // -----------------------------------------------------------------------
      // Timer settings
      // -----------------------------------------------------------------------

      val timerSoundTitle by
        viewModel.timerSoundTitle.collectAsStateWithLifecycle()

      val timerGradualVolume by
        viewModel.timerGradualVolume.collectAsStateWithLifecycle()

      val timerVibrate by
        viewModel.timerVibrate.collectAsStateWithLifecycle()

      // -----------------------------------------------------------------------
      // Smart Skip settings
      // -----------------------------------------------------------------------

      val smartSkipEnabled by
        viewModel.smartSkipEnabled.collectAsStateWithLifecycle()

      val smartSkipMode by
        viewModel.smartSkipMode.collectAsStateWithLifecycle()

      val smartSkipHomeWifi by
        viewModel.smartSkipHomeWifi.collectAsStateWithLifecycle()

      // -----------------------------------------------------------------------
      // Notification permission
      // -----------------------------------------------------------------------

      NotificationPermissionRequest()

      // -----------------------------------------------------------------------
      // Triggered-screen window management
      // -----------------------------------------------------------------------

      /*
       * Track the previous screen so sound is stopped only when leaving an
       * alarm or timer screen.
       *
       * Sound replay is intentionally not started here. Compose effects may be
       * recreated. Playback self-healing is performed once for every incoming
       * Activity intent in handleIncomingIntent().
       */
      var previousScreen by remember {
        mutableStateOf<Screen?>(null)
      }

      LaunchedEffect(currentScreen) {
        val isTriggeredScreen =
          currentScreen is Screen.AlarmTriggered ||
            currentScreen is Screen.TimerFinished

        val wasTriggeredScreen =
          previousScreen is Screen.AlarmTriggered ||
            previousScreen is Screen.TimerFinished

        if (isTriggeredScreen) {
          showTriggeredWindow()
        } else {
          setKeepScreenOn(false)

          if (wasTriggeredScreen) {
            /*
             * The ViewModel dismiss and snooze functions already stop sound.
             * This is an additional safety cleanup using the same shared
             * application-level SoundPlayer.
             */
            appContainer.soundPlayer.stopSound()
            setShowOverLockScreen(false)

            /*
             * If the device is still locked, finish the Activity after the
             * alarm or timer is dismissed. This returns control to the
             * keyguard instead of exposing the normal app screen.
             */
            if (keyguardManager.isKeyguardLocked) {
              finish()
            }
          } else {
            setShowOverLockScreen(false)
          }
        }

        previousScreen = currentScreen
      }

      /*
       * Always remove KEEP_SCREEN_ON when this Compose UI leaves composition.
       *
       * We deliberately do not stop sound here because composition can be
       * destroyed during a configuration change while an alarm is active.
       */
      DisposableEffect(Unit) {
        onDispose {
          setKeepScreenOn(false)
        }
      }

      // -----------------------------------------------------------------------
      // Main UI
      // -----------------------------------------------------------------------

      MetroClockTheme(
        accentColor = selectedAccent.color,
        useDynamicColor = useDynamicColor,
        isLightTheme = isLightTheme
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = if (isLightTheme) {
            Color.White
          } else {
            MetroBlack
          }
        ) {
          AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
              metroTurnstileTransition<Screen>()
            },
            label = "MetroScreenTurnstile"
          ) { screen ->
            when (screen) {
              Screen.Main -> {
                MainClockScreen(
                  viewModel = viewModel,
                  onNavigateToSettings = {
                    viewModel.navigateTo(Screen.Settings)
                  },
                  onNavigateToAddAlarm = {
                    viewModel.navigateTo(
                      Screen.AddEditAlarm(null)
                    )
                  },
                  onNavigateToEditAlarm = { alarmId ->
                    viewModel.navigateTo(
                      Screen.AddEditAlarm(alarmId)
                    )
                  }
                )
              }

              is Screen.AddEditAlarm -> {
                val existingAlarm =
                  alarms.firstOrNull { alarm ->
                    alarm.id == screen.alarmId
                  }

                AddEditAlarmScreen(
                  existingAlarm = existingAlarm,
                  onSave = {
                      hour,
                      minute,
                      name,
                      repeats,
                      sound,
                      soundUri,
                      snooze,
                      skipCalendar,
                      smartSkip ->

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
                  onDelete = if (existingAlarm != null) {
                    {
                      viewModel.deleteAlarm(existingAlarm)
                    }
                  } else {
                    null
                  },
                  onCancel = {
                    viewModel.navigateBack()
                  },
                  onPreviewSound = { uri ->
                    viewModel.previewSoundUri(uri)
                  }
                )
              }

              Screen.Settings -> {
                SettingsScreen(
                  selectedAccent = selectedAccent,
                  useDynamicColor = useDynamicColor,
                  isLightTheme = isLightTheme,
                  onSelectAccent = { accentId ->
                    viewModel.selectAccentColor(accentId)
                  },
                  onToggleDynamicColor = { enabled ->
                    viewModel.setUseDynamicColor(enabled)
                  },
                  onToggleLightTheme = { enabled ->
                    viewModel.setLightTheme(enabled)
                  },
                  alarmVibrate = alarmVibrate,
                  onToggleAlarmVibrate = { enabled ->
                    viewModel.setAlarmVibrate(enabled)
                  },
                  alarmSilenceAfter = alarmSilenceAfter,
                  onSelectSilenceAfter = { value ->
                    viewModel.setAlarmSilenceAfter(value)
                  },
                  alarmSnoozeLength = alarmSnoozeLength,
                  onUpdateSnoozeLength = { minutes ->
                    viewModel.setAlarmSnoozeLength(minutes)
                  },
                  alarmGradualVolume = alarmGradualVolume,
                  onSelectGradualVolume = { seconds ->
                    viewModel.setAlarmGradualVolume(seconds)
                  },
                  alarmVolumeButtons = alarmVolumeButtons,
                  onSelectVolumeButtons = { action ->
                    viewModel.setAlarmVolumeButtons(action)
                  },
                  smartSkipEnabled = smartSkipEnabled,
                  onToggleSmartSkip = { enabled ->
                    viewModel.setSmartSkipEnabled(enabled)
                  },
                  smartSkipMode = smartSkipMode,
                  onSelectSmartSkipMode = { mode ->
                    viewModel.setSmartSkipMode(mode)
                  },
                  smartSkipHomeWifi = smartSkipHomeWifi,
                  onUpdateHomeWifi = { ssid ->
                    viewModel.setSmartSkipHomeWifi(ssid)
                  },
                  timerSoundTitle = timerSoundTitle,
                  onSelectTimerSound = { uri, title ->
                    viewModel.setTimerSound(
                      uri = uri,
                      title = title
                    )
                  },
                  timerGradualVolume = timerGradualVolume,
                  onToggleTimerGradualVolume = { enabled ->
                    viewModel.setTimerGradualVolume(enabled)
                  },
                  timerVibrate = timerVibrate,
                  onToggleTimerVibrate = { enabled ->
                    viewModel.setTimerVibrate(enabled)
                  },
                  onBack = {
                    viewModel.navigateBack()
                  }
                )
              }

              is Screen.AlarmTriggered -> {
                AlarmTriggeredScreen(
                  alarmId = screen.alarmId,
                  alarmName = screen.alarmName,
                  hour = screen.hour,
                  minute = screen.minute,
                  snoozeMinutes = screen.snoozeMinutes,
                  onSnooze = { minutes ->
                    viewModel.snoozeAlarm(
                      alarmId = screen.alarmId,
                      name = screen.alarmName,
                      minutes = minutes
                    )
                  },
                  onDismiss = {
                    viewModel.dismissAlarm()
                  }
                )
              }

              is Screen.TimerFinished -> {
                TimerFinishedScreen(
                  totalSeconds = screen.totalSeconds,
                  onRestart = {
                    viewModel.restartTimer()
                  },
                  onDismiss = {
                    viewModel.dismissTimerFinished()
                  }
                )
              }
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    /*
     * singleTop causes new notification/alarm intents to arrive here when this
     * Activity already exists.
     */
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  // ---------------------------------------------------------------------------
  // Intent handling
  // ---------------------------------------------------------------------------

  private fun handleIncomingIntent(incomingIntent: Intent?) {
    val destination =
      incomingIntent?.getStringExtra(EXTRA_DESTINATION) ?: return

    when (destination) {
      DEST_ALARM_TRIGGERED -> {
        val alarmId = incomingIntent.getLongExtra(
          AlarmReceiver.EXTRA_ALARM_ID,
          0L
        )

        /*
         * Ignore malformed alarm intents. A valid Room alarm ID should be
         * greater than zero.
         */
        if (alarmId <= 0L) {
          return
        }

        val alarmName =
          incomingIntent.getStringExtra(
            AlarmReceiver.EXTRA_ALARM_NAME
          )
            ?.trim()
            ?.takeIf { name -> name.isNotEmpty() }
            ?: "Alarm"

        val hour = incomingIntent.getIntExtra(
          AlarmReceiver.EXTRA_ALARM_HOUR,
          0
        ).coerceIn(0, 23)

        val minute = incomingIntent.getIntExtra(
          AlarmReceiver.EXTRA_ALARM_MINUTE,
          0
        ).coerceIn(0, 59)

        val snoozeMinutes = incomingIntent.getIntExtra(
          AlarmReceiver.EXTRA_SNOOZE_MINUTES,
          10
        ).coerceAtLeast(1)

        val screen = Screen.AlarmTriggered(
          alarmId = alarmId,
          alarmName = alarmName,
          hour = hour,
          minute = minute,
          snoozeMinutes = snoozeMinutes
        )

        showTriggeredWindow()

        /*
         * navigateTo updates the StateFlow synchronously. replayAlarmSound()
         * can therefore confirm that this exact triggered screen is active.
         */
        viewModel.navigateTo(screen)
        viewModel.replayAlarmSound(alarmId)
      }

      DEST_TIMER_FINISHED -> {
        val totalSeconds = incomingIntent.getIntExtra(
          EXTRA_TIMER_SECONDS,
          0
        ).coerceAtLeast(0)

        val screen = Screen.TimerFinished(
          totalSeconds = totalSeconds
        )

        showTriggeredWindow()
        viewModel.navigateTo(screen)
        viewModel.replayTimerSound()
      }

      else -> {
        /*
         * Ignore unknown destinations. Do not stop current audio because an
         * unrelated or stale intent must not dismiss an active alarm.
         */
      }
    }
  }

  private fun isAlarmOrTimerIntent(intent: Intent?): Boolean {
    return when (
      intent?.getStringExtra(EXTRA_DESTINATION)
    ) {
      DEST_ALARM_TRIGGERED,
      DEST_TIMER_FINISHED -> true

      else -> false
    }
  }

  // ---------------------------------------------------------------------------
  // Hardware buttons
  // ---------------------------------------------------------------------------

  override fun onKeyDown(
    keyCode: Int,
    event: KeyEvent?
  ): Boolean {
    val isVolumeButton =
      keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_VOLUME_UP

    if (!isVolumeButton) {
      return super.onKeyDown(keyCode, event)
    }

    val activeScreen = viewModel.currentScreen.value

    if (activeScreen !is Screen.AlarmTriggered) {
      return super.onKeyDown(keyCode, event)
    }

    /*
     * Normalize the stored setting so values such as "Do Nothing" and
     * "do nothing" behave identically.
     */
    return when (
      viewModel.alarmVolumeButtons.value
        .trim()
        .lowercase()
    ) {
      "snooze" -> {
        viewModel.snoozeAlarm(
          alarmId = activeScreen.alarmId,
          name = activeScreen.alarmName,
          minutes = activeScreen.snoozeMinutes
        )

        true
      }

      "stop",
      "dismiss" -> {
        viewModel.dismissAlarm()
        true
      }

      "do nothing",
      "nothing",
      "ignore" -> {
        /*
         * Consume the event without changing audio or alarm state.
         */
        true
      }

      else -> {
        super.onKeyDown(keyCode, event)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Window and keyguard handling
  // ---------------------------------------------------------------------------

  private fun showTriggeredWindow() {
    setShowOverLockScreen(true)
    setKeepScreenOn(true)
  }

  private fun clearTriggeredWindowFlags() {
    setKeepScreenOn(false)
    setShowOverLockScreen(false)
  }

  /**
   * Shows this Activity above the keyguard only while an alarm or timer is
   * active.
   */
  private fun setShowOverLockScreen(show: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(show)
      setTurnScreenOn(show)
    } else {
      @Suppress("DEPRECATION")
      if (show) {
        window.addFlags(
          WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
      } else {
        window.clearFlags(
          WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
      }
    }
  }

  private fun setKeepScreenOn(keepScreenOn: Boolean) {
    if (keepScreenOn) {
      window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      )
    } else {
      window.clearFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Compose permission request
  // ---------------------------------------------------------------------------

  @androidx.compose.runtime.Composable
  private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      return
    }

    val notificationPermissionLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
      ) {
        /*
         * A denied notification permission is handled gracefully. Alarm and
         * timer settings can provide another contextual explanation later.
         */
      }

    LaunchedEffect(Unit) {
      val permissionGranted =
        ContextCompat.checkSelfPermission(
          this@MainActivity,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

      if (!permissionGranted) {
        notificationPermissionLauncher.launch(
          Manifest.permission.POST_NOTIFICATIONS
        )
      }
    }
  }
}
