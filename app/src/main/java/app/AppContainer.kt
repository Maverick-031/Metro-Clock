package app.metroclock

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.metroclock.alarm.AlarmScheduler
import app.metroclock.data.AlarmRepository
import app.metroclock.data.AppDatabase
import app.metroclock.data.SettingsDataStore
import app.metroclock.service.TimerService
import app.metroclock.service.TimerStateManager
import app.metroclock.util.CalendarHelper
import app.metroclock.util.SmartSkipManager
import app.metroclock.util.SoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Application-level dependency container.
 *
 * This class must be created only once by the Application class. Creating a
 * separate AppContainer in an Activity, Service, or BroadcastReceiver would
 * also create separate SoundPlayer and TimerStateManager instances.
 */
class AppContainer(context: Context) {

  /**
   * Always retain the application context.
   *
   * This prevents the container and its long-lived dependencies from retaining
   * an Activity, Service, or BroadcastReceiver context.
   */
  private val appContext: Context = context.applicationContext

  private val applicationJob = SupervisorJob()

  private val applicationCoroutineContext: CoroutineContext =
    applicationJob + Dispatchers.Default

  /**
   * Long-lived scope used by application-level dependencies.
   *
   * SupervisorJob prevents a failure in one child coroutine from cancelling
   * unrelated application work.
   */
  val applicationScope: CoroutineScope =
    CoroutineScope(applicationCoroutineContext)

  /**
   * Application database.
   */
  val database: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    AppDatabase.getDatabase(
      context = appContext,
      scope = applicationScope
    )
  }

  /**
   * Alarm database repository.
   */
  val alarmRepository: AlarmRepository by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    AlarmRepository(database.alarmDao())
  }

  /**
   * Persistent application settings.
   */
  val settingsDataStore: SettingsDataStore by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    SettingsDataStore(appContext)
  }

  /**
   * Alarm scheduling dependency.
   */
  val alarmScheduler: AlarmScheduler by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    AlarmScheduler(appContext)
  }

  /**
   * Shared application-level sound player.
   *
   * Activities, ViewModels, services, and receivers should obtain this
   * instance through the application's AppContainer. They must not construct
   * another SoundPlayer directly if playback-state coordination is required.
   */
  val soundPlayer: SoundPlayer by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    SoundPlayer(appContext)
  }

  /**
   * Smart Skip dependency.
   */
  val smartSkipManager: SmartSkipManager by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    SmartSkipManager(
      context = appContext,
      settingsDataStore = settingsDataStore
    )
  }

  /**
   * Calendar helper singleton.
   */
  val calendarHelper: CalendarHelper
    get() = CalendarHelper

  /**
   * Shared in-process timer and stopwatch state.
   *
   * TimerService must receive this same TimerStateManager through the
   * application AppContainer. It must not create TimerStateManager() itself.
   */
  val timerStateManager: TimerStateManager by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    TimerStateManager()
  }

  // ---------------------------------------------------------------------------
  // Timer commands
  // ---------------------------------------------------------------------------

  fun startTimer() {
    sendTimerServiceCommand(TimerService.ACTION_START_TIMER)
  }

  fun pauseTimer() {
    sendTimerServiceCommand(TimerService.ACTION_PAUSE_TIMER)
  }

  fun resetTimer() {
    sendTimerServiceCommand(TimerService.ACTION_RESET_TIMER)
  }

  // ---------------------------------------------------------------------------
  // Stopwatch commands
  // ---------------------------------------------------------------------------

  fun startStopwatch() {
    sendTimerServiceCommand(
      TimerService.ACTION_START_STOPWATCH
    )
  }

  fun pauseStopwatch() {
    sendTimerServiceCommand(
      TimerService.ACTION_PAUSE_STOPWATCH
    )
  }

  fun resetStopwatch() {
    sendTimerServiceCommand(
      TimerService.ACTION_RESET_STOPWATCH
    )
  }

  fun recordStopwatchLap() {
    sendTimerServiceCommand(
      TimerService.ACTION_LAP_STOPWATCH
    )
  }

  // ---------------------------------------------------------------------------
  // Service command handling
  // ---------------------------------------------------------------------------

  /**
   * Sends a supported command to TimerService.
   *
   * Invalid or blank actions are ignored so an accidental caller cannot start
   * the foreground service without a command it knows how to process.
   */
  private fun sendTimerServiceCommand(action: String) {
    if (action !in SUPPORTED_TIMER_SERVICE_ACTIONS) {
      return
    }

    val serviceIntent =
      Intent(appContext, TimerService::class.java).apply {
        this.action = action
      }

    try {
      ContextCompat.startForegroundService(
        appContext,
        serviceIntent
      )
    } catch (e: SecurityException) {
      /*
       * This can happen when a required foreground-service permission or
       * declaration is missing.
       */
      e.printStackTrace()
    } catch (e: IllegalStateException) {
      /*
       * This can happen when Android does not allow the app to start a
       * foreground service from its current background state.
       */
      e.printStackTrace()
    } catch (e: Exception) {
      /*
       * Keep command failures from crashing the UI. TimerService should still
       * log and expose command failures where appropriate.
       */
      e.printStackTrace()
    }
  }

  companion object {

    private val SUPPORTED_TIMER_SERVICE_ACTIONS: Set<String> =
      setOf(
        TimerService.ACTION_START_TIMER,
        TimerService.ACTION_PAUSE_TIMER,
        TimerService.ACTION_RESET_TIMER,
        TimerService.ACTION_START_STOPWATCH,
        TimerService.ACTION_PAUSE_STOPWATCH,
        TimerService.ACTION_RESET_STOPWATCH,
        TimerService.ACTION_LAP_STOPWATCH,
        TimerService.ACTION_DISMISS_TIMER_FINISHED,
        TimerService.ACTION_RESTART_TIMER,
        TimerService.ACTION_STOP_SERVICE
      )
  }
}
