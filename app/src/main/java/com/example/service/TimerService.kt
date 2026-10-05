package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.ClockApplication
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var timerJob: Job? = null
  private var stopwatchJob: Job? = null
  private var wakeLock: android.os.PowerManager.WakeLock? = null

  companion object {
    const val CHANNEL_ID = "metro_timer_channel"
    const val NOTIFICATION_ID = 4001
    const val NOTIFICATION_ID_FINISHED = 4002

    const val ACTION_START_TIMER = "com.example.START_TIMER"
    const val ACTION_PAUSE_TIMER = "com.example.PAUSE_TIMER"
    const val ACTION_RESET_TIMER = "com.example.RESET_TIMER"

    const val ACTION_START_STOPWATCH = "com.example.START_STOPWATCH"
    const val ACTION_PAUSE_STOPWATCH = "com.example.PAUSE_STOPWATCH"
    const val ACTION_RESET_STOPWATCH = "com.example.RESET_STOPWATCH"
    const val ACTION_LAP_STOPWATCH = "com.example.LAP_STOPWATCH"

    const val ACTION_STOP_SERVICE = "com.example.STOP_SERVICE"
  }

  private val app: ClockApplication
    get() = applicationContext as ClockApplication

  private val stateManager: TimerStateManager
    get() = app.container.timerStateManager

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MetroClock:TimerWakeLock")
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START_TIMER -> startTimer()
      ACTION_PAUSE_TIMER -> pauseTimer()
      ACTION_RESET_TIMER -> resetTimer()

      ACTION_START_STOPWATCH -> startStopwatch()
      ACTION_PAUSE_STOPWATCH -> pauseStopwatch()
      ACTION_RESET_STOPWATCH -> resetStopwatch()
      ACTION_LAP_STOPWATCH -> stateManager.recordLap()

      ACTION_STOP_SERVICE -> stopForegroundAndService()
    }
    return START_NOT_STICKY
  }

  private fun startTimer() {
    val current = stateManager.timerState.value
    if (current.remainingSeconds <= 0) return

    val endTime = System.currentTimeMillis() + (current.remainingSeconds * 1000L)
    stateManager.setTimerRunning(true, endTime)

    try {
      if (wakeLock?.isHeld == false) {
        wakeLock?.acquire(current.remainingSeconds * 1000L + 60000L)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    startForegroundNotification("Timer", "Running: ${current.formattedTime}")

    timerJob?.cancel()
    timerJob = serviceScope.launch {
      var remaining = current.remainingSeconds
      while (isActive && remaining > 0) {
        delay(1000L)
        remaining -= 1
        stateManager.updateTimerTick(remaining)
        updateNotification("Timer", "Running: ${stateManager.timerState.value.formattedTime}")
      }

      if (remaining <= 0) {
        onTimerCompleted()
      }
    }
  }

  private fun pauseTimer() {
    timerJob?.cancel()
    timerJob = null
    stateManager.setTimerRunning(false)
    updateNotification("Timer", "Paused: ${stateManager.timerState.value.formattedTime}")
    checkIfCanStopService()
  }

  private fun resetTimer() {
    timerJob?.cancel()
    timerJob = null
    stateManager.resetTimer()
    checkIfCanStopService()
  }

  private fun onTimerCompleted() {
    timerJob?.cancel()
    timerJob = null

    serviceScope.launch {
      val dataStore = app.container.settingsDataStore
      val customUri = dataStore.timerSoundUri.firstOrNull() ?: ""
      val vibrate = dataStore.timerVibrate.firstOrNull() ?: true
      val gradual = dataStore.timerGradualVolume.firstOrNull() ?: false

      app.container.soundPlayer.playTimerFinishedSound(
        customUri = customUri,
        vibrate = vibrate,
        gradualVolume = gradual
      )
    }

    // Trigger full screen notification for Time's Up
    val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DEST_TIMER_FINISHED)
      putExtra(MainActivity.EXTRA_TIMER_SECONDS, stateManager.timerState.value.totalSeconds)
    }
    val pendingIntent = PendingIntent.getActivity(
      this,
      999,
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentTitle("Timer Time’s up!")
      .setContentText("Time’s up")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setFullScreenIntent(pendingIntent, true)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID_FINISHED, notification)

    try {
      startActivity(fullScreenIntent)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    checkIfCanStopService()
  }

  private fun startStopwatch() {
    stateManager.setStopwatchRunning(true)
    try {
      if (wakeLock?.isHeld == false) {
        wakeLock?.acquire(2 * 3600 * 1000L)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    startForegroundNotification("Stopwatch", "Running: ${stateManager.stopwatchState.value.formattedTime}")

    stopwatchJob?.cancel()
    stopwatchJob = serviceScope.launch {
      val startTime = System.currentTimeMillis() - stateManager.stopwatchState.value.elapsedMillis
      var lastLapResetTime = System.currentTimeMillis() - stateManager.stopwatchState.value.currentLapElapsedMillis
      var lastNotifSec = -1L

      while (isActive && stateManager.stopwatchState.value.isRunning) {
        val now = System.currentTimeMillis()
        val elapsed = now - startTime
        val currentLapElapsed = now - lastLapResetTime

        stateManager.updateStopwatchTick(elapsed, currentLapElapsed)

        val sec = elapsed / 1000
        if (sec != lastNotifSec) {
          lastNotifSec = sec
          updateNotification("Stopwatch", "Elapsed: ${stateManager.stopwatchState.value.formattedTime}")
        }
        delay(25L)
      }
    }
  }

  private fun pauseStopwatch() {
    stopwatchJob?.cancel()
    stopwatchJob = null
    stateManager.setStopwatchRunning(false)
    updateNotification("Stopwatch", "Paused: ${stateManager.stopwatchState.value.formattedTime}")
    checkIfCanStopService()
  }

  private fun resetStopwatch() {
    stopwatchJob?.cancel()
    stopwatchJob = null
    stateManager.resetStopwatch()
    checkIfCanStopService()
  }

  private fun checkIfCanStopService() {
    val isTimerRunning = stateManager.timerState.value.isRunning
    val isStopwatchRunning = stateManager.stopwatchState.value.isRunning
    if (!isTimerRunning && !isStopwatchRunning) {
      stopForegroundAndService()
    }
  }

  private fun startForegroundNotification(title: String, text: String) {
    val notification = buildNotification(title, text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun updateNotification(title: String, text: String) {
    val notification = buildNotification(title, text)
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, notification)
  }

  private fun buildNotification(title: String, text: String): android.app.Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      this,
      0,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title)
      .setContentText(text)
      .setContentIntent(openPendingIntent)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun stopForegroundAndService() {
    timerJob?.cancel()
    stopwatchJob?.cancel()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Metro Timer & Stopwatch",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Ongoing timer and stopwatch notifications"
      }
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      nm.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    try {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    wakeLock = null
  }
}
