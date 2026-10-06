package app.metroclock.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.metroclock.ClockApplication
import app.metroclock.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

class TimerService : Service() {

  companion object {

    /*
     * Versioned notification-channel IDs.
     *
     * Channel sound and vibration settings usually cannot be reliably changed
     * after Android creates a channel. New IDs prevent old channel properties
     * from causing duplicate sound or vibration.
     */
    const val CHANNEL_ID =
      "metro_timer_channel_v2"

    const val CHANNEL_ID_FINISHED =
      "metro_timer_finished_channel_v2"

    const val NOTIFICATION_ID = 4001
    const val NOTIFICATION_ID_FINISHED = 4002

    const val ACTION_START_TIMER =
      "app.metroclock.START_TIMER"

    const val ACTION_PAUSE_TIMER =
      "app.metroclock.PAUSE_TIMER"

    const val ACTION_RESET_TIMER =
      "app.metroclock.RESET_TIMER"

    const val ACTION_START_STOPWATCH =
      "app.metroclock.START_STOPWATCH"

    const val ACTION_PAUSE_STOPWATCH =
      "app.metroclock.PAUSE_STOPWATCH"

    const val ACTION_RESET_STOPWATCH =
      "app.metroclock.RESET_STOPWATCH"

    const val ACTION_LAP_STOPWATCH =
      "app.metroclock.LAP_STOPWATCH"

    const val ACTION_DISMISS_TIMER_FINISHED =
      "app.metroclock.DISMISS_TIMER_FINISHED"

    const val ACTION_RESTART_TIMER =
      "app.metroclock.RESTART_TIMER"

    const val ACTION_STOP_SERVICE =
      "app.metroclock.STOP_SERVICE"

    private const val REQUEST_CODE_OPEN_APP = 40_001
    private const val REQUEST_CODE_TIMER_FINISHED = 40_002
    private const val REQUEST_CODE_DISMISS_FINISHED = 40_003
    private const val REQUEST_CODE_RESTART_TIMER = 40_004
    private const val REQUEST_CODE_PAUSE_TIMER = 40_005
    private const val REQUEST_CODE_RESET_TIMER = 40_006
    private const val REQUEST_CODE_PAUSE_STOPWATCH = 40_007
    private const val REQUEST_CODE_RESET_STOPWATCH = 40_008

    private const val TIMER_NOTIFICATION_UPDATE_MS = 1000L
    private const val STOPWATCH_UPDATE_MS = 25L

    /*
     * A timed wake lock is used only as a defensive safeguard. The service
     * remains foreground while a timer or stopwatch is active.
     */
    private const val WAKE_LOCK_GRACE_PERIOD_MS = 60_000L
    private const val STOPWATCH_WAKE_LOCK_TIMEOUT_MS =
      2L * 60L * 60L * 1000L
  }

  private val app: ClockApplication
    get() = applicationContext as ClockApplication

  private val stateManager: TimerStateManager
    get() = app.container.timerStateManager

  private val soundPlayer
    get() = app.container.soundPlayer

  private val notificationManager: NotificationManager by lazy {
    getSystemService(
      Context.NOTIFICATION_SERVICE
    ) as NotificationManager
  }

  private val serviceJob = SupervisorJob()

  private val serviceScope =
    CoroutineScope(serviceJob + Dispatchers.Default)

  private var timerJob: Job? = null
  private var stopwatchJob: Job? = null
  private var timerCompletionJob: Job? = null

  private var wakeLock: PowerManager.WakeLock? = null

  private var isForegroundStarted = false

  override fun onBind(intent: Intent?): IBinder? = null

  // ---------------------------------------------------------------------------
  // Service lifecycle
  // ---------------------------------------------------------------------------

