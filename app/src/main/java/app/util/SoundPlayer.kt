package app.metroclock.util

import android.content.Context
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

class SoundPlayer(private val context: Context) {

  companion object {
    /**
     * Waveform format: [initial delay, vibrate ON, pause OFF, vibrate ON, ...]
     */
    private val ALARM_VIBRATION_PATTERN = longArrayOf(0, 1000, 1000)
    private val TIMER_VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400, 2000)

    private const val PREVIEW_DURATION_MS = 5000L
  }

  private var currentRingtone: Ringtone? = null
  private var currentMediaPlayer: MediaPlayer? = null
  private val handler = Handler(Looper.getMainLooper())
  private var volumeFadeRunnable: Runnable? = null
  private var autoSilenceRunnable: Runnable? = null

  // Vibration keep-alive state
  private var activeVibrationPattern: LongArray? = null
  private var vibrationKeepAliveRunnable: Runnable? = null

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun playAlarmSound(
    customUri: String = "",
    vibrate: Boolean = true,
    silenceAfterMinutes: String = "10",
    gradualVolumeSeconds: String = "never"
  ) {
    stopSound()
    try {
      val parsedCustomUri = if (customUri.isNotBlank()) Uri.parse(customUri) else null
      val alarmUri: Uri = parsedCustomUri
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

      val gradualSecs = gradualVolumeSeconds.toIntOrNull() ?: 0

      if (gradualSecs > 0) {
        // Use MediaPlayer for gradual volume fade-in
        currentMediaPlayer = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ALARM)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          setDataSource(context, alarmUri)
          isLooping = true
          setVolume(0.05f, 0.05f)
          prepare()
          start()
        }
        startVolumeFade(gradualSecs)
      } else {
        currentRingtone = RingtoneManager.getRingtone(context, alarmUri)?.apply {
          audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLooping = true
          }
          play()
        }
      }

      if (vibrate) {
        startVibration(ALARM_VIBRATION_PATTERN)
      }

      // Schedule auto-silence
      val silenceMinutes = silenceAfterMinutes.toIntOrNull() ?: 0
      if (silenceMinutes > 0) {
        autoSilenceRunnable = Runnable { stopSound() }
        handler.postDelayed(autoSilenceRunnable!!, silenceMinutes * 60 * 1000L)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun playTimerFinishedSound(
    customUri: String = "",
    vibrate: Boolean = true,
    gradualVolume: Boolean = false
  ) {
    stopSound()
    try {
      val customParsed = if (customUri.isNotBlank()) {
        try { Uri.parse(customUri) } catch (e: Exception) { null }
      } else null

      val soundUri: Uri? = customParsed
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

      if (soundUri == null) return

      currentMediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        setDataSource(context, soundUri)
        isLooping = true
        if (gradualVolume) setVolume(0.1f, 0.1f)
        prepare()
        start()
      }
      if (gradualVolume) {
        startVolumeFade(5)
      }

      if (vibrate) {
        startVibration(TIMER_VIBRATION_PATTERN)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun startVolumeFade(durationSeconds: Int) {
    val steps = 20
    val intervalMillis = (durationSeconds * 1000L) / steps
    var currentStep = 1

    volumeFadeRunnable = object : Runnable {
      override fun run() {
        val mp = currentMediaPlayer ?: return
        if (currentStep <= steps) {
          val vol = (currentStep.toFloat() / steps.toFloat()).coerceIn(0.05f, 1.0f)
          try {
            mp.setVolume(vol, vol)
          } catch (e: Exception) {
            e.printStackTrace()
          }
          currentStep++
          handler.postDelayed(this, intervalMillis)
        }
      }
    }
    handler.postDelayed(volumeFadeRunnable!!, intervalMillis)
  }

  /**
   * Plays a preview of the selected sound. Resolves [soundName] as follows:
   * 1. Blank / "default" -> default notification sound
   * 2. A content:// or file:// URI (or absolute path) -> played directly
   * 3. Otherwise -> matched by title against system alarm/notification/ringtone sounds
   * 4. Falls back to the default notification sound if nothing matches
   */
  fun previewSound(soundName: String, durationMillis: Long = PREVIEW_DURATION_MS) {
    stopSound()
    try {
      val uri = resolvePreviewUri(soundName) ?: return
      currentRingtone = RingtoneManager.getRingtone(context, uri)?.apply {
        audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        play()
      }
      autoSilenceRunnable = Runnable { stopSound() }
      handler.postDelayed(autoSilenceRunnable!!, durationMillis)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun resolvePreviewUri(soundName: String): Uri? {
    if (soundName.isBlank() || soundName.equals("default", ignoreCase = true)) {
      return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    // 1. Looks like a URI or absolute path?
    val parsed = try { Uri.parse(soundName) } catch (e: Exception) { null }
    if (parsed != null &&
      (parsed.scheme == "content" || parsed.scheme == "file" || soundName.startsWith("/"))
    ) {
      return parsed
    }

    // 2. Try to match a system sound by its title (e.g. "Platinum", "Lunar")
    findByTitle(soundName)?.let { return it }

    // 3. Fall back to default
    return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
  }

  private fun findByTitle(title: String): Uri? {
    return try {
      val manager = RingtoneManager(context)
      manager.setType(
        RingtoneManager.TYPE_NOTIFICATION or
          RingtoneManager.TYPE_ALARM or
          RingtoneManager.TYPE_RINGTONE
      )
      val cursor = manager.cursor
      var found: Uri? = null
      if (cursor.moveToFirst()) {
        do {
          val soundTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
          if (soundTitle?.equals(title, ignoreCase = true) == true) {
            found = manager.getRingtoneUri(cursor.position)
            break
          }
        } while (cursor.moveToNext())
      }
      cursor.close()
      found
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun previewSoundUri(uriString: String, durationMillis: Long = 3000L) {
    stopSound()
    try {
      if (uriString.isBlank()) return
      val parsedUri = Uri.parse(uriString)
      currentMediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        setDataSource(context, parsedUri)
        prepare()
        setOnCompletionListener { stopSound() }
        start()
      }
      autoSilenceRunnable = Runnable { stopSound() }
      handler.postDelayed(autoSilenceRunnable!!, durationMillis)
    } catch (e: Exception) {
      try {
        currentRingtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))?.apply {
          play()
        }
        autoSilenceRunnable = Runnable { stopSound() }
        handler.postDelayed(autoSilenceRunnable!!, durationMillis)
      } catch (e2: Exception) {
        e2.printStackTrace()
      }
    }
  }

  fun stopSound() {
    try {
      volumeFadeRunnable?.let { handler.removeCallbacks(it) }
      volumeFadeRunnable = null

      autoSilenceRunnable?.let { handler.removeCallbacks(it) }
      autoSilenceRunnable = null

      currentMediaPlayer?.let {
        if (it.isPlaying) it.stop()
        it.release()
      }
      currentMediaPlayer = null

      currentRingtone?.stop()
      currentRingtone = null

      stopVibration()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // --- VIBRATION ---

  /**
   * Starts a repeating vibration pattern and schedules a keep-alive that
   * re-asserts it periodically, in case another app cancels our vibration
   * (e.g. an incoming notification). The re-assert interval is a multiple of
   * the pattern length, so restarts are seamless.
   */
  private fun startVibration(pattern: LongArray) {
    val vib = vibrator ?: return
    if (!vib.hasVibrator()) return

    // Cancel any previous keep-alive before starting fresh
    vibrationKeepAliveRunnable?.let { handler.removeCallbacks(it) }
    activeVibrationPattern = pattern

    vibrateNow(pattern)

    val keepAliveIntervalMs = (pattern.sum() * 2).coerceIn(4000L, 10000L)
    vibrationKeepAliveRunnable = object : Runnable {
      override fun run() {
        val currentPattern = activeVibrationPattern ?: return
        vibrateNow(currentPattern)
        handler.postDelayed(this, keepAliveIntervalMs)
      }
    }
    handler.postDelayed(vibrationKeepAliveRunnable!!, keepAliveIntervalMs)
  }

  private fun vibrateNow(pattern: LongArray) {
    val vib = vibrator ?: return
    try {
      // repeat = 0 -> loop the entire pattern forever.
      // (repeat = -1 would play it exactly ONCE — that was the original bug.)
      val repeatingEffect = VibrationEffect.createWaveform(pattern, 0)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // API 31+ : VibrationAttributes with USAGE_ALARM so the vibration
        // isn't silenced by Silent or DND mode.
        val attributes = VibrationAttributes.Builder()
          .setUsage(VibrationAttributes.USAGE_ALARM)
          .build()
        vib.vibrate(repeatingEffect, attributes)
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // API 26-30 : standard VibrationEffect with alarm audio attributes
        val audioAttrs = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
        @Suppress("DEPRECATION")
        vib.vibrate(repeatingEffect, audioAttrs)
      } else {
        // API < 26 : deprecated overload — here the 2nd param IS the repeat index
        @Suppress("DEPRECATION")
        vib.vibrate(pattern, 0)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun stopVibration() {
    activeVibrationPattern = null
    vibrationKeepAliveRunnable?.let { handler.removeCallbacks(it) }
    vibrationKeepAliveRunnable = null
    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
