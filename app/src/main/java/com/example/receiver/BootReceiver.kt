package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ClockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
      val app = context.applicationContext as? ClockApplication ?: return
      val repository = app.container.alarmRepository
      val scheduler = app.container.alarmScheduler

      CoroutineScope(Dispatchers.IO).launch {
        val enabledAlarms = repository.getEnabledAlarms()
        for (alarm in enabledAlarms) {
          scheduler.schedule(alarm)
        }
      }
    }
  }
}
