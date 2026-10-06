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
      .map { id ->
        MetroAccentsList.find { accent ->
          accent.id == id
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
  // Global alarm preference
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
   * Stored URI of the selected timer sound.
   *
   * An empty value means that the system default timer sound should be used.
   */
  val timerSoundUri: S*ateFlow<String> =
    settingsData*tore.timerSoundUri.stateIn(
      *cope = viewModelScope,
      start*d = SharingStarted.WhileSubscribed*5000),
      initialValue = ""
   *)

  val timerGradualVolume: State*low<Boolean> =
    settingsDataSto*e.timerGradualVolume.stateIn(
    * scope = viewModelScope,
      sta*ted = SharingStarted.WhileSubscrib*d(5000),
      initialValue = fals*
    )

  val timerVibrate: StateF*ow<Boolean> =
    settingsDataStor*.timerVibrate.stateIn(
      scope*= viewModelScope,
      started = *haringStarted.WhileSubscribed(5000*,
      initialValue = true
    )
*  // -----------------------------*----------------------------------*----------
  // Smart Skip setting*
  // ----------------------------*----------------------------------*-----------

  val smartSkipEnable*: StateFlow<Boolean> =
    setting*DataStore.smartSkipEnabled.stateIn*
      scope = viewModelScope,
   *  started = SharingStarted.WhileSu*scribed(5000),
      initialValue * false
    )

  val smartSkipMode:*StateFlow<String> =
    settingsDa*aStore.smartSkipMode.stateIn(
    * scope = viewModelScope,
      sta*ted = SharingStarted.WhileSubscrib*d(5000),
      initialValue = "wif*"
    )

  val smartSkipHomeWifi: *tateFlow<String> =
    settingsDat*Store.smartSkipHomeWifi.stateIn(
 *    scope = viewModelScope,
      *tarted = SharingStarted.WhileSubsc*ibed(5000),
      initialValue = "*
    )

  // ---------------------*----------------------------------*------------------
  // Navigation*  // -----------------------------*----------------------------------*----------

  private val _current*creen =
    MutableStateFlow<Scree*>(Screen.Main)

  val currentScree*: StateFlow<Screen> =
    _current*creen.asStateFlow()

  // Main piv*t tab: 0 = alarms, 1 = timer, 2 = *topwatch.
  private val _selectedT*b = MutableStateFlow(0)

  val sel*ctedTab: StateFlow<Int> =
    _sel*ctedTab.asStateFlow()

  // ------*--------------------------------------------------------------------
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
   * These jobs prevent an unfinished Room or DataStore lookup from starting
   * sound after the user has already dismissed or left the triggered screen.
   */
  private var alarmReplayJob: Job? = null
  private var timerReplayJob: Job? = null

  // ---------------------------------------------------------------------------
  // Navigation operations
  // ---------------------------------------------------------------------------

  fun navigateTo(screen: Screen) {
    /*
     * Cancel a pending replay when navigating away from its related screen.
     */
    when (screen) {
      is Scr*en.AlarmTriggered -> {
        tim*rReplayJob?.cancel()
        timer*eplayJob = null
      }

      is *creen.TimerFinished -> {
        a*armReplayJob?.cancel()
        ala*mReplayJob = null
      }

      e*se -> {
        cancelSoundReplayJ*bs()
      }
    }

    _currentSc*een.value = screen
  }

  fun navi*ateBack() {
    cancelSoundReplayJ*bs()
    soundPlayer.stopSound()
 *  _currentScreen.value = Screen.Ma*n
  }

  fun setSelectedTab(tab: I*t) {
    _selectedTab.value = tab.*oerceIn(0, 2)
  }

  // ----------*----------------------------------*-----------------------------
  //*Alarm management
  // ------------*----------------------------------*---------------------------

  fun*toggleAllAlarmsDisabled() {
    vi*wModelScope.launch {
      val cur*entlyDisabled =
        settingsDa*aStore.allAlarmsDisabled.first()

*     val newDisabled = !currentlyD*sabled

      settingsDataStore.se*AllAlarmsDisabled(newDisabled)

  *   val enabledAlarms = repository.*etEnabledAlarms()

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
    val updatedAlarm = alarm.copy(
      isEnabled = enabled
    )

    viewModelScope.launch {
      var globallyDisabled =
        settingsDataStore.allAlarmsDisabled.first()

      /*
       * Enabling an individual alarm also restores the global alarm switch.
       */
      if (globallyDisabled && en*bled) {
        settingsDataStore.*etAllAlarmsDisabled(false)
       *globallyDisabled = false
      }

*     repository.updateAlarm(update*Alarm)

      if (enabled && !glob*llyDisabled) {
        scheduler.s*hedule(updatedAlarm)
      } else *
        scheduler.cancel(updatedA*arm)
      }
    }
  }

  fun save*larm(
    id: Long?,
    hour: Int*
    minute: Int,
    name: String*
    repeatDays: Int,
    soundNam*: String,
    soundUri: String = "*,
    snoozeMinutes: Int = 10,
   *skipIfCalendarEvent: Boolean = fal*e,
    smartSkipLocation: Boolean * false
  ) {
    viewModelScope.la*nch {
      val safeHour = hour.co*rceIn(0, 23)
      val safeMinute * minute.coerceIn(0, 59)
      val *afeSnoozeMinutes = snoozeMinutes.c*erceAtLeast(1)

      val alarm = *larmEntity(
        id = id ?: 0L,*        hour = safeHour,
        m*nute = safeMinute,
        name = *ame.ifBlank { "Alarm" },
        i*Enabled = true,
        repeatDays*= repeatDays,
        soundName = *oundName,
        soundUri = soundUri,
        snoozeMinutes = safeSnoozeMinutes,
        skipIfCalendarEvent = skipIfCalendarEvent,
        smartSkipLocation = smartSkipLocation
      )

      val globallyDisabled =
        settingsDataStore.allAlarmsDisabled.first()

      if (id == null || id == 0L) {
        val newId = repository.insertAlarm(alarm)
        val insertedAlarm = alarm.copy(id = newId)

        if (!globallyDisabled) {
          scheduler.schedule(insertedAlarm)
        }
      } else {
        /*
         * Cancel the old schedule before replacing it. This is especially
         * important when the alarm time or repeat days were changed.
         */
        val existingAla*m = repository.allAlarms
         *.first()
          .firstOrNull { *xisting ->
            existing.id*== id
          }

        if (exi*tingAlarm != null) {
          sch*duler.cancel(existingAlarm)
      * }

        repository.updateAlarm*alarm)

        if (!globallyDisab*ed) {
          scheduler.schedule*alarm)
        }
      }

      _c*rrentScreen.value = Screen.Main
  * }
  }

  fun deleteAlarm(alarm: A*armEntity) {
    viewModelScope.la*nch {
      scheduler.cancel(alarm*
      repository.deleteAlarm(alar*)
      _currentScreen.value = Scr*en.Main
    }
  }

  fun snoozeAla*m(
    alarmId: Long,
    name: St*ing,
    minutes: Int = 10
  ) {
 *  alarmReplayJob?.cancel()
    ala*mReplayJob = null

    soundPlayer*stopSound()

    scheduler.snooze(*      alarmId = alarmId,
      nam* = name,
      minutes = minutes.c*erceAtLeast(1)
    )

    _current*creen.value = Screen.Main
* }

  fun dismissAlarm() {
    ala*mReplayJob?.cancel()
   *alarmReplayJob = null

   *sound*layer.stopSound()
   *_currentScreen.value = Screen.Main*  }

 *// -------------------------------*----------------------------------*--------
  // Timer operations
 *// -------------------------------*----------------------------------*--------

  fun setTimerDuration(s*conds: Int) {
   *container.timerStateManager.setTim*rDuration(
      seconds.coerceAtL*ast(0)
    )
 *}

* fun startTimer() {
*   container.startTimer()
* }

* fun pauseTimer() {
*   container.pauseTimer()
**}

* fun resetTimer() {
   *timerReplayJob?.cancel()
*   timerReplayJob = null

   *soundPlayer.stopSound()
*  *container.resetTimer()
 *}

 *fun showTimerLength(show: Boolean)*{
    _showTimerLengthDialog.value*= show
  }

**fun dismissTimerFinished() {
   *timerReplayJob?.cancel()
   *timer*eplayJob = null

   *soundPlayer.stopSound()
    contai*er.timerStateManager.dismissTimerF*nished()
    _currentScreen.value * Screen.Main
  }

**fun restartTimer() {
*   timerReplayJob?.cancel()
*   timerReplayJob = null

*  *soundPlayer.stopSound()
*  *container.timerStateManager.dismis*TimerFinished()
    container.star*Timer()
   *_*urrentScreen.value = Screen.Main
 *}

**// -------------------------------*----------------------------------*--------
  // Stopwatch operations* *// -------------------------------*----------------------------------*--------

  fun startStopwatch() {*   *container*startStopwatch()
**}

**fun*pauseStopwatch() {
*  *container.pauseStopwatch()
**}

**fun*resetStopwatch() {
*  *container*resetStopwatch()
**}

**fun*recordStopwatchLap() {
*  *container*recordStopwatchLap()
**}

**//*----------------------------------*----------------------------------*-----
**//*Appearance settings operations
**// -------------------------------*----------------------------------*--------

**fun selectAccentColor(accentId: St*ing) {
*  *view*odelScope.launch {
     *settings*ataStore.setAccentColor(accentId)
*  *}
**}

**fun*setUseDynamicColor(useDynamic: Boo*ean) {
   *view*odelScope.launch {
*    *settings*ataStore.setUseDynamicColor(useDyn*mic)
*  *}
**}

**fun setLightTheme(enabled: Boolean* {
   *viewModelScope.launch {
*    *settingsDataStore.setLightTheme(en*bled)
   *}
**}

**//*----------------------------------*----------------------------------*-----
  // World clock operations
* // ------------------------------*----------------------------------*---------

  fun addWorldCity(city* WorldClockCity) {
*   if (_worldCities.value.none { e*istingCity ->
        existingCity*id == city.id
     *}
*   ) {
*    *_worldCities.value = _worldCities.*alue + city
   *}
**}

**fun removeWorldCity(city: WorldClo*kCity) {
   *_worldCities.value = _worldCities.*alue.filter { existingCity ->
    * existingCity.id != city.id
   *}
**}

 *// -------------------------------*----------------------------------*--------
  // Alarm settings opera*ions
  // ------------------------*----------------------------------*---------------

  fun setAlarmVib*ate(enabled: Boolean) {
    viewMo*elScope.launch {
     *settingsDataStore.setAlarmVibrate(*nabled)
   *}
**}

**fun setAlarmSilenceAfter(silenceAf*er: String) {
    viewModelScope.l*unch {
     *settingsDataStore.setAlarmSilenceA*ter(silenceAfter)
   *}
* }

**fun setAlarmSnoozeLength(minutes: *nt) {
   *viewModelScope.launch {
     *settingsDataStore.setAlarmSnoozeLe*gth(
        minutes.coerceAtLeast*1)
     *)
*  *}
 *}

* fun*setAlarmGradualVolume(seconds: Str*ng) {
    viewModelScope.launch {
*    *settings*ataStore.setAlarmGradualVolume(sec*nds)
    }
**}

**fun setAlarmVolumeButtons(action: *tring) {
    viewModelScope.launch*{
     *settingsDataStore.setAlarmVolumeBu*tons(action)
    }
**}

* // ------------------------------*----------------------------------*---------
  // Timer settings oper*tions
  // -----------------------*----------------------------------*----------------

  fun setTimerSo*nd(
    uri: String,
   *title: String
 *) {
*   viewModelScope.launch {
*     settingsDataStore.setTimerSou*d(
        uri*= uri,
       *title = title
*    *)
   *}
 *}

 *fun setTimerGradualVolume(enabled:*Boolean) {
    viewModelScope.laun*h {
      settingsDataStore.setTim*rGradualVolume(enabled)
   *}
 *}

* fun setTimerVibrate(enabled: Bool*an) {
*   viewModelScope.launch {
*     settingsDataStore.setTimerVib*ate(enabled)
    }
* }

* // ------------------------------*----------------------------------*---------
  // Smart Skip operatio*s
  // ---------------------------*----------------------------------*------------

  fun setSmartSkipEn*bled(enabled: Boolean) {
    viewM*delScope.launch {
      settingsDa*aStore.setSmartSkipEnabled(enabled*
    }
  }

  fun setSmartSkipMode*mode: String) {
    viewModelScope*launch {
      settingsDataStore.s*tSmartSkipMode(mode)
    }
* }

**fun setSmartSkipHomeWifi(ssid: Str*ng) {
    viewModelScope.launch {
*     settingsDataStore.setSmartSki*HomeWifi(ssid)
    }
 *}

  fun setSmartSkipHomeLocation*
    lat: Double,
   *lng: Double,
   *radius: Int = 1000
  ) {
*   viewModelScope.launch {
*     settingsDataStore.setSmartSki*HomeLocation(
        lat = lat,
 *      lng = lng,
        radius = *adius.coerceAtLeast(1)
      )
   *}
  }

