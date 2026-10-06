package app.metroclock

import android.app.Application

/**
 * Root application class for MetroClock.
 *
 * AppContainer is created once for the entire application process. Activities,
 * services, and receivers must obtain dependencies from this container instead
 * of constructing separate AppContainer, SoundPlayer, or TimerStateManager
 * instances.
 */
class ClockApplication : Application() {

  val container: AppContainer by lazy(
    mode = LazyThreadSafetyMode.SYNCHRONIZED
  ) {
    AppContainer(applicationContext)
  }

  override fun onCreate() {
    super.onCreate()

    /*
     * Force initialization during application startup.
     *
     * This keeps dependency initialization deterministic while retaining the
     * safety of a read-only lazy property.
     */
    container
  }
}