  override fun onCreate() {
    super.onCreate()

    createNotificationChannels()

    val powerManager =
      getSystemService(Context.POWER_SERVICE) as? PowerManager

    wakeLock = powerManager?.newWakeLock(
      PowerManager.PARTIAL_WAKE_LOCK,
      "$packageName:TimerWakeLock"
    )?.apply {
      setReferenceCounted(false)
    }
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    val action = intent?.action

    /*
     * AppContainer starts this service with startForegroundService(). Android
     * therefore expects this service to call startForeground promptly.
     *
     * Stop and dismiss actions can immediately finish without promotion.
     */
    if (
      action != ACTION_STOP_SERVICE &&
      action != ACTION_DISMISS_TIMER_FINISHED
    ) {
      ensureForegroundStarted()
    }

    when (action) {
      ACTION_START_TIMER -> {
        startTimer()
      }

      ACTION_PAUSE_TIMER -> {
        pauseTimer()
      }

      ACTION_RESET_TIMER -> {
        resetTimer()
      }

      ACTION_START_STOPWATCH -> {
        startStopwatch()
      }

      ACTION_PAUSE_STOPWATCH -> {
        pauseStopwatch()
      }

      ACTION_RESET_STOPWATCH -> {
        resetStopwatch()
      }

      ACTION_LAP_STOPWATCH -> {
        stateManager.recordStopwatchLap()
        updateActiveNotification()
      }

      ACTION_DISMISS_TIMER_FINISHED -> {
        dismissTimerFinished()
      }

      ACTION_RESTART_TIMER -> {
        restartTimer()
      }

      ACTION_STOP_SERVICE -> {
        stopForegroundAndService(
          stopFinishedSound = true,
          removeFinishedNotification = true
        )
      }

      null -> {
        /*
         * START_NOT_STICKY means Android should not normally recreate this
         * service with a null Intent. Stop safely if it happens.
         */
        stopForegroundAndService(
          stopFinishedSound = false,
          removeFinishedNotification = false
        )
      }

      else -> {
        /*
         * Ignore unsupported internal commands and stop if no active timer or
         * stopwatch requires this service.
         */
        checkIfCanStopService()
      }
    }

    return START_NOT_STICKY
  }

  // ---------------------------------------------------------------------------
  // Timer
  // ---------------------------------------------------------------------------

  private fun startTimer() {
    val currentState = stateManager.timerState.value
    val remainingSeconds = currentState.remainingSeconds

    if (remainingSeconds <= 0) {
      checkIfCanStopService()
      return
    }

    timerCompletionJob?.cancel()
    timerCompletionJob = null

    cancelFinishedNotification()
    soundPlayer.stopSound()

    val endTimeMillis =
      System.currentTimeMillis() +
        remainingSeconds * 1000L

    stateManager.setTimerRunning(
      true,
      endTimeMillis
    )

    acquireWakeLock(
      timeoutMillis =
        remainingSeconds * 1000L +
          WAKE_LOCK_GRACE_PERIOD_MS
    )

    updateForegroundNotification(
      title = "Timer",
      text = "Running: ${stateManager.timerState.value.formattedTime}"
    )

    timerJob?.cancel()

    timerJob = serviceScope.launch {
      try {
        runTimerLoop(endTimeMillis)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        e.printStackTrace()

        stateManager.setTimerRunning(false)
        releaseWakeLockIfIdle()
        checkIfCanStopService()
      }
    }
  }

  /**
   * Calculates remaining time from the target end time instead of subtracting
   * one every loop. This prevents cumulative drift when a delay resumes late.
   */
  private suspend fun runTimerLoop(endTimeMillis: Long) {
    var lastReportedSeconds = Long.MIN_VALUE

    while (serviceScope.isActive) {
      val remainingMillis =
        endTimeMillis - System.currentTimeMillis()

      val remainingSeconds =
        if (remainingMillis <= 0L) {
          0
        } else {
          ceil(remainingMillis / 1000.0)
            .toInt()
            .coerceAtLeast(1)
        }

      if (remainingSeconds.toLong() != lastReportedSeconds) {
        lastReportedSeconds = remainingSeconds.toLong()

        stateManager.updateTimerTick(remainingSeconds)

        updateForegroundNotification(
          title = "Timer",
          text =
            "Running: ${stateManager.timerState.value.formattedTime}"
        )
      }

      if (remainingSeconds <= 0) {
        onTimerCompleted()
        return
      }

      /*
       * Align the next update close to the following second boundary without
       * producing a tight loop.
       */
      val delayMillis =
        (remainingMillis % TIMER_NOTIFICATION_UPDATE_MS)
          .takeIf { it > 0L }
          ?.coerceAtLeast(50L)
          ?: TIMER_NOTIFICATION_UPDATE_MS

      delay(delayMillis)
    }
  }

