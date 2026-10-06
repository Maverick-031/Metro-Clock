package app.metroclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.metroclock.ClockApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores enabled alarms after the device finishes booting.
 *
 * AlarmManager entries are generally cleared when a device restarts, so every
 * enabled alarm must be scheduled again.
 */
class BootReceiver : BroadcastReceiver() {

  companion object {

    /**
     * Some Android device manufacturers send this action instead of, or in
     * addition to, the standard BOOT_COMPLETED action.
     */
    private const val ACTION_QUICKBOOT_POWERON =
      "android.intent.action.QUICKBOOT_POWERON"
  }

  override fun onReceive(
    context: Context,
    intent: Intent
  ) {
    /*
     * Ignore any unexpected broadcast delivered to this receiver.
     */
    if (!isSupportedBootAction(intent.action)) {
      return
    }

    val application =
      context.applicationContext as? ClockApplication
        ?: return

    /*
     * Reading Room and DataStore is asynchronous work. goAsync() allows the
     * receiver to continue after onReceive() returns while keeping the
     * broadcast pending until finish() is called.
     */
    val pendingResult = goAsync()

    /*
     * Reuse the application-level scope rather than creating an unmanaged
     * CoroutineScope for every boot broadcast.
     */
    application.container.applicationScope.launch {
      try {
        restoreAlarms(application)
      } catch (e: CancellationException) {
        /*
         * Preserve normal coroutine cancellation behavior.
         */
        throw e
      } catch (e: Exception) {
        /*
         * A database or scheduling failure must not crash the application
         * process during boot.
         */
        e.printStackTrace()
      } finally {
        /*
         * Always finish the asynchronous broadcast, including error and
         * cancellation paths.
         */
        pendingResult.finish()
      }
    }
  }

  /**
   * Restores every enabled alarm when alarms have not been globally disabled.
   */
  private suspend fun restoreAlarms(
    application: ClockApplication
  ) {
    val container = application.container

    /*
     * Respect the global "Turn all alarms off" setting. The individual alarms
     * may still be enabled in Room so they can be restored when the user turns
     * the global switch back on.
     */
    val allAlarmsDisabled =
      try {
        container.settingsDataStore
          .allAlarmsDisabled
          .first()
      } catch (e: Exception) {
        /*
         * If settings cannot be read during boot, use the safer alarm-clock
         * behavior and attempt to restore enabled alarms.
         */
        e.printStackTrace()
        false
      }

    if (allAlarmsDisabled) {
      return
    }

    val enabledAlarms =
      try {
        container.alarmRepository
          .getEnabledAlarms()
      } catch (e: Exception) {
        e.printStackTrace()
        return
      }

    enabledAlarms.forEach { alarm ->
      try {
        /*
         * AlarmScheduler validates the alarm ID and enabled state, calculates
         * its next occurrence, and handles exact-alarm fallback.
         */
        container.alarmScheduler.schedule(alarm)
      } catch (e: SecurityException) {
        /*
         * Exact-alarm permission or access can be unavailable. AlarmScheduler
         * normally handles its fallback internally, but this protects the boot
         * receiver from unexpected platform failures.
         */
        e.printStackTrace()
      } catch (e: Exception) {
        /*
         * Continue restoring other alarms when one alarm fails.
         */
        e.printStackTrace()
      }
    }
  }

  private fun isSupportedBootAction(
    action: String?
  ): Boolean {
    return action == Intent.ACTION_BOOT_COMPLETED ||
      action == ACTION_QUICKBOOT_POWERON
  }
}
