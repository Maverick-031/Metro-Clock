package app.metroclock.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

object SmartSkipHelper {
  /**
   * Evaluates if smart skip conditions are met (e.g. user is away on vacation/off-site).
   */
  fun isUserAwayFromHome(context: Context, homeWifiSsid: String, vacationModeEnabled: Boolean): Boolean {
    if (vacationModeEnabled) return true

    if (homeWifiSsid.isBlank()) return false

    try {
      val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      val connectionInfo = wifiManager?.connectionInfo
      val currentSsid = connectionInfo?.ssid?.trim('"') ?: ""
      if (currentSsid.isNotEmpty() && currentSsid != "<unknown ssid>") {
        return !currentSsid.equals(homeWifiSsid, ignoreCase = true)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return false
  }
}
