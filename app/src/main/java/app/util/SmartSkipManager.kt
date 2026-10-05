package app.metroclock.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import app.metroclock.data.SettingsDataStore
import kotlinx.coroutines.flow.first

class SmartSkipManager(
  private val context: Context,
  private val settingsDataStore: SettingsDataStore
) {

  /**
   * Evaluates whether the user is currently off-site / on vacation based on Wi-Fi or GPS Geofence.
   * Returns true if the alarm should be skipped.
   * 
   * Logic: Alarm should RING (return false = don't skip) when:
   * - Smart Skip is disabled
   * - No home SSID / GPS location has been set by the user
   * - No Wi-Fi is connected (cellular only)
   * - Location permissions are missing
   * 
   * Alarm should be SKIPPED (return true) when:
   * - Connected to a DIFFERENT Wi-Fi than the saved home SSID
   * - Outside the approximate GPS geofence radius from the saved home location
   */
  suspend fun shouldSkipAlarmDueToLocation(): Boolean {
    val enabled = settingsDataStore.smartSkipEnabled.first()
    if (!enabled) return false

    val mode = settingsDataStore.smartSkipMode.first()
    return when (mode) {
      "wifi" -> isAwayFromHomeWifi()
      "gps" -> isAwayFromHomeGeofence()
      else -> isAwayFromHomeWifi() || isAwayFromHomeGeofence()
    }
  }

  /**
   * Checks the currently connected Wi-Fi SSID against the saved home SSID.
   * Returns true (skip alarm) ONLY if a different Wi-Fi is connected.
   * Returns false (ring alarm) if no Wi-Fi is connected, or if the saved home SSID is blank.
   */
  private suspend fun isAwayFromHomeWifi(): Boolean {
    val homeWifiSsid = settingsDataStore.smartSkipHomeWifi.first().trim()
    if (homeWifiSsid.isBlank()) return false // No home SSID set, don't skip

    return try {
      val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      
      // Note: On Android 10+, this requires Location permission AND the device's GPS toggle to be ON.
      // If GPS is off on the device, this will return "<unknown ssid>".
      @Suppress("DEPRECATION")
      val currentSsid = wifiManager?.connectionInfo?.ssid?.replace("\"", "")?.trim() ?: ""

      // If no Wi-Fi is connected, or SSID is unknown, ALARM SHOULD RING (return false = don't skip)
      if (currentSsid.isEmpty() || currentSsid == "<unknown ssid>") {
        return false
      }

      // If a Wi-Fi is connected, skip ONLY if it's a DIFFERENT SSID than home
      !currentSsid.equals(homeWifiSsid, ignoreCase = true)
    } catch (e: Exception) {
      false // On error, better to ring the alarm than to skip it silently
    }
  }

  /**
   * Checks the user's current approximate location against the saved home GPS coordinates.
   * Uses Network/Passive providers only (approximate) to save battery.
   * Returns true (skip alarm) if outside the geofence radius.
   */
  private suspend fun isAwayFromHomeGeofence(): Boolean {
    val homeLat = settingsDataStore.smartSkipHomeLat.first()
    val homeLng = settingsDataStore.smartSkipHomeLng.first()
    val radiusMeters = settingsDataStore.smartSkipRadiusMeters.first()

    // If home is not set (0,0), do not skip
    if (homeLat == 0.0 && homeLng == 0.0) return false

    // We only need COARSE location for approximate geofencing
    val hasCoarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasCoarse) return false

    return try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

      // Use ONLY approximate providers (Network and Passive) to save battery.
      // We intentionally do NOT use GPS_PROVIDER here to honor the "approximate" requirement.
      val providers = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
      )

      var lastKnown: Location? = null
      for (provider in providers) {
        try {
          val loc = locationManager?.getLastKnownLocation(provider)
          if (loc != null && (lastKnown == null || loc.time > lastKnown.time)) {
            lastKnown = loc
          }
        } catch (e: SecurityException) {
          // Permission was revoked, ignore and try the next provider
        }
      }

      if (lastKnown != null) {
        val results = FloatArray(1)
        Location.distanceBetween(
          lastKnown.latitude,
          lastKnown.longitude,
          homeLat,
          homeLng,
          results
        )
        val distance = results[0]
        // Skip alarm if OUTSIDE the home radius
        distance > radiusMeters
      } else {
        // No location available (e.g., GPS is off, no network). DON'T skip, alarm should ring.
        false
      }
    } catch (e: Exception) {
      false
    }
  }
}