* // ------------------------------*----------------------------------*---------
  // Sound preview
  // *----------------------------------*----------------------------------*----

  fun previewSound(soundName* String) {
    cancelSoundReplayJo*s()
    soundPlayer.previewSound(s*undName)
  }

  /**
   * Plays the*exact selected alarm or timer soun* briefly.
   */
  fun previewSound*ri(uriString: String) {
    cancel*oundReplayJobs()
    soundPlayer.p*eviewSoundUri(uriString)
  }

  fu* stopSound() {
    cancelSoundRepl*yJobs()
    soundPlayer.stopSound(*
  }

  // -----------------------*----------------------------------*----------------
  // Sound self-h*al
  // --------------------------*----------------------------------*-------------

  /**
   * Restarts*alarm playback if the shared Sound*layer is not currently playing.
  **
   * Room and DataStore are read directly instead of relying on StateFlow.value.
   * A WhileSubscribed StateFlow can still contain its initial value if no UI
   * has started collecting it.
   */
  fun replayAlarmSound(alarmId: Long) {
    if (soundPlayer.isSoundPlaying()) {
      return
    }

    /*
     * Cancel an older pending request. This prevents two suspended replay
     * requests from both reaching playAlarmSound().
     */
    alarmReplayJob?.cancel()

  * alarmReplayJob = viewModelScope.l*unch {
      try {
        if (sou*dPlayer.isSoundPlaying()) {
      *   return@launch
        }

      * val alarm = repository.allAlarms
*         .first()
          .first*rNull { candidate ->
            c*ndidate.id == alarmId
          }
*        val shouldVibrate =
      *   settingsDataStore.alarmVibrate.*irst()

        val silenceAfter =*         *settingsDataStore.alarmSilenceAfte*.first()

        val gradualVolum* =
          settingsDataStore.ala*mGradualVolume.first()

        /*
         * Reading Room and DataStore can suspend. Verify that the user is still
         * viewing the same alarm-triggered screen before starting playback.
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
         * Cancellation is expected when the user dismisses, snoozes, or leaves
         * the alarm screen. It must not trigger fallback playback.
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
           * Last-resort fallback. This uses the currently cached preferences
           * and the system default sound if Room or DataStore cannot be read.
           */
          soundPlayer.playAlarmSound(
            customUri = "",
            vibrate = alarmVibrate.value,
            silenceAfterMinutes = alarmSilenceAfter.value,
            gradualVolumeSeconds = alarmGradualVolume.value
          )
        }
      }
    }
  }

  /**
   * Restarts timer-finished playback if the shared SoundPlayer is not playing.
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
         * Do not start delayed playback if the user already dismissed or left
         * the timer-finished screen while DataStore was being read.
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
         * Never start fallback playback after this job was intentionally
         * cancelled by dismiss, restart, reset, or navigation.
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
            gradualVolume = timerGradualVolume.value
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
          if (!modelClass.isAssignableFrom(ClockViewModel::class.java)) {
            throw IllegalArgumentException(
              "Unknown ViewModel class: ${modelClass.name}"
            )
          }

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
}
