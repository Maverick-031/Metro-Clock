package app.metroclock.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// -----------------------------------------------------------------------------
// Timer state
// -----------------------------------------------------------------------------

data class TimerUiState(
  val totalSeconds: Int = DEFAULT_TIMER_SECONDS,
  val remainingSeconds: Int = DEFAULT_TIMER_SECONDS,
  val isRunning: Boolean = false,
  val isPaused: Boolean = false,
  val isFinished: Boolean = false,
  val endTimestampMillis: Long = 0L
) {

  /**
   * Timer progress from 0 to 1.
   *
   * 0 means the timer has not started.
   * 1 means the timer has completed.
   */
  val progress: Float
    get() {
      if (totalSeconds <= 0) {
        return 0f
      }

      val safeRemaining =
        remainingSeconds.coerceIn(0, totalSeconds)

      return (
        (totalSeconds - safeRemaining).toFloat() /
          totalSeconds.toFloat()
        ).coerceIn(0f, 1f)
    }

  /**
   * Remaining timer duration formatted as MM:SS or H:MM:SS.
   */
  val formattedTime: String
    get() {
      val safeRemaining =
        remainingSeconds.coerceAtLeast(0)

      val hours = safeRemaining / 3600
      val minutes = (safeRemaining % 3600) / 60
      val seconds = safeRemaining % 60

      return if (hours > 0) {
        String.format(
          Locale.getDefault(),
          "%d:%02d:%02d",
          hours,
          minutes,
          seconds
        )
      } else {
        String.format(
          Locale.getDefault(),
          "%02d:%02d",
          minutes,
          seconds
        )
      }
    }

  /**
   * Expected completion time for a running timer.
   *
   * Returns an empty string when the timer is not running or no valid end
   * timestamp is available.
   */
  val formattedEndTime: String
    get() {
      if (
        !isRunning ||
        endTimestampMillis <= 0L
      ) {
        return ""
      }

      return SimpleDateFormat(
        "h:mm a",
        Locale.getDefault()
      ).format(
        Date(endTimestampMillis)
      )
    }

  companion object {
    const val DEFAULT_TIMER_SECONDS = 180
  }
}

// -----------------------------------------------------------------------------
// Stopwatch state
// -----------------------------------------------------------------------------

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

    /**
     * Formats stopwatch time as:
     *
     * MM:SS.CC for durations under one hour.
     * H:MM:SS.CC for durations of one hour or longer.
     */
    fun formatStopwatchMillis(millis: Long): String {
      val safeMillis = millis.coerceAtLeast(0L)

      val hours =
        safeMillis / 3_600_000L

      val minutes =
        (safeMillis % 3_600_000L) / 60_000L

      val seconds =
        (safeMillis % 60_000L) / 1000L

      val centiseconds =
        (safeMillis % 1000L) / 10L

      return if (hours > 0L) {
        String.format(
          Locale.getDefault(),
          "%d:%02d:%02d.%02d",
          hours,
          minutes,
          seconds,
          centiseconds
        )
      } else {
        String.format(
          Locale.getDefault(),
          "%02d:%02d.%02d",
          minutes,
          seconds,
          centiseconds
        )
      }
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
    get() = StopwatchLap.formatStopwatchMillis(
      elapsedMillis
    )

  val formattedCurrentLap: String
    get() = StopwatchLap.formatStopwatchMillis(
      currentLapElapsedMillis
    )
}

// -----------------------------------------------------------------------------
// State manager
// -----------------------------------------------------------------------------

class TimerStateManager {

  private val _timerState =
    MutableStateFlow(TimerUiState())

  val timerState: StateFlow<TimerUiState> =
    _timerState.asStateFlow()

  private val _stopwatchState =
    MutableStateFlow(StopwatchUiState())

  val stopwatchState: StateFlow<StopwatchUiState> =
    _stopwatchState.asStateFlow()

  // ---------------------------------------------------------------------------
  // Timer operations
  // ---------------------------------------------------------------------------

  /**
   * Sets a new timer duration.
   *
   * The duration cannot be changed while the timer is running. A zero-second
   * duration is allowed but cannot be started.
   */
  fun setTimerDuration(seconds: Int) {
    val safeSeconds =
      seconds.coerceAtLeast(0)

    _timerState.update { current ->
      if (current.isRunning) {
        current
      } else {
        TimerUiState(
          totalSeconds = safeSeconds,
          remainingSeconds = safeSeconds,
          isRunning = false,
          isPaused = false,
          isFinished = false,
          endTimestampMillis = 0L
        )
      }
    }
  }
}
