package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundPlayer(private val context: Context) {
  private var currentRingtone: Ringtone? = null
  private var currentMediaPlayer: MediaPlayer? = null
  private val handler = Handler(Looper.getMainLooper())
  private var volumeFadeRunnable: Runnable? = null
  private var autoSilenceRunnable: Runnable? = null

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
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
        startVibration()
      }

      // Schedule auto-silence
      val silenceMinutes = silenceAfterMinutes.toIntOrNull() ?: 0
      if (silenceMinutes > 0) {
        autoSilenceRunnable = Runnable {
          stopSound()
        }
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

      // Loop the timer expiry sound continuously (like a real timer expiring)
      // instead of playing it once like a notification ping.
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
        // Constant repeating vibration while the timer sound rings,
        // not a single one-shot notification buzz.
        startVibration()
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

  fun previewSound(soundName: String) {
    stopSound()
    try {
      val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      currentRingtone = RingtoneManager.getRingtone(context, uri)?.apply {
        play()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Previews an exact sound (by uri) for a limited time — used by the play button next to
   * the sound picker in the new alarm screen and the timer settings. Stops automatically.
   */
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
      // Fallback to Ringtone API for content uris MediaPlayer can't open directly
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

  private fun startVibration() {
    try {
      val pattern = longArrayOf(0, 500, 300, 500, 300, 1000)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(pattern, 0)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun startShortVibration() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(800)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun stopVibration() {
    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