  private fun pauseTimer() {
    timerJob?.cancel()
    timerJob = null

    stateManager.setTimerRunning(false)

    updateForegroundNotification(
      title = "Timer",
      text =
        "Paused: ${stateManager.timerState.value.formattedTime}"
    )

    releaseWakeLockIfIdle()
    checkIfCanStopService()
  }

  private fun resetTimer() {
    timerJob?.cancel()
    timerJob = null

    timerCompletionJob?.cancel()
    timerCompletionJob = null

    soundPlayer.stopSound()
    cancelFinishedNotification()

    stateManager.resetTimer()

    releaseWakeLockIfIdle()
    checkIfCanStopService()
  }

  private fun onTimerCompleted() {
    timerJob = null

    stateManager.updateTimerTick(0)
    stateManager.setTimerRunning(false)

    releaseWakeLockIfIdle()

    /*
     * Keep completion work in one job. The old implementation launched sound
     * asynchronously and then immediately stopped the service, which could
     * cancel that coroutine before DataStore finished loading.
     */
    timerCompletionJob?.cancel()

    timerCompletionJob = serviceScope.launch {
      try {
        val settingsDataStore =
          app.container.settingsDataStore

        val customSoundUri =
          settingsDataStore.timerSoundUri.first()

        val shouldVibrate =
          settingsDataStore.timerVibrate.first()

        val useGradualVolume =
          settingsDataStore.timerGradualVolume.first()

        /*
         * The SoundPlayer belongs to AppContainer, not this Service, so its
         * playback remains available to MainActivity and ClockViewModel.
         */
        soundPlayer.playTimerFinishedSound(
          customUri = customSoundUri,
          vibrate = shouldVibrate,
          gradualVolume = useGradualVolume
        )

        showTimerFinishedNotification()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        e.printStackTrace()

        /*
         * If DataStore cannot be read, still alert the user with the system
         * default sound and vibration.
         */
        if (!soundPlayer.isSoundPlaying()) {
          soundPlayer.playTimerFinishedSound(
            customUri = "",
            vibrate = true,
            gradualVolume = false
          )
        }

        showTimerFinishedNotification()
      } finally {
        timerCompletionJob = null

        /*
         * The finished notification is separate from the foreground
         * notification, so the running service can now stop safely.
         */
        stopForegroundAndService(
          stopFinishedSound = false,
          removeFinishedNotification = false
        )
      }
    }
  }

  private fun dismissTimerFinished() {
    timerCompletionJob?.cancel()
    timerCompletionJob = null

    soundPlayer.stopSound()
    stateManager.dismissTimerFinished()

    cancelFinishedNotification()

    stopForegroundAndService(
      stopFinishedSound = false,
      removeFinishedNotification = false
    )
  }

  private fun restartTimer() {
    timerCompletionJob?.cancel()
    timerCompletionJob = null

    soundPlayer.stopSound()
    cancelFinishedNotification()

    stateManager.dismissTimerFinished()

    val currentState = stateManager.timerState.value

    if (currentState.remainingSeconds <= 0) {
      /*
       * TimerStateManager.dismissTimerFinished() is expected to restore the
       * original duration. If it does not, the ViewModel should set the desired
       * duration before sending ACTION_RESTART_TIMER.
       */
      checkIfCanStopService()
      return
    }

    startTimer()
  }

  // ---------------------------------------------------------------------------
  // Stopwatch
  // ---------------------------------------------------------------------------

