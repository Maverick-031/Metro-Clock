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

/**
 * Application-wide dependency container.
 *
 * ClockApplication must create exactly one AppContainer for the application
 * process. Activities, services, receivers, and ViewModels must retrieve their
 * dependencies from that shared container.
 *
 * Do not create additional AppContainer, SoundPlayer, or TimerStateManager
 * instances in individual Android components.
 */
class AppContainer(context: Context) {

  /**
   * Retain only the application context.
   *
   * This prevents application-wide dependencies from retaining an Activity,
   * Service, or BroadcastReceiver context.
   */
  private val appContext: Context =
    context.applicationContext

  // ---------------------------------------------------------------------------
  // Application coroutine scope
  // ---------------------------------------------------------------------------

  private val applicationJob = SupervisorJob()

  /**
   * Long-lived application scope.
   *
   * SupervisorJob ensures that a failure in one child coroutine does not
   * automatically cancel unrelated application-level operations.
   */
  val applicationScope: CoroutineScope =
    CoroutineScope(
      applicationJob + Dispatchers.Default
    )

  // ---------------------------------------------------------------------------
  // Database and repository
  // ---------------------------------------------------------------------------

  /**
   * Shared Room database instance.
   */
  val database: AppDatabase by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    /*
     * Positional parameters avoid compilation errors if the function's
     * parameter names differ while its parameter types remain the same.
     */
    AppDatabase.getDatabase(
      appContext,
      applicationScope
    )
  }

  /**
   * Shared alarm repository.
   */
  val alarmRepository: AlarmRepository by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    AlarmRepository(
      database.alarmDao()
    )
  }

  // ---------------------------------------------------------------------------
  // Settings
  // ---------------------------------------------------------------------------

  /**
   * Shared DataStore settings provider.
   */
  val settingsDataStore: SettingsDataStore by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    SettingsDataStore(appContext)
  }

  // ---------------------------------------------------------------------------
  // Alarm dependencies
  // ---------------------------------------------------------------------------

  /**
   * Shared alarm scheduler.
   */
  val alarmScheduler: AlarmScheduler by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    AlarmScheduler(appContext)
  }

  /**
   * Shared application-level sound player.
   *
   * AlarmReceiver, TimerService, MainActivity, and ClockViewModel must use this
   * exact instance. Creating another SoundPlayer would prevent
   * isSoundPlaying() and stopSound() from coordinating playback correctly.
   */
  val soundPlayer: SoundPlayer by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    SoundPlayer(appContext)
  }

  // ---------------------------------------------------------------------------
  // Smart Skip dependencies
  // ---------------------------------------------------------------------------

  /**
   * Shared Smart Skip manager.
   */
  val smartSkipManager: SmartSkipManager by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    /*
     * Positional parameters avoid dependency on the constructor's parameter
     * names.
     */
    SmartSkipManager(
      appContext,
      settingsDataStore
    )
  }

  /**
   * Calendar helper singleton.
   */
  val calendarHelper: CalendarHelper
    get() = CalendarHelper

  // ---------------------------------------------------------------------------
  // Timer and stopwatch state
  // ---------------------------------------------------------------------------

  /**
   * Shared in-process timer and stopwatch state.
   *
   * TimerService and ClockViewModel must use this same instance. TimerService
   * must not construct its own TimerStateManager.
   */
  val timerStateManager: TimerStateManager by lazy(
    LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    TimerStateManager()
  }

  // ---------------------------------------------------------------------------
  // Timer commands
  // ---------------------------------------------------------------------------

  /**
   * Starts or resumes the configured timer.
   */
  fun startTimer() {
    sendTimerServiceCommand(
      TimerService.ACTION_START_TIMER
    )
  }

  /**
   * Pauses the currently running timer.
   */
  fun pauseTimer() {
    sendTimerServiceCommand(
      TimerService.ACTION_PAUSE_TIMER
    )
  }

  /**
   * Resets the timer and stops any timer-finished audio.
   */
  fun resetTimer() {
    /*
     * Stop immediately in the current process. TimerService will also perform
     * cleanup when it receives ACTION_RESET_TIMER.
     */
    soundPlayer.stopSound()

    sendTimerServiceCommand(
      TimerService.ACTION_RESET_TIMER
    )
  }

  /**
   * Dismisses timer-finished playback and its notification.
   */
  fun dismissTimerFinished() {
    /*
     * Stop playback immediately rather than waiting for TimerService command
     * processing. The service performs the remaining state and notification
     * cleanup.
     */
    soundPlayer.stopSound()

    sendTimerServiceCommand(
      TimerService.ACTION_DISMISS_TIMER_FINISHED
    )
  }

  /**
   * Stops the finished alert and restarts the timer using its original
   * configured duration.
   */
  fun restartFinishedTimer() {
    soundPlayer.stopSound()

    sendTimerServiceCommand(
      TimerService.ACTION_RESTART_TIMER
    )
  }

  // ---------------------------------------------------------------------------
  // Stopwatch commands
  // ---------------------------------------------------------------------------

  /**
   * Starts or resumes the stopwatch.
   */
  fun startStopwatch() {
    sendTimerServiceCommand(
      TimerService.ACTION_START_STOPWATCH
    )
  }

  /**
   * Pauses the stopwatch.
   */
  fun pauseStopwatch() {
    sendTimerServiceCommand(
      TimerService.ACTION_PAUSE_STOPWATCH
    )
  }

  /**
   * Resets the stopwatch.
   */
  fun resetStopwatch() {
    sendTimerServiceCommand(
      TimerService.ACTION_RESET_STOPWATCH
    )
  }

  /**
   * Records a stopwatch lap.
   */
  fun recordStopwatchLap() {
    sendTimerServiceCommand(
      TimerService.ACTION_LAP_STOPWATCH
    )
  }

  // ---------------------------------------------------------------------------
  // General service control
  // ---------------------------------------------------------------------------

  /**
   * Stops TimerService and removes its active foreground notification.
   *
   * This also requests that TimerService stop timer-finished audio and remove
   * its finished notification.
   */
  fun stopTimerService() {
    soundPlayer.stopSound()

    sendTimerServiceCommand(
      TimerService.ACTION_STOP_SERVICE
    )
  }

  // ---------------------------------------------------------------------------
  // TimerService command dispatch
  // ---------------------------------------------------------------------------

  /**
   * Sends a supported command to TimerService.
   *
   * Returns true if Android accepted the service-start request. This does not
   * guarantee that TimerService completed the requested operation.
   */
  private fun sendTimerServiceCommand(
    action: String
  ): Boolean {
    if (action !in SUPPORTED_TIMER_SERVICE_ACTIONS) {
      return false
    }

    val serviceIntent =
      Intent(appContext, TimerService::class.java).apply {
        this.action = action
      }

    return try {
      /*
       * TimerService promotes itself promptly by calling startForeground().
       * ContextCompat handles the correct foreground-service starting API for
       * the running Android version.
       */
      ContextCompat.startForegroundService(
        appContext,
        serviceIntent
      )

      true
    } catch (e: SecurityException) {
      /*
       * Possible causes include a missing foreground-service permission,
       * incorrect service type declaration, or operating-system restrictions.
       */
      e.printStackTrace()
      false
    } catch (e: IllegalStateException) {
      /*
       * Android may reject a foreground-service start when the application is
       * in a restricted background state.
       */
      e.printStackTrace()
      false
    } catch (e: Exception) {
      /*
       * Service command failures should not crash the Activity or Compose UI.
       */
      e.printStackTrace()
      false
    }
  }

  companion object {

    /**
     * Commands that AppContainer is allowed to send to TimerService.
     *
     * Keeping an allowlist prevents accidental startup with an unsupported or
     * malformed command.
     */
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
