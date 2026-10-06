package app.metroclock.util

import android.content.Context
import android.database.Cursor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundPlayer(context: Context) {

  companion object {

    /**
     * Waveform format:
     * [initial delay, vibrate ON, pause OFF, vibrate ON, ...]
     */
    private val ALARM_VIBRATION_PATTERN =
      longArrayOf(0L, 1000L, 1000L)

    private val TIMER_VIBRATION_PATTERN =
      longArrayOf(0L, 400L, 200L, 400L, 2000L)

    private const val PREVIEW_DURATION_MS = 5000L
    private const val TIMER_FADE_DURATION_SECONDS = 5
    private const val VOLUME_FADE_STEPS = 20

    private const val MIN_FADE_VOLUME = 0.05f
    private const val TIMER_INITIAL_VOLUME = 0.10f
    private const val MAX_VOLUME = 1.0f
  }

  /*
   * Use the application context so this class does not accidentally retain
   * an Activity, Service, or other short-lived Context.
   */
  private val appContext = context.applicationContext

  private val handler = Handler(Looper.getMainLooper())

  private var currentMediaPlayer: MediaPlayer? = null
  private var currentRingtone: Ringtone? = null

  /*
   * Ringtone.isPlaying is not available on every Android version supported by
   * many projects. This flag also gives replay logic a consistent local state.
   */
  @Volatile
  private var audioPlaybackActive = false

  private var volumeFadeRunnable: Runnable? = null
  private var autoSilenceRunnable: Runnable? = null

  // Vibration keep-alive state.
  private var activeVibrationPattern: LongArray? = null
  private var vibrationKeepAliveRunnable: Runnable? = null

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager =
        appContext.getSystemService(
          Context.VIBRATOR_MANAGER_SERVICE
        ) as? VibratorManager

      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      appContext.getSystemService(
        Context.VIBRATOR_SERVICE
      ) as? Vibrator
    }
  }

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  /**
   * Starts alarm audio and optionally starts repeating vibration.
   *
   * [customUri] may be blank, in which case the system default alarm,
   * notification, or ringtone sound is used.
   *
   * [silenceAfterMinutes] should contain a number. A blank, invalid, zero,
   * negative, or "never" value disables automatic silence.
   *
   * [gradualVolumeSeconds] should contain a number. A blank, invalid, zero,
   * negative, or "never" value disables gradual volume.
   */
  fun playAlarmSound(
    customUri: String = "",
    vibrate: Boolean = true,
    silenceAfterMinutes: String = "10",
    gradualVolumeSeconds: String = "never"
  ) {
    stopSound()

    val alarmUri = resolveAlarmUri(customUri)

    if (alarmUri != null) {
      val gradualSeconds =
        gradualVolumeSeconds.toIntOrNull()?.coerceAtLeast(0) ?: 0

      val initialVolume =
        if (gradualSeconds > 0) MIN_FADE_VOLUME else MAX_VOLUME

      val started = startLoopingMediaPlayer(
        uri = alarmUri,
        initialVolume = initialVolume
      )

      if (started && gradualSeconds > 0) {
        startVolumeFade(
          durationSeconds = gradualSeconds,
          initialVolume = MIN_FADE_VOLUME
        )
      }
    }

    /*
     * Vibration remains independent from audio. This allows vibration to work
     * even if the device has no usable default alarm sound.
     */
    if (vibrate) {
      startVibration(ALARM_VIBRATION_PATTERN)
    }

    val silenceMinutes =
      silenceAfterMinutes.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

    if (silenceMinutes > 0L) {
      val delayMillis = safeMinutesToMillis(silenceMinutes)

      if (delayMillis > 0L) {
        scheduleAutoSilence(delayMillis)
      }
    }
  }

  /**
   * Starts the timer-finished sound and optionally starts repeating vibration.
   */
  fun playTimerFinishedSound(
    customUri: String = "",
    vibrate: Boolean = true,
    gradualVolume: Boolean = false
  ) {
    stopSound()

    val timerUri = resolveTimerUri(customUri)

    if (timerUri != null) {
      val initialVolume =
        if (gradualVolume) TIMER_INITIAL_VOLUME else MAX_VOLUME

      val started = startLoopingMediaPlayer(
        uri = timerUri,
        initialVolume = initialVolume
      )

      if (started && gradualVolume) {
        startVolumeFade(
          durationSeconds = TIMER_FADE_DURATION_SECONDS,
          initialVolume = TIMER_INITIAL_VOLUME
        )
      }
    }

    if (vibrate) {
      startVibration(TIMER_VIBRATION_PATTERN)
    }
  }

  /**
   * Plays a preview of the selected sound.
   *
   * Resolution order:
   *
   * 1. Blank or "default" uses the default notification or alarm sound.
   * 2. A content URI, file URI, android.resource URI, or absolute path is used.
   * 3. Other values are matched against installed system sound titles.
   * 4. If no title matches, the default notification or alarm sound is used.
   */
  fun previewSound(
    soundName: String,
    durationMillis: Long = PREVIEW_DURATION_MS
  ) {
    stopSound()

    val soundUri = resolvePreviewUri(soundName) ?: return

    playRingtonePreview(
      uri = soundUri,
      durationMillis = durationMillis
    )
  }

  /**
   * Plays a URI preview using MediaPlayer.
   *
   * If MediaPlayer cannot play the URI, this method attempts a Ringtone
   * fallback.
   */
  fun previewSoundUri(
    uriString: String,
    durationMillis: Long = 3000L
  ) {
    stopSound()

    if (uriString.isBlank()) {
      return
    }

    val uri = parsePlayableUri(uriString) ?: return
    val safeDuration = durationMillis.coerceAtLeast(0L)

    if (startPreviewMediaPlayer(uri)) {
      if (safeDuration == 0L) {
        stopSound()
      } else {
        scheduleAutoSilence(safeDuration)
      }

      return
    }

    playRingtonePreview(
      uri = uri,
      durationMillis = safeDuration
    )
  }

  /**
   * Returns true when this SoundPlayer instance currently owns active audio.
   *
   * Important: this only detects playback started by this exact SoundPlayer
   * instance. A separate SoundPlayer created by another receiver, service, or
   * ViewModel has separate playback state.
   */
  fun isSoundPlaying(): Boolean {
    val mediaPlayerPlaying = try {
      currentMediaPlayer?.isPlaying == true
    } catch (_: IllegalStateException) {
      false
    } catch (_: Exception) {
      false
    }

    return mediaPlayerPlaying ||
      (currentRingtone != null && audioPlaybackActive)
  }

  /**
   * Stops audio, pending callbacks, gradual volume, auto-silence, and
   * vibration.
   */
  fun stopSound() {
    cancelVolumeFade()
    cancelAutoSilence()

    /*
     * Clear references before stopping resources. This prevents callbacks from
     * seeing a resource that is already being released.
     */
    val mediaPlayer = currentMediaPlayer
    currentMediaPlayer = null

    val ringtone = currentRingtone
    currentRingtone = null

    audioPlaybackActive = false

    if (mediaPlayer != null) {
      try {
        mediaPlayer.setOnCompletionListener(null)
        mediaPlayer.setOnErrorListener(null)
      } catch (_: Exception) {
      }

      try {
        if (mediaPlayer.isPlaying) {
          mediaPlayer.stop()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      try {
        mediaPlayer.reset()
      } catch (_: Exception) {
      }

      try {
        mediaPlayer.release()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    if (ringtone != null) {
      try {
        ringtone.stop()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    stopVibration()
  }

  // ---------------------------------------------------------------------------
  // MediaPlayer
  // ---------------------------------------------------------------------------

  /**
   * Starts a looping MediaPlayer for an alarm or timer.
   */
  private fun startLoopingMediaPlayer(
    uri: Uri,
    initialVolume: Float
  ): Boolean {
    val mediaPlayer = MediaPlayer()

    return try {
      mediaPlayer.setAudioAttributes(createAlarmAudioAttributes())
      mediaPlayer.setDataSource(appContext, uri)
      mediaPlayer.isLooping = true

      val safeInitialVolume =
        initialVolume.coerceIn(MIN_FADE_VOLUME, MAX_VOLUME)

      mediaPlayer.setVolume(
        safeInitialVolume,
        safeInitialVolume
      )

      mediaPlayer.setOnErrorListener { player, _, _ ->
        if (currentMediaPlayer === player) {
          stopSound()
        } else {
          safelyReleaseMediaPlayer(player)
        }

        true
      }

      mediaPlayer.prepare()
      mediaPlayer.start()

      currentMediaPlayer = mediaPlayer
      audioPlaybackActive = true

      true
    } catch (e: Exception) {
      e.printStackTrace()
      safelyReleaseMediaPlayer(mediaPlayer)

      if (currentMediaPlayer === mediaPlayer) {
        currentMediaPlayer = null
      }

      audioPlaybackActive = false
      false
    }
  }

  /**
   * Starts a non-looping MediaPlayer for preview playback.
   */
  private fun startPreviewMediaPlayer(uri: Uri): Boolean {
    val mediaPlayer = MediaPlayer()

    return try {
      mediaPlayer.setAudioAttributes(createPreviewAudioAttributes())
      mediaPlayer.setDataSource(appContext, uri)
      mediaPlayer.isLooping = false

      mediaPlayer.setOnCompletionListener { completedPlayer ->
        if (currentMediaPlayer === completedPlayer) {
          stopSound()
        } else {
          safelyReleaseMediaPlayer(completedPlayer)
        }
      }

      mediaPlayer.setOnErrorListener { failedPlayer, _, _ ->
        if (currentMediaPlayer === failedPlayer) {
          stopSound()
        } else {
          safelyReleaseMediaPlayer(failedPlayer)
        }

        true
      }

      mediaPlayer.prepare()
      mediaPlayer.start()

      currentMediaPlayer = mediaPlayer
      audioPlaybackActive = true

      true
    } catch (e: Exception) {
      e.printStackTrace()
      safelyReleaseMediaPlayer(mediaPlayer)

      if (currentMediaPlayer === mediaPlayer) {
        currentMediaPlayer = null
      }

      audioPlaybackActive = false
      false
    }
  }

  private fun safelyReleaseMediaPlayer(mediaPlayer: MediaPlayer) {
    try {
      mediaPlayer.setOnCompletionListener(null)
      mediaPlayer.setOnErrorListener(null)
    } catch (_: Exception) {
    }

    try {
      mediaPlayer.reset()
    } catch (_: Exception) {
    }

    try {
      mediaPlayer.release()
    } catch (_: Exception) {
    }
  }

  // ---------------------------------------------------------------------------
  // Ringtone preview
  // ---------------------------------------------------------------------------

  private fun playRingtonePreview(
    uri: Uri,
    durationMillis: Long
  ) {
    val safeDuration = durationMillis.coerceAtLeast(0L)

    try {
      val ringtone =
        RingtoneManager.getRingtone(appContext, uri) ?: return

      ringtone.audioAttributes = createPreviewAudioAttributes()
      ringtone.play()

      currentRingtone = ringtone
      audioPlaybackActive = true

      if (safeDuration == 0L) {
        stopSound()
      } else {
        scheduleAutoSilence(safeDuration)
      }
    } catch (e: Exception) {
      e.printStackTrace()

      currentRingtone = null
      audioPlaybackActive = false
    }
  }

  // ---------------------------------------------------------------------------
  // Volume fade
  // ---------------------------------------------------------------------------

  private fun startVolumeFade(
    durationSeconds: Int,
    initialVolume: Float
  ) {
    cancelVolumeFade()

    if (durationSeconds <= 0) {
      setCurrentMediaPlayerVolume(MAX_VOLUME)
      return
    }

    val durationMillis = durationSeconds * 1000L

    val intervalMillis =
      (durationMillis / VOLUME_FADE_STEPS).coerceAtLeast(1L)

    val startingVolume =
      initialVolume.coerceIn(MIN_FADE_VOLUME, MAX_VOLUME)

    var currentStep = 1

    volumeFadeRunnable = object : Runnable {
      override fun run() {
        val mediaPlayer = currentMediaPlayer ?: return

        if (currentStep > VOLUME_FADE_STEPS) {
          try {
            mediaPlayer.setVolume(MAX_VOLUME, MAX_VOLUME)
          } catch (e: Exception) {
            e.printStackTrace()
          }

          volumeFadeRunnable = null
          return
        }

        val progress =
          currentStep.toFloat() / VOLUME_FADE_STEPS.toFloat()

        val volume =
          (
            startingVolume +
              ((MAX_VOLUME - startingVolume) * progress)
            ).coerceIn(startingVolume, MAX_VOLUME)

        try {
          mediaPlayer.setVolume(volume, volume)
        } catch (e: Exception) {
          e.printStackTrace()
          volumeFadeRunnable = null
          return
        }

        currentStep++

        if (currentStep <= VOLUME_FADE_STEPS) {
          handler.postDelayed(this, intervalMillis)
        } else {
          try {
            mediaPlayer.setVolume(MAX_VOLUME, MAX_VOLUME)
          } catch (e: Exception) {
            e.printStackTrace()
          }

          volumeFadeRunnable = null
        }
      }
    }

    handler.postDelayed(
      volumeFadeRunnable!!,
      intervalMillis
    )
  }

  private fun setCurrentMediaPlayerVolume(volume: Float) {
    val safeVolume = volume.coerceIn(0f, MAX_VOLUME)

    try {
      currentMediaPlayer?.setVolume(
        safeVolume,
        safeVolume
      )
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun cancelVolumeFade() {
    volumeFadeRunnable?.let(handler::removeCallbacks)
    volumeFadeRunnable = null
  }

  // ---------------------------------------------------------------------------
  // Auto-silence
  // ---------------------------------------------------------------------------

  private fun scheduleAutoSilence(delayMillis: Long) {
    cancelAutoSilence()

    if (delayMillis <= 0L) {
      return
    }

    autoSilenceRunnable = Runnable {
      stopSound()
    }

    handler.postDelayed(
      autoSilenceRunnable!!,
      delayMillis
    )
  }

  private fun cancelAutoSilence() {
    autoSilenceRunnable?.let(handler::removeCallbacks)
    autoSilenceRunnable = null
  }

  private fun safeMinutesToMillis(minutes: Long): Long {
    if (minutes <= 0L) {
      return 0L
    }

    val maxSafeMinutes = Long.MAX_VALUE / 60_000L

    return if (minutes > maxSafeMinutes) {
      Long.MAX_VALUE
    } else {
      minutes * 60_000L
    }
  }

  // ---------------------------------------------------------------------------
  // URI resolution
  // ---------------------------------------------------------------------------

  private fun resolveAlarmUri(customUri: String): Uri? {
    parsePlayableUri(customUri)?.let {
      return it
    }

    return RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_ALARM
    ) ?: RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_NOTIFICATION
    ) ?: RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_RINGTONE
    )
  }

  private fun resolveTimerUri(customUri: String): Uri? {
    parsePlayableUri(customUri)?.let {
      return it
    }

    return RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_NOTIFICATION
    ) ?: RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_ALARM
    ) ?: RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_RINGTONE
    )
  }

  private fun resolvePreviewUri(soundName: String): Uri? {
    if (
      soundName.isBlank() ||
      soundName.equals("default", ignoreCase = true)
    ) {
      return RingtoneManager.getDefaultUri(
        RingtoneManager.TYPE_NOTIFICATION
      ) ?: RingtoneManager.getDefaultUri(
        RingtoneManager.TYPE_ALARM
      )
    }

    parsePlayableUri(soundName)?.let {
      return it
    }

    findByTitle(soundName)?.let {
      return it
    }

    return RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_NOTIFICATION
    ) ?: RingtoneManager.getDefaultUri(
      RingtoneManager.TYPE_ALARM
    )
  }

  private fun parsePlayableUri(value: String): Uri? {
    if (value.isBlank()) {
      return null
    }

    return try {
      if (value.startsWith("/")) {
        Uri.fromFile(java.io.File(value))
      } else {
        val parsedUri = Uri.parse(value)

        when (parsedUri.scheme?.lowercase()) {
          "content",
          "file",
          "android.resource" -> parsedUri

          else -> null
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  private fun findByTitle(title: String): Uri? {
    var cursor: Cursor? = null

    return try {
      val ringtoneManager = RingtoneManager(appContext)

      ringtoneManager.setType(
        RingtoneManager.TYPE_NOTIFICATION or
          RingtoneManager.TYPE_ALARM or
          RingtoneManager.TYPE_RINGTONE
      )

      cursor = ringtoneManager.cursor

      if (!cursor.moveToFirst()) {
        null
      } else {
        var foundUri: Uri? = null

        do {
          val soundTitle = cursor.getString(
            RingtoneManager.TITLE_COLUMN_INDEX
          )

          if (soundTitle.equals(title, ignoreCase = true)) {
            foundUri = ringtoneManager.getRingtoneUri(
              cursor.position
            )
            break
          }
        } while (cursor.moveToNext())

        foundUri
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    } finally {
      try {
        cursor?.close()
      } catch (_: Exception) {
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Audio attributes
  // ---------------------------------------------------------------------------

  private fun createAlarmAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
  }

  private fun createPreviewAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_NOTIFICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
  }

  // ---------------------------------------------------------------------------
  // Vibration
  // ---------------------------------------------------------------------------

  /**
   * Starts repeating vibration and periodically reasserts it.
   *
   * The keep-alive helps restore the alarm vibration if another vibration
   * temporarily interrupts it.
   */
  private fun startVibration(pattern: LongArray) {
    val currentVibrator = vibrator ?: return

    if (!currentVibrator.hasVibrator()) {
      return
    }

    stopVibration()

    activeVibrationPattern = pattern.copyOf()
    vibrateNow(pattern)

    val patternDuration = pattern.sum().coerceAtLeast(1L)

    val keepAliveIntervalMillis =
      (patternDuration * 2L).coerceIn(
        4000L,
        10_000L
      )

    vibrationKeepAliveRunnable = object : Runnable {
      override fun run() {
        val currentPattern =
          activeVibrationPattern ?: return

        vibrateNow(currentPattern)

        if (activeVibrationPattern != null) {
          handler.postDelayed(
            this,
            keepAliveIntervalMillis
          )
        }
      }
    }

    handler.postDelayed(
      vibrationKeepAliveRunnable!!,
      keepAliveIntervalMillis
    )
  }

  private fun vibrateNow(pattern: LongArray) {
    val currentVibrator = vibrator ?: return

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val repeatingEffect =
          VibrationEffect.createWaveform(pattern, 0)

        if (
          Build.VERSION.SDK_INT >=
          Build.VERSION_CODES.TIRAMISU
        ) {
          val vibrationAttributes =
            VibrationAttributes.Builder()
              .setUsage(VibrationAttributes.USAGE_ALARM)
              .build()

          currentVibrator.vibrate(
            repeatingEffect,
            vibrationAttributes
          )
        } else {
          val audioAttributes =
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(
                AudioAttributes.CONTENT_TYPE_SONIFICATION
              )
              .build()

          @Suppress("DEPRECATION")
          currentVibrator.vibrate(
            repeatingEffect,
            audioAttributes
          )
        }
      } else {
        /*
         * The second argument is the repeat index. Zero means repeat the
         * complete waveform from its first element.
         */
        @Suppress("DEPRECATION")
        currentVibrator.vibrate(pattern, 0)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun stopVibration() {
    activeVibrationPattern = null

    vibrationKeepAliveRunnable?.let(
      handler::removeCallbacks
    )
    vibrationKeepAliveRunnable = null

    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
