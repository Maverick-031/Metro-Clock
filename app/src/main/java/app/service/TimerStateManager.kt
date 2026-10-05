package app.metroclock.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimerUiState(
  val totalSeconds: Int = 180, // Default 3 min
  val remainingSeconds: Int = 180,
  val isRunning: Boolean = false,
  val isPaused: Boolean = false,
  val isFinished: Boolean = false,
  val endTimestampMillis: Long = 0L
) {
  val progress: Float
    get() = if (totalSeconds > 0) {
      ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

  val formattedTime: String
    get() {
      val hours = remainingSeconds / 3600
      val minutes = (remainingSeconds % 3600) / 60
      val seconds = remainingSeconds % 60
      return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
      } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
      }
    }

  val formattedEndTime: String
    get() {
      if (endTimestampMillis == 0L) return ""
      val sdf = SimpleDateFormat("h:mm a", Locale.US)
      return sdf.format(Date(endTimestampMillis))
    }
}

data class StopwatchLap(
  val lapNumber: Int,
  val lapTimeMillis: Long,
  val totalTimeMillis: Long
) {
  val formattedLapTime: String
    get() = formatStopwatchMillis(lapTimeMillis)

  val formattedTotalTime: String
    get() = formatStopwatchMillis(totalTimeMillis)

  companion object {
    fun formatStopwatchMillis(millis: Long): String {
      val minutes = (millis / 60000)
      val seconds = (millis % 60000) / 1000
      val centiseconds = (millis % 1000) / 10
      return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
  }
}

data class StopwatchUiState(
  val elapsedMillis: Long = 0L,
  val isRunning: Boolean = false,
  val laps: List<StopwatchLap> = emptyList(),
  val currentLapElapsedMillis: Long = 0L
) {
  val formattedTime: String
    get() = StopwatchLap.formatStopwatchMillis(elapsedMillis)

  val formattedCurrentLap: String
    get() = StopwatchLap.formatStopwatchMillis(currentLapElapsedMillis)
}

class TimerStateManager {
  private val _timerState = MutableStateFlow(TimerUiState())
  val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

  private val _stopwatchState = MutableStateFlow(StopwatchUiState())
  val stopwatchState: StateFlow<StopwatchUiState> = _stopwatchState.asStateFlow()

  fun setTimerDuration(seconds: Int) {
    if (!_timerState.value.isRunning) {
      _timerState.value = TimerUiState(
        totalSeconds = seconds,
        remainingSeconds = seconds,
        isRunning = false,
        isPaused = false,
        isFinished = false
      )
    }
  }

  fun updateTimerTick(remaining: Int) {
    val current = _timerState.value
    if (remaining <= 0) {
      _timerState.value = current.copy(
        remainingSeconds = 0,
        isRunning = false,
        isPaused = false,
        isFinished = true
      )
    } else {
      _timerState.value = current.copy(
        remainingSeconds = remaining,
        isRunning = true,
        isPaused = false,
        isFinished = false
      )
    }
  }

  fun setTimerRunning(running: Boolean, endTimestamp: Long = 0L) {
    val current = _timerState.value
    _timerState.value = current.copy(
      isRunning = running,
      isPaused = !running && current.remainingSeconds < current.totalSeconds && current.remainingSeconds > 0,
      endTimestampMillis = endTimestamp
    )
  }

  fun resetTimer() {
    val current = _timerState.value
    _timerState.value = TimerUiState(
      totalSeconds = current.totalSeconds,
      remainingSeconds = current.totalSeconds,
      isRunning = false,
      isPaused = false,
      isFinished = false
    )
  }

  fun dismissTimerFinished() {
    val current = _timerState.value
    _timerState.value = current.copy(
      isFinished = false,
      remainingSeconds = current.totalSeconds,
      isRunning = false,
      isPaused = false
    )
  }

  fun updateStopwatchTick(elapsed: Long, currentLapElapsed: Long) {
    val current = _stopwatchState.value
    _stopwatchState.value = current.copy(
      elapsedMillis = elapsed,
      currentLapElapsedMillis = currentLapElapsed,
      isRunning = true
    )
  }

  fun setStopwatchRunning(running: Boolean) {
    _stopwatchState.value = _stopwatchState.value.copy(isRunning = running)
  }

  fun recordLap() {
    val current = _stopwatchState.value
    if (current.isRunning && current.elapsedMillis > 0) {
      val lapNumber = current.laps.size + 1
      val newLap = StopwatchLap(
        lapNumber = lapNumber,
        lapTimeMillis = current.currentLapElapsedMillis,
        totalTimeMillis = current.elapsedMillis
      )
      _stopwatchState.value = current.copy(
        laps = listOf(newLap) + current.laps,
        currentLapElapsedMillis = 0L
      )
    }
  }

  fun resetStopwatch() {
    _stopwatchState.value = StopwatchUiState()
  }
}
