package app.metroclock.util

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
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
   * - Connected to home Wi-Fi SSID
   * - No Wi-Fi connected (cellular only) - alarm should still ring
   * - Within home GPS geofence radius
   * 
   * Alarm should be SKIPPED (return true) when:
   * - Smart Skip is enabled AND
   * - Connected to a different Wi-Fi (not home) OR
   * - Outside home GPS geofence
      */
  suspend fun shouldSkipAlarmDueToLocation(): Boolean {
    val enabled = settingsDataStore.smartSkipEnabled.first()
    if (!enabled) return false
​
    val mode = settingsDataStore.smartSkipMode.first()
    return when (mode) {
      "wifi" -> isAwayFromHomeWifi()
      "gps" -> isAwayFromHomeGeofence()
      else -> isAwayFromHomeWifi() || isAwayFromHomeGeofence()
    }
  }
​
  private suspend fun isAwayFromHomeWifi(): Boolean {
    val homeWifiSsid = settingsDataStore.smartSkipHomeWifi.first()
    if (homeWifiSsid.isBlank()) return false
​
    return try {
      val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      val currentSsid = wifiManager?.connectionInfo?.ssid?.replace("\"", "") ?: ""
      if (currentSsid.isNotEmpty() && currentSsid != "<unknown ssid>") {
        // Skip alarm if connected to a DIFFERENT Wi-Fi than home
        !currentSsid.equals(homeWifiSsid, ignoreCase = true)
      } else {
        // No Wi-Fi connected (cellular only) - DON'T skip, alarm should ring
        false
      }
    } catch (e: Exception) {
      false
    }
  }

  private suspend fun isAwayFromHomeGeofence(): Boolean {
    val homeLat = settingsDataStore.smartSkipHomeLat.first()
    val homeLng = settingsDataStore.smartSkipHomeLng.first()
    val radiusMeters = settingsDataStore.smartSkipRadiusMeters.first()

    // If home is not set (0,0), do not skip - alarm should ring
    if (homeLat == 0.0 && homeLng == 0.0) return false
​
    val hasFine = ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
​
    val hasCoarse = ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
​
    // Use approximate location if fine is not available
    if (!hasFine && !hasCoarse) return false
​
    return try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      
      // Try to get location from any available provider
      // Prefer GPS for precision, but use network if GPS not available
      val providers = mutableListOf<String>()
      if (hasFine) {
        providers.add(LocationManager.GPS_PROVIDER)
      }
      providers.add(LocationManager.NETWORK_PROVIDER)
      providers.add(LocationManager.PASSIVE_PROVIDER)
​
      var lastKnown: Location? = null
      for (provider in providers) {
        val loc = locationManager?.getLastKnownLocation(provider)
        if (loc != null && (lastKnown == null || loc.time > lastKnown.time)) {
          lastKnown = loc
        }
      }
​
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
        // No location available - DON'T skip, alarm should ring
        false
      }
    } catch (e: Exception) {
      false
    }
  }
}
