package app.metroclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.metroclock.AppContainer
import app.metroclock.alarm.AlarmScheduler
import app.metroclock.data.AlarmEntity
import app.metroclock.data.AlarmRepository
import app.metroclock.data.SettingsDataStore
import app.metroclock.service.StopwatchUiState
import app.metroclock.service.TimerUiState
import app.metroclock.ui.theme.AccentColor
import app.metroclock.ui.theme.DefaultAccent
import app.metroclock.ui.theme.MetroAccentsList
import app.metroclock.util.SoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
  object Main : Screen()
  data class AddEditAlarm(val alarmId: Long? = null) : Screen()
  object Settings : Screen()
  data class AlarmTriggered(
    val alarmId: Long,
    val alarmName: String,
    val hour: Int,
    val minute: Int,
    val snoozeMinutes: Int
  ) : Screen()
  data class TimerFinished(val totalSeconds: Int) : Screen()
}

class ClockViewModel(
  private val repository: AlarmRepository,
  private val scheduler: AlarmScheduler,
  private val settingsDataStore: SettingsDataStore,
  private val soundPlayer: SoundPlayer,
  private val container: AppContainer
) : ViewModel() {

  // Alarms from Room Database
  val alarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  // Timer & Stopwatch from TimerStateManager
  val timerState: StateFlow<TimerUiState> = container.timerStateManager.timerState
  val stopwatchState: StateFlow<StopwatchUiState> = container.timerStateManager.stopwatchState

  // Settings: Accent Color & Dynamic Colors
  val selectedAccent: StateFlow<AccentColor> = settingsDataStore.selectedAccentId
    .map { id -> MetroAccentsList.find { it.id == id } ?: DefaultAccent }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = DefaultAccent
    )

  val useDynamicColor: StateFlow<Boolean> = settingsDataStore.useDynamicColor
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val isLightTheme: StateFlow<Boolean> = settingsDataStore.isLightTheme
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  // World Clock Cities
  private val _worldCities = MutableStateFlow(app.metroclock.data.DefaultWorldCities)
  val worldCities: StateFlow<List<app.metroclock.data.WorldClockCity>> = _worldCities.asStateFlow()

  // Turn All Alarms Off Preference
  val allAlarmsDisabled: StateFlow<Boolean> = settingsDataStore.allAlarmsDisabled
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  // Alarm Settings Flows
  val alarmVibrate: StateFlow<Boolean> = settingsDataStore.alarmVibrate
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val alarmSilenceAfter: StateFlow<String> = settingsDataStore.alarmSilenceAfter
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10")

  val alarmSnoozeLength: StateFlow<Int> = settingsDataStore.alarmSnoozeLength
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

  val alarmGradualVolume: StateFlow<String> = settingsDataStore.alarmGradualVolume
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "never")

  val alarmVolumeButtons: StateFlow<String> = settingsDataStore.alarmVolumeButtons
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "snooze")

  // Timer Settings Flows
  val timerSoundTitle: StateFlow<String> = settingsDataStore.timerSoundTitle
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default Sound")

  // Stored URI of the selected timer sound ("" = app default)
  val timerSoundUri: StateFlow<String> = settingsDataStore.timerSoundUri
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  val timerGradualVolume: StateFlow<Boolean> = settingsDataStore.timerGradualVolume
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val timerVibrate: StateFlow<Boolean> = settingsDataStore.timerVibrate
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  // Smart Skip Flows
  val smartSkipEnabled: StateFlow<Boolean> = settingsDataStore.smartSkipEnabled
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val smartSkipMode: StateFlow<String> = settingsDataStore.smartSkipMode
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "wifi")

  val smartSkipHomeWifi: StateFlow<String> = settingsDataStore.smartSkipHomeWifi
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  // Screen Navigation
  private val _currentScreen = MutableStateFlow<Screen>(Screen.Main)
  val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

  // Main Pivot Tab: 0 = alarms, 1 = timer, 2 = stopwatch
  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  // Dialog states
  private val _showTimerLengthDialog = MutableStateFlow(false)
  val showTimerLengthDialog: StateFlow<Boolean> = _showTimerLengthDialog.asStateFlow()

  fun navigateTo(screen: Screen) {
    _currentScreen.value = screen
  }

  fun navigateBack() {
    soundPlayer.stopSound()
    _currentScreen.value = Screen.Main
  }

  fun setSelectedTab(tab: Int) {
    _selectedTab.value = tab
  }

  // --- Alarms Management ---
  fun toggleAllAlarmsDisabled() {
    viewModelScope.launch {
      val currentlyDisabled = allAlarmsDisabled.value
      val newDisabled = !currentlyDisabled
      settingsDataStore.setAllAlarmsDisabled(newDisabled)

      val allList = repository.getEnabledAlarms()
      if (newDisabled) {
        allList.forEach { scheduler.cancel(it) }
      } else {
        allList.forEach { scheduler.schedule(it) }
      }
    }
  }

  fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
    val updated = alarm.copy(isEnabled = enabled)
    viewModelScope.launch {
      if (allAlarmsDisabled.value && enabled) {
        // If all alarms were disabled, re-enable global switch
        settingsDataStore.setAllAlarmsDisabled(false)
      }
      repository.updateAlarm(updated)
      if (enabled && !allAlarmsDisabled.value) {
        scheduler.schedule(updated)
      } else {
        scheduler.cancel(updated)
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
      val alarm = AlarmEntity(
        id = id ?: 0,
        hour = hour,
        minute = minute,
        name = if (name.isBlank()) "Alarm" else name,
        isEnabled = true,
        repeatDays = repeatDays,
        soundName = soundName,
        soundUri = soundUri,
        snoozeMinutes = snoozeMinutes,
        skipIfCalendarEvent = skipIfCalendarEvent,
        smartSkipLocation = smartSkipLocation
      )
      if (id == null || id == 0L) {
        val newId = repository.insertAlarm(alarm)
        if (!allAlarmsDisabled.value) {
          scheduler.schedule(alarm.copy(id = newId))
        }
      } else {
        repository.updateAlarm(alarm)
        if (!allAlarmsDisabled.value) {
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

  fun snoozeAlarm(alarmId: Long, name: String, minutes: Int = 10) {
    soundPlayer.stopSound()
    scheduler.snooze(alarmId, name, minutes)
    _currentScreen.value = Screen.Main
  }

  fun dismissAlarm() {
    soundPlayer.stopSound()
    _currentScreen.value = Screen.Main
  }

  // --- Timer Operations ---
  fun setTimerDuration(seconds: Int) {
    container.timerStateManager.setTimerDuration(seconds)
  }

  fun startTimer() {
    container.startTimer()
  }

  fun pauseTimer() {
    container.pauseTimer()
  }

  fun resetTimer() {
    container.resetTimer()
  }

  fun showTimerLength(show: Boolean) {
    _showTimerLengthDialog.value = show
  }

  fun dismissTimerFinished() {
    soundPlayer.stopSound()
    container.timerStateManager.dismissTimerFinished()
    _currentScreen.value = Screen.Main
  }

  fun restartTimer() {
    soundPlayer.stopSound()
    container.timerStateManager.dismissTimerFinished()
    container.startTimer()
    _currentScreen.value = Screen.Main
  }

  // --- Stopwatch Operations ---
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

  // --- Settings Operations ---
  fun selectAccentColor(accentId: String) {
    viewModelScope.launch {
      settingsDataStore.setAccentColor(accentId)
    }
  }

  fun setUseDynamicColor(useDynamic: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setUseDynamicColor(useDynamic)
    }
  }

  fun setLightTheme(enabled: Boolean) {
    viewModelScope.launch {
      settingsDataStore.setLightTheme(enabled)
    }
  }

  fun addWorldCity(city: app.metroclock.data.WorldClockCity) {
    if (_worldCities.value.none { it.id == city.id }) {
      _worldCities.value = _worldCities.value + city
    }
  }

  fun removeWorldCity(city: app.metroclock.data.WorldClockCity) {
    _worldCities.value = _worldCities.value.filter { it.id != city.id }
  }

  fun setAlarmVibrate(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setAlarmVibrate(enabled) }
  }

  fun setAlarmSilenceAfter(silenceAfter: String) {
    viewModelScope.launch { settingsDataStore.setAlarmSilenceAfter(silenceAfter) }
  }

  fun setAlarmSnoozeLength(minutes: Int) {
    viewModelScope.launch { settingsDataStore.setAlarmSnoozeLength(minutes) }
  }

  fun setAlarmGradualVolume(seconds: String) {
    viewModelScope.launch { settingsDataStore.setAlarmGradualVolume(seconds) }
  }

  fun setAlarmVolumeButtons(action: String) {
    viewModelScope.launch { settingsDataStore.setAlarmVolumeButtons(action) }
  }

  fun setTimerSound(uri: String, title: String) {
    viewModelScope.launch { settingsDataStore.setTimerSound(uri, title) }
  }

  fun setTimerGradualVolume(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setTimerGradualVolume(enabled) }
  }

  fun setTimerVibrate(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setTimerVibrate(enabled) }
  }

  fun setSmartSkipEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setSmartSkipEnabled(enabled) }
  }

  fun setSmartSkipMode(mode: String) {
    viewModelScope.launch { settingsDataStore.setSmartSkipMode(mode) }
  }

  fun setSmartSkipHomeWifi(ssid: String) {
    viewModelScope.launch { settingsDataStore.setSmartSkipHomeWifi(ssid) }
  }

  fun setSmartSkipHomeLocation(lat: Double, lng: Double, radius: Int = 1000) {
    viewModelScope.launch { settingsDataStore.setSmartSkipHomeLocation(lat, lng, radius) }
  }

  fun previewSound(soundName: String) {
    soundPlayer.previewSound(soundName)
  }

  /** Plays the exact selected sound (alarm or timer) briefly — used by the play button. */
  fun previewSoundUri(uriString: String) {
    soundPlayer.previewSoundUri(uriString)
  }

  fun stopSound() {
    soundPlayer.stopSound()
  }

  // --- Sound self-heal -------------------------------------------------------
  // Called by MainActivity when the AlarmTriggered screen appears. If the
  // receiver was killed while the phone was locked, the sound never started --
  // this restarts it. Skips silently if the sound is already playing so there
  // is never a double-play or restart glitch.
  fun replayAlarmSound(alarmId: Long) {
    if (soundPlayer.isSoundPlaying()) return
    val alarm = alarms.value.firstOrNull { it.id == alarmId }
    soundPlayer.playAlarmSound(
      customUri = alarm?.soundUri ?: "",
      vibrate = alarmVibrate.value,
      silenceAfterMinutes = alarmSilenceAfter.value,
      gradualVolumeSeconds = alarmGradualVolume.value
    )
  }

  // Same self-heal for the timer-finished screen.
  fun replayTimerSound() {
    if (soundPlayer.isSoundPlaying()) return
    soundPlayer.playTimerFinishedSound(
      customUri = timerSoundUri.value,
      vibrate = timerVibrate.value,
      gradualVolume = timerGradualVolume.value
    )
  }

  // Factory
  companion object {
    fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return ClockViewModel(
            repository = container.alarmRepository,
            scheduler = container.alarmScheduler,
            settingsDataStore = container.settingsDataStore,
            soundPlayer = container.soundPlayer,
            container = container
          ) as T
        }
      }
  }
}
