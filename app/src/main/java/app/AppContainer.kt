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
import app.metroclock.util.SoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(private val context: Context) {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val database: AppDatabase by lazy {
    AppDatabase.getDatabase(context, applicationScope)
  }

  val alarmRepository: AlarmRepository by lazy {
    AlarmRepository(database.alarmDao())
  }

  val settingsDataStore: SettingsDataStore by lazy {
    SettingsDataStore(context)
  }

  val alarmScheduler: AlarmScheduler by lazy {
    AlarmScheduler(context)
  }

  val soundPlayer: SoundPlayer by lazy {
    SoundPlayer(context)
  }

  val smartSkipManager: app.metroclock.util.SmartSkipManager by lazy {
    app.metroclock.util.SmartSkipManager(context, settingsDataStore)
  }

  val timerStateManager: TimerStateManager by lazy {
    TimerStateManager()
  }

  fun sendTimerServiceCommand(action: String) {
    val intent = Intent(context, TimerService::class.java).apply {
      this.action = action
    }
    try {
      ContextCompat.startForegroundService(context, intent)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun startTimer() = sendTimerServiceCommand(TimerService.ACTION_START_TIMER)
  fun pauseTimer() = sendTimerServiceCommand(TimerService.ACTION_PAUSE_TIMER)
  fun resetTimer() = sendTimerServiceCommand(TimerService.ACTION_RESET_TIMER)

  fun startStopwatch() = sendTimerServiceCommand(TimerService.ACTION_START_STOPWATCH)
  fun pauseStopwatch() = sendTimerServiceCommand(TimerService.ACTION_PAUSE_STOPWATCH)
  fun resetStopwatch() = sendTimerServiceCommand(TimerService.ACTION_RESET_STOPWATCH)
  fun recordStopwatchLap() = sendTimerServiceCommand(TimerService.ACTION_LAP_STOPWATCH)
}