  private fun startStopwatch() {
    stateManager.setStopwatchRunning(true)

    acquireWakeLock(
      timeoutMillis = STOPWATCH_WAKE_LOCK_TIMEOUT_MS
    )

    updateForegroundNotification(
      title = "Stopwatch",
      text =
        "Running: ${stateManager.stopwatchState.value.formattedTime}"
    )

    stopwatchJob?.cancel()

    stopwatchJob = serviceScope.launch {
      try {
        val initialState =
          stateManager.stopwatchState.value

        val startTimeMillis =
          System.currentTimeMillis() -
            initialState.elapsedMillis

        val lapStartTimeMillis =
          System.currentTimeMillis() -
            initialState.currentLapElapsedMillis

        var lastNotificationSecond = -1L

        while (
          isActive &&
          stateManager.stopwatchState.value.isRunning
        ) {
          val now = System.currentTimeMillis()

          val elapsedMillis =
            (now - startTimeMillis).coerceAtLeast(0L)

          val currentLapElapsedMillis =
            (now - lapStartTimeMillis).coerceAtLeast(0L)

          stateManager.updateStopwatchTick(
            elapsedMillis,
            currentLapElapsedMillis
          )

          val elapsedSecond =
            elapsedMillis / 1000L

          if (elapsedSecond != lastNotificationSecond) {
            lastNotificationSecond = elapsedSecond

            updateForegroundNotification(
              title = "Stopwatch",
              text =
                "Elapsed: ${stateManager.stopwatchState.value.formattedTime}"
            )
          }

          delay(STOPWATCH_UPDATE_MS)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        e.printStackTrace()
        stateManager.setStopwatchRunning(false)
        releaseWakeLockIfIdle()
        checkIfCanStopService()
      }
    }
  }

  private fun pauseStopwatch() {
    stopwatchJob?.cancel()
    stopwatchJob = null

    stateManager.setStopwatchRunning(false)

    updateForegroundNotification(
      title = "Stopwatch",
      text =
        "Paused: ${stateManager.stopwatchState.value.formattedTime}"
    )

    releaseWakeLockIfIdle()
    checkIfCanStopService()
  }

  private fun resetStopwatch() {
    stopwatchJob?.cancel()
    stopwatchJob = null

    stateManager.resetStopwatch()

    releaseWakeLockIfIdle()
    checkIfCanStopService()
  }

  // ---------------------------------------------------------------------------
  // Foreground service
  // ---------------------------------------------------------------------------

  private fun ensureForegroundStarted() {
    if (isForegroundStarted) {
      return
    }

    val timerState = stateManager.timerState.value
    val stopwatchState = stateManager.stopwatchState.value

    val notification = when {
      timerState.isRunning -> {
        buildActiveNotification(
          title = "Timer",
          text = "Running: ${timerState.formattedTime}"
        )
      }

      stopwatchState.isRunning -> {
        buildActiveNotification(
          title = "Stopwatch",
          text = "Running: ${stopwatchState.formattedTime}"
        )
      }

      else -> {
        buildActiveNotification(
          title = "MetroClock",
          text = "Updating timer"
        )
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceCompat.startForeground(
          this,
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
      } else {
        startForeground(
          NOTIFICATION_ID,
          notification
        )
      }

      isForegroundStarted = true
    } catch (e: SecurityException) {
      e.printStackTrace()
      stopSelf()
    } catch (e: IllegalArgumentException) {
      /*
       * This can occur if the foreground-service type passed here does not
       * match the service type declared in AndroidManifest.xml.
       */
      e.printStackTrace()
      stopSelf()
    } catch (e: Exception) {
      e.printStackTrace()
      stopSelf()
    }
  }

  private fun updateForegroundNotification(
    title: String,
    text: String
  ) {
    ensureForegroundStarted()

    if (!isForegroundStarted) {
      return
    }

    try {
      notificationManager.notify(
        NOTIFICATION_ID,
        buildActiveNotification(
          title = title,
          text = text
        )
      )
    } catch (e: SecurityException) {
      e.printStackTrace()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun updateActiveNotification() {
    val timerState = stateManager.timerState.value
    val stopwatchState = stateManager.stopwatchState.value

    when {
      timerState.isRunning -> {
        updateForegroundNotification(
          title = "Timer",
          text = "Running: ${timerState.formattedTime}"
        )
      }

      stopwatchState.isRunning -> {
        updateForegroundNotification(
          title = "Stopwatch",
          text = "Running: ${stopwatchState.formattedTime}"
        )
      }
    }
  }

  private fun buildActiveNotification(
    title: String,
    text: String
  ): Notification {
    val openIntent =
      Intent(this, MainActivity::class.java).apply {
        flags =
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

    val openPendingIntent =
      PendingIntent.getActivity(
        this,
        REQUEST_CODE_OPEN_APP,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    val builder =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(
          android.R.drawable.ic_dialog_info
        )
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(openPendingIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(
          NotificationCompat.CATEGORY_STOPWATCH
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)

    val timerState = stateManager.timerState.value
    val stopwatchState = stateManager.stopwatchState.value

    if (timerState.isRunning) {
      builder.addAction(
        android.R.drawable.ic_media_pause,
        "Pause",
        createServicePendingIntent(
          requestCode = REQUEST_CODE_PAUSE_TIMER,
          action = ACTION_PAUSE_TIMER
        )
      )

      builder.addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        "Reset",
        createServicePendingIntent(
          requestCode = REQUEST_CODE_RESET_TIMER,
          action = ACTION_RESET_TIMER
        )
      )
    } else if (stopwatchState.isRunning) {
      builder.addAction(
        android.R.drawable.ic_media_pause,
        "Pause",
        createServicePendingIntent(
          requestCode = REQUEST_CODE_PAUSE_STOPWATCH,
          action = ACTION_PAUSE_STOPWATCH
        )
      )

      builder.addAction(
        android.R.drawable.ic_menu_close_clear_cancel,
        "Reset",
        createServicePendingIntent(
          requestCode = REQUEST_CODE_RESET_STOPWATCH,
          action = ACTION_RESET_STOPWATCH
        )
      )
    }

    return builder.build()
  }

  private fun checkIfCanStopService() {
    val timerRunning =
      stateManager.timerState.value.isRunning

    val stopwatchRunning =
      stateManager.stopwatchState.value.isRunning

    val completionRunning =
      timerCompletionJob?.isActive == true

    if (
      !timerRunning &&
      !stopwatchRunning &&
      !completionRunning
    ) {
      releaseWakeLock()

      stopForegroundAndService(
        stopFinishedSound = false,
        removeFinishedNotification = false
      )
    }
  }

  private fun stopForegroundAndService(
    stopFinishedSound: Boolean,
    removeFinishedNotification: Boolean
  ) {
    timerJob?.cancel()
    timerJob = null

    stopwatchJob?.cancel()
    stopwatchJob = null

    if (stopFinishedSound) {
      timerCompletionJob?.cancel()
      timerCompletionJob = null
      soundPlayer.stopSound()
    }

    if (removeFinishedNotification) {
      cancelFinishedNotification()
    }

    releaseWakeLock()

    try {
      ServiceCompat.stopForeground(
        this,
        ServiceCompat.STOP_FOREGROUND_REMOVE
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }

    isForegroundStarted = false
    stopSelf()
  }

  // ---------------------------------------------------------------------------
  // Finished notification
  // ---------------------------------------------------------------------------

  private fun showTimerFinishedNotification() {
    if (!canPostNotifications()) {
      return
    }

    val totalSeconds =
      stateManager.timerState.value.totalSeconds
        .coerceAtLeast(0)

    val fullScreenIntent =
      Intent(this, MainActivity::class.java).apply {
        flags =
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

        putExtra(
          MainActivity.EXTRA_DESTINATION,
          MainActivity.DEST_TIMER_FINISHED
        )

        putExtra(
          MainActivity.EXTRA_TIMER_SECONDS,
          totalSeconds
        )
      }

    val fullScreenPendingIntent =
      PendingIntent.getActivity(
        this,
        REQUEST_CODE_TIMER_FINISHED,
        fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or
          PendingIntent.FLAG_IMMUTABLE
      )

    val dismissPendingIntent =
      createServicePendingIntent(
        requestCode =
          REQUEST_CODE_DISMISS_FINISHED,
        action =
          ACTION_DISMISS_TIMER_FINISHED
      )

    val restartPendingIntent =
      createServicePendingIntent(
        requestCode =
          REQUEST_CODE_RESTART_TIMER,
        action =
          ACTION_RESTART_TIMER
      )

    val notification =
      NotificationCompat.Builder(
        this,
        CHANNEL_ID_FINISHED
      )
        .setSmallIcon(
          android.R.drawable.ic_lock_idle_alarm
        )
        .setContentTitle("Timer finished")
        .setContentText("Time's up")
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(
          NotificationCompat.VISIBILITY_PUBLIC
        )
        .setFullScreenIntent(
          fullScreenPendingIntent,
          true
        )
        .setContentIntent(fullScreenPendingIntent)
        .setDeleteIntent(dismissPendingIntent)
        .setAutoCancel(false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .addAction(
          android.R.drawable.ic_media_play,
          "Restart",
          restartPendingIntent
        )
        .addAction(
          android.R.drawable.ic_menu_close_clear_cancel,
          "Dismiss",
          dismissPendingIntent
        )
        .build()

    try {
      notificationManager.notify(
        NOTIFICATION_ID_FINISHED,
        notification
      )
    } catch (e: SecurityException) {
      e.printStackTrace()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun cancelFinishedNotification() {
    try {
      notificationManager.cancel(
        NOTIFICATION_ID_FINISHED
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // Notification channels
  // ---------------------------------------------------------------------------

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    val activeChannel =
      NotificationChannel(
        CHANNEL_ID,
        "Metro timer and stopwatch",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description =
          "Ongoing timer and stopwatch status"

        enableVibration(false)
        setSound(null, null)
        setShowBadge(false)
      }

    val finishedChannel =
      NotificationChannel(
        CHANNEL_ID_FINISHED,
        "Metro timer alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description =
          "Alerts shown when a timer finishes"

        /*
         * SoundPlayer owns timer audio and vibration. Keeping this notification
         * channel silent prevents a second sound and conflicting vibration.
         */
        enableVibration(false)
        setSound(null, null)

        lockscreenVisibility =
          NotificationCompat.VISIBILITY_PUBLIC
      }

    try {
      notificationManager.createNotificationChannel(
        activeChannel
      )

      notificationManager.createNotificationChannel(
        finishedChannel
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // ---------------------------------------------------------------------------
  // PendingIntent helpers
  // ---------------------------------------------------------------------------

  private fun createServicePendingIntent(
    requestCode: Int,
    action: String
  ): PendingIntent {
    val serviceIntent =
      Intent(this, TimerService::class.java).apply {
        this.action = action
      }

    return PendingIntent.getService(
      this,
      requestCode,
      serviceIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or
        PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun canPostNotifications(): Boolean {
    return Build.VERSION.SDK_INT <
      Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
  }

  // ---------------------------------------------------------------------------
  // Wake lock
  // ---------------------------------------------------------------------------

  private fun acquireWakeLock(timeoutMillis: Long) {
    val safeTimeout =
      timeoutMillis.coerceAtLeast(1000L)

    try {
      val currentWakeLock = wakeLock ?: return

      if (currentWakeLock.isHeld) {
        currentWakeLock.release()
      }

      currentWakeLock.acquire(safeTimeout)
    } catch (e: SecurityException) {
      e.printStackTrace()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun releaseWakeLockIfIdle() {
    val timerRunning =
      stateManager.timerState.value.isRunning

    val stopwatchRunning =
      stateManager.stopwatchState.value.isRunning

    if (!timerRunning && !stopwatchRunning) {
      releaseWakeLock()
    }
  }

  private fun releaseWakeLock() {
    try {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onDestroy() {
    timerJob?.cancel()
    timerJob = null

    stopwatchJob?.cancel()
    stopwatchJob = null

    timerCompletionJob?.cancel()
    timerCompletionJob = null

    serviceScope.cancel()

    releaseWakeLock()
    wakeLock = null

    isForegroundStarted = false

    super.onDestroy()
  }
}
