package app.metroclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.metroclock.AppContainer
import app.metroclock.alarm.AlarmScheduler
import app.metroclock.data.AlarmEntity
import app.metroclock.data.AlarmRepository
import app.metroclock.data.DefaultWorldCities
import app.metroclock.data.SettingsDataStore
import app.metroclock.data.WorldClockCity
import app.metroclock.service.StopwatchUiState
import app.metroclock.service.TimerUiState
import app.metroclock.ui.theme.AccentColor
import app.metroclock.ui.theme.DefaultAccent
import app.metroclock.ui.theme.MetroAccentsList
import app.metroclock.util.SoundPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {

  object Main : Screen()

  data class AddEditAlarm(
    val alarmId: Long? = null
  ) : Screen()

  object Settings : Screen()

  data class AlarmTriggered(
    val alarmId: Long,
    val alarmName: String,
    val hour: Int,
    val minute: Int,
    val snoozeMinutes: Int
  ) : Screen()

  data class TimerFinished(
    val totalSeconds: Int
  ) : Screen()
}

class ClockViewModel(
  private val repository: AlarmRepository,
  private val scheduler: AlarmScheduler,
  private val settingsDataStore: SettingsDataStore,
  private val soundPlayer: SoundPlayer,
  private val container: AppContainer
) : ViewModel() {

  // ---------------------------------------------------------------------------
  // Alarms
  // ---------------------------------------------------------------------------

  val alarms: StateFlow<List<AlarmEntity>> =
    repository.allAlarms.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // ---------------------------------------------------------------------------
  // Timer and stopwatch
  // ---------------------------------------------------------------------------

  val timerState: StateFlow<TimerUiState> =
    container.timerStateManager.timerState

  val stopwatchState: StateFlow<StopwatchUiState> =
    container.timerStateManager.stopwatchState

  // ---------------------------------------------------------------------------
  // Appearance settings
  // ---------------------------------------------------------------------------

  val selectedAccent: StateFlow<AccentColor> =
    settingsDataStore.selectedAccentId
      .map { accentId ->
        MetroAccentsList.find { accent ->
          accent.id == accentId
        } ?: DefaultAccent
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DefaultAccent
      )

  val useDynamicColor: StateFlow<Boolean> =
    settingsDataStore.useDynamicColor.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val isLightTheme: StateFlow<Boolean> =
    settingsDataStore.isLightTheme.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  // ---------------------------------------------------------------------------
  // World clock
  // ---------------------------------------------------------------------------

  private val _worldCities =
    MutableStateFlow(DefaultWorldCities)

  val worldCities: StateFlow<List<WorldClockCity>> =
    _worldCities.asStateFlow()

  // ---------------------------------------------------------------------------
  // Global alarm settings
  // ---------------------------------------------------------------------------

  val allAlarmsDisabled: StateFlow<Boolean> =
    settingsDataStore.allAlarmsDisabled.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  // ---------------------------------------------------------------------------
  // Alarm settings
  // ---------------------------------------------------------------------------

  val alarmVibrate: StateFlow<Boolean> =
    settingsDataStore.alarmVibrate.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  val alarmSilenceAfter: StateFlow<String> =
    settingsDataStore.alarmSilenceAfter.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "10"
    )

  val alarmSnoozeLength: StateFlow<Int> =
    settingsDataStore.alarmSnoozeLength.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 10
    )

  val alarmGradualVolume: StateFlow<String> =
    settingsDataStore.alarmGradualVolume.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "never"
    )

  val alarmVolumeButtons: StateFlow<String> =
    settingsDataStore.alarmVolumeButtons.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "snooze"
    )

  // ---------------------------------------------------------------------------
  // Timer settings
  // ---------------------------------------------------------------------------

  val timerSoundTitle: StateFlow<String> =
    settingsDataStore.timerSoundTitle.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "Default Sound"
    )

  /**
   * URI of the selected timer sound.
   *
   * An empty string means that the system default sound should be used.
   */
  val timerSoundUri: StateFlow<String> =
    settingsDataStore.timerSoundUri.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ""
    )

  val timerGradualVolume: StateFlow<Boolean> =
    settingsDataStore.timerGradualVolume.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val timerVibrate: StateFlow<Boolean> =
    settingsDataStore.timerVibrate.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  // ---------------------------------------------------------------------------
  // Smart Skip settings
  // ---------------------------------------------------------------------------

  val smartSkipEnabled: StateFlow<Boolean> =
    settingsDataStore.smartSkipEnabled.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val smartSkipMode: StateFlow<String> =
    settingsDataStore.smartSkipMode.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "wifi"
    )

  val smartSkipHomeWifi: StateFlow<String> =
    settingsDataStore.smartSkipHomeWifi.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ""
    )

  // ---------------------------------------------------------------------------
  // Navigation
  // ---------------------------------------------------------------------------

  private val _currentScreen =
    MutableStateFlow<Screen>(Screen.Main)

  val currentScreen: StateFlow<Screen> =
    _currentScreen.asStateFlow()

  // Main pivot tab: 0 = alarms, 1 = timer, 2 = stopwatch.
  private val _selectedTab =
    MutableStateFlow(0)

  val selectedTab: StateFlow<Int> =
    _selectedTab.asStateFlow()

  // ---------------------------------------------------------------------------
  // Dialog state
  // ---------------------------------------------------------------------------

  private val _showTimerLengthDialog =
    MutableStateFlow(false)

  val showTimerLengthDialog: StateFlow<Boolean> =
    _showTimerLengthDialog.asStateFlow()

  // ---------------------------------------------------------------------------
  // Sound replay jobs
  // ---------------------------------------------------------------------------

  /*
   * These jobs allow pending Room/DataStore reads to be cancelled when the
   * user dismisses, snoozes, restarts, or leaves a triggered screen.
   */
  private var alarmReplayJob: Job? = null
  private var timerReplayJob: Job? = null

  // ---------------------------------------------------------------------------
  // Navigation operations
  // ---------------------------------------------------------------------------

  fun navigateTo(screen: Screen) {
    when (screen) {
      is Screen.AlarmTriggered -> {
        /*
         * Entering an alarm screen invalidates a pending timer replay.
         */
        timerReplayJob?.cancel()
        timerReplayJob = null
      }

      is Screen.TimerFinished -> {
        /*
         * Entering a timer-finished screen invalidates a pending alarm replay.
         */
        alarmReplayJob?.cancel()
        alarmReplayJob = null
      }

      else -> {
        cancelSoundReplayJobs()
      }
    }

    _currentScreen.value = screen
  }

  fun navigateBack() {
    cancelSoundReplayJobs()
    soundPlayer.stopSound()
    _currentScreen.value = Screen.Main
  }

  fun setSelectedTab(tab: Int) {
    _selectedTab.value = tab.coerceIn(0, 2)
  }

  // ---------------------------------------------------------------------------
  // Alarm management
  // ---------------------------------------------------------------------------

  fun toggleAllAlarmsDisabled() {
    viewModelScope.launch {
      val currentlyDisabled =
        settingsDataStore.allAlarmsDisabled.first()

      val newDisabled = !currentlyDisabled

      settingsDataStore.setAllAlarmsDisabled(
        newDisabled
      )

      val enabledAlarms =
        repository.getEnabledAlarms()

      if (newDisabled) {
        enabledAlarms.forEach { alarm ->
          scheduler.cancel(alarm)
        }
      } else {
        enabledAlarms.forEach { alarm ->
          scheduler.schedule(alarm)
        }
      }
    }
  }

  fun toggleAlarm(
    alarm: AlarmEntity,
    enabled: Boolean
  ) {
    viewModelScope.launch {
      var globallyDisabled =
        settingsDataStore.allAlarmsDisabled.first()

      /*
       * Enabling one alarm also re-enables alarms globally.
       */
      if (enabled && globallyDisabled) {
        settingsDataStore.setAllAlarmsDisabled(false)
        globallyDisabled = false
      }

      val updatedAlarm =
        alarm.copy(isEnabled = enabled)

      repository.updateAlarm(updatedAlarm)

      if (enabled && !globallyDisabled) {
        scheduler.schedule(updatedAlarm)
      } else {
        scheduler.cancel(updatedAlarm)
      }
    }
  }

  fun saveAlarm(
    id: Long?,
    hour: Int,
    minute: Int,
    name: String,
    repeatDays: Int,
    soundName: String,
    soundUri: String = "",
    snoozeMinutes: Int = 10,
    skipIfCalendarEvent: Boolean = false,
    smartSkipLocation: Boolean = false
  ) {
    viewModelScope.launch {
      val normalizedId = id ?: 0L

      val alarm = AlarmEntity(
        id = normalizedId,
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        name = name.trim().ifBlank { "Alarm" },
        isEnabled = true,
        repeatDays = repeatDays,
        soundName = soundName.trim(),
        soundUri = soundUri.trim(),
        snoozeMinutes = snoozeMinutes.coerceAtLeast(1),
        skipIfCalendarEvent = skipIfCalendarEvent,
        smartSkipLocation = smartSkipLocation
      )

      val globallyDisabled =
        settingsDataStore.allAlarmsDisabled.first()

      if (normalizedId == 0L) {
        val newAlarmId =
          repository.insertAlarm(alarm)

        val insertedAlarm =
          alarm.copy(id = newAlarmId)

        if (!globallyDisabled) {
          scheduler.schedule(insertedAlarm)
        }
      } else {
        /*
         * Cancel the previous PendingIntent before updating. This prevents an
         * old time or repeat schedule from remaining active.
         */
        val previousAlarm =
          repository.allAlarms
            .first()
            .firstOrNull { existingAlarm ->
              existingAlarm.id == normalizedId
            }

        if (previousAlarm != null) {
          scheduler.cancel(previousAlarm)
        }

        repository.updateAlarm(alarm)

        if (!globallyDisabled) {
          scheduler.schedule(alarm)
        }
      }

      _currentScreen.value = Screen.Main
    }
  }

  fun deleteAlarm(alarm: AlarmEntity) {
    viewModelScope.launch {
      scheduler.cancel(alarm)
      repository.deleteAlarm(alarm)
      _currentScreen.value = Screen.Main
    }
  }

  fun snoozeAlarm(
    alarmId: Long,
    name: String,
    minutes: Int = 10
  ) {
    alarmReplayJob?.cancel()
    alarmReplayJob = null

    soundPlayer.stopSound()

    /*
     * Positional arguments avoid depending on AlarmScheduler parameter names.
     */
    scheduler.snooze(
      alarmId,
      name.ifBlank { "Alarm" },
      minutes.coerceAtLeast(1)
    )

    _currentScreen.value = Screen.Main
  }

  fun dismissAlarm() {
    alarmReplayJob?.cancel()
    alarmReplayJob = null

    soundPlayer.stopSound()
    _currentScreen.value = Screen.Main
  }

  // ---------------------------------------------------------------------------
  // Timer operations
  // ---------------------------------------------------------------------------

  fun setTimerDuration(seconds: Int) {
    /*
     * Changing timer duration also invalidates any previous finished alert.
     */
    timerReplayJob?.cancel()
    timerReplayJob = null

    soundPlayer.stopSound()

    container.timerStateManager.setTimerDuration(
      seconds.coerceAtLeast(0)
    )
  }

  fun startTimer() {
    timerReplayJob?.cancel()
    timerReplayJob = null

    soundPlayer.stopSound()
    container.startTimer()
  }

  fun pauseTimer() {
    container.pauseTimer()
  }

  fun resetTimer() {
    timerReplayJob?.cancel()
    timerReplayJob = null

    /*
     * AppContainer stops sound immediately and asks TimerService to clear the
     * timer state and finished notification.
     */
    container.resetTimer()

    _currentScreen.value = Screen.Main
  }

  fun showTimerLength(show: Boolean) {
    _showTimerLengthDialog.value = show
  }

  fun dismissTimerFinished() {
    timerReplayJob?.cancel()
    timerReplayJob = null

    /*
     * Update in-process UI state immediately. TimerService performs the same
     * operation safely and removes its finished notification.
     */
    soundPlayer.stopSound()
    container.timerStateManager.dismissTimerFinished()
    container.dismissTimerFinished()

    _currentScreen.value = Screen.Main
  }

  fun restartTimer() {
    timerReplayJob?.cancel()
    timerReplayJob = null

    /*
     * TimerService owns restart cleanup, notification removal, and countdown
     * startup. Do not independently call startTimer() here.
     */
    soundPlayer.stopSound()
    container.restartFinishedTimer()

    _currentScreen.value = Screen.Main
  }

  // ---------------------------------------------------------------------------
  // Stopwatch operations
  // ---------------------------------------------------------------------------

  fun startStopwatch() {
    container.startStopwatch()
  }

  fun pauseStopwatch() {
    container.pauseStopwatch()
  }

  fun resetStopwatch() {
    container.resetStopwatch()
  }

  fun recordStopwatchLap() {
    container.recordStopwatchLap()
  }

  // ---------------------------------------------------------------------------
  // Appearance settings operations
  // ---------------------------------------------------------------------------

  fun selectAccentColor(accentId: String) {
    viewModelScope.launch {
      settingsDataStore.setAccentColor(accentId)
    }
  }

  fun setUseDynamicColor(useDynamic: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setUseDynamicColor(
        useDynamic
      )
    }
  }

  fun setLightTheme(enabled: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setLightTheme(enabled)
    }
  }

  // ---------------------------------------------------------------------------
  // World clock operations
  // ---------------------------------------------------------------------------

  fun addWorldCity(city: WorldClockCity) {
    val alreadyAdded =
      _worldCities.value.any { existingCity ->
        existingCity.id == city.id
      }

    if (!alreadyAdded) {
      _worldCities.value =
        _worldCities.value + city
    }
  }

  fun removeWorldCity(city: WorldClockCity) {
    _worldCities.value =
      _worldCities.value.filter { existingCity ->
        existingCity.id != city.id
      }
  }

  // ---------------------------------------------------------------------------
  // Alarm settings operations
  // ---------------------------------------------------------------------------

  fun setAlarmVibrate(enabled: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setAlarmVibrate(enabled)
    }
  }

  fun setAlarmSilenceAfter(
    silenceAfter: String
  ) {
    viewModelScope.launch {
      settingsDataStore.setAlarmSilenceAfter(
        silenceAfter
      )
    }
  }

  fun setAlarmSnoozeLength(minutes: Int) {
    viewModelScope.launch {
      settingsDataStore.setAlarmSnoozeLength(
        minutes.coerceAtLeast(1)
      )
    }
  }

  fun setAlarmGradualVolume(seconds: String) {
    viewModelScope.launch {
      settingsDataStore.setAlarmGradualVolume(
        seconds
      )
    }
  }

  fun setAlarmVolumeButtons(action: String) {
    viewModelScope.launch {
      settingsDataStore.setAlarmVolumeButtons(
        action
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Timer settings operations
  // ---------------------------------------------------------------------------

  fun setTimerSound(
    uri: String,
    title: String
  ) {
    viewModelScope.launch {
      /*
       * Positional arguments avoid relying on the parameter names declared by
       * SettingsDataStore.setTimerSound().
       */
      settingsDataStore.setTimerSound(
        uri.trim(),
        title.trim().ifBlank { "Default Sound" }
      )
    }
  }

  fun setTimerGradualVolume(enabled: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setTimerGradualVolume(
        enabled
      )
    }
  }

  fun setTimerVibrate(enabled: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setTimerVibrate(enabled)
    }
  }

  // ---------------------------------------------------------------------------
  // Smart Skip operations
  // ---------------------------------------------------------------------------

  fun setSmartSkipEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setSmartSkipEnabled(
        enabled
      )
    }
  }

  fun setSmartSkipMode(mode: String) {
    viewModelScope.launch {
      settingsDataStore.setSmartSkipMode(mode)
    }
  }

  fun setSmartSkipHomeWifi(ssid: String) {
    viewModelScope.launch {
      settingsDataStore.setSmartSkipHomeWifi(
        ssid
      )
    }
  }

  fun setSmartSkipHomeLocation(
    lat: Double,
    lng: Double,
    radius: Int = 1000
  ) {
    viewModelScope.launch {
      settingsDataStore.setSmartSkipHomeLocation(
        lat,
        lng,
        radius.coerceAtLeast(1)
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Sound preview
  // ---------------------------------------------------------------------------

  fun previewSound(soundName: String) {
    cancelSoundReplayJobs()
    soundPlayer.previewSound(soundName)
  }

  /**
   * Plays the selected sound URI briefly.
   */
  fun previewSoundUri(uriString: String) {
    cancelSoundReplayJobs()
    soundPlayer.previewSoundUri(uriString)
  }

  fun stopSound() {
    cancelSoundReplayJobs()
    soundPlayer.stopSound()
  }

  // ---------------------------------------------------------------------------
  // Sound self-heal
  // ---------------------------------------------------------------------------

  /**
   * Restarts alarm playback if the application-wide SoundPlayer is not
   * currently playing.
   *
   * Room and DataStore are read directly because WhileSubscribed StateFlows
   * can temporarily contain their initial values when no screen is collecting
   * them.
   */
  fun replayAlarmSound(alarmId: Long) {
    if (alarmId <= 0L) {
      return
    }

    if (soundPlayer.isSoundPlaying()) {
      return
    }

    /*
     * Cancel an existing lookup before starting another one. This prevents two
     * suspended jobs from both reaching playAlarmSound().
     */
    alarmReplayJob?.cancel()

    alarmReplayJob = viewModelScope.launch {
      try {
        if (soundPlayer.isSoundPlaying()) {
          return@launch
        }

        val alarm =
          repository.allAlarms
            .first()
            .firstOrNull { candidate ->
              candidate.id == alarmId
            }

        val shouldVibrate =
          settingsDataStore.alarmVibrate.first()

        val silenceAfter =
          settingsDataStore.alarmSilenceAfter.first()

        val gradualVolume =
          settingsDataStore.alarmGradualVolume.first()

        /*
         * Room and DataStore reads can suspend. Confirm that the user is still
         * viewing this exact alarm screen before starting fallback playback.
         */
        val activeScreen = _currentScreen.value

        if (
          activeScreen !is Screen.AlarmTriggered ||
          activeScreen.alarmId != alarmId
        ) {
          return@launch
        }

        if (soundPlayer.isSoundPlaying()) {
          return@launch
        }

        soundPlayer.playAlarmSound(
          customUri = alarm?.soundUri.orEmpty(),
          vibrate = shouldVibrate,
          silenceAfterMinutes = silenceAfter,
          gradualVolumeSeconds = gradualVolume
        )
      } catch (e: CancellationException) {
        /*
         * Dismiss, snooze, and navigation intentionally cancel this job. Never
         * start fallback playback after cancellation.
         */
        throw e
      } catch (e: Exception) {
        e.printStackTrace()

        val activeScreen = _currentScreen.value

        if (
          activeScreen is Screen.AlarmTriggered &&
          activeScreen.alarmId == alarmId &&
          !soundPlayer.isSoundPlaying()
        ) {
          /*
           * Last-resort fallback. Use currently cached settings and the system
           * default alarm sound if Room or DataStore cannot be read.
           */
          soundPlayer.playAlarmSound(
            customUri = "",
            vibrate = alarmVibrate.value,
            silenceAfterMinutes =
              alarmSilenceAfter.value,
            gradualVolumeSeconds =
              alarmGradualVolume.value
          )
        }
      } finally {
        /*
         * Do not cancel a newer replay job that may have replaced this one.
         * The Job reference is primarily used for explicit cancellation.
         */
      }
    }
  }

  /**
   * Restarts timer-finished playback if the application-wide SoundPlayer is
   * not currently playing.
   */
  fun replayTimerSound() {
    if (soundPlayer.isSoundPlaying()) {
      return
    }

    timerReplayJob?.cancel()

    timerReplayJob = viewModelScope.launch {
      try {
        if (soundPlayer.isSoundPlaying()) {
          return@launch
        }

        val selectedSoundUri =
          settingsDataStore.timerSoundUri.first()

        val shouldVibrate =
          settingsDataStore.timerVibrate.first()

        val useGradualVolume =
          settingsDataStore.timerGradualVolume.first()

        /*
         * Do not start playback if the user left or dismissed the finished
         * screen while DataStore was being read.
         */
        if (_currentScreen.value !is Screen.TimerFinished) {
          return@launch
        }

        if (soundPlayer.isSoundPlaying()) {
          return@launch
        }

        soundPlayer.playTimerFinishedSound(
          customUri = selectedSoundUri,
          vibrate = shouldVibrate,
          gradualVolume = useGradualVolume
        )
      } catch (e: CancellationException) {
        /*
         * Never start fallback audio after an intentional cancellation.
         */
        throw e
      } catch (e: Exception) {
        e.printStackTrace()

        if (
          _currentScreen.value is Screen.TimerFinished &&
          !soundPlayer.isSoundPlaying()
        ) {
          soundPlayer.playTimerFinishedSound(
            customUri = timerSoundUri.value,
            vibrate = timerVibrate.value,
            gradualVolume =
              timerGradualVolume.value
          )
        }
      }
    }
  }

  private fun cancelSoundReplayJobs() {
    alarmReplayJob?.cancel()
    alarmReplayJob = null

    timerReplayJob?.cancel()
    timerReplayJob = null
  }

  // ---------------------------------------------------------------------------
  // ViewModel cleanup
  // ---------------------------------------------------------------------------

  override fun onCleared() {
    cancelSoundReplayJobs()

    /*
     * Do not stop active alarm/timer sound automatically here. A ViewModel can
     * be cleared during Activity lifecycle transitions while AlarmReceiver or
     * TimerService still legitimately owns active playback.
     */
    super.onCleared()
  }

  // ---------------------------------------------------------------------------
  // Factory
  // ---------------------------------------------------------------------------

  companion object {

    fun provideFactory(
      container: AppContainer
    ): ViewModelProvider.Factory {
      return object : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
          modelClass: Class<T>
        ): T {
          if (
            !modelClass.isAssignableFrom(
              ClockViewModel::class.java
            )
          ) {
            throw IllegalArgumentException(
              "Unknown ViewModel class: ${modelClass.name}"
            )
          }

          return ClockViewModel(
            repository = container.alarmRepository,
            scheduler = container.alarmScheduler,
            settingsDataStore =
              container.settingsDataStore,
            soundPlayer = container.soundPlayer,
            container = container
          ) as T
        }
      }
    }
  }
}
