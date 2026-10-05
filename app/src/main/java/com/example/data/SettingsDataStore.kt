package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "metro_clock_settings")

class SettingsDataStore(private val context: Context) {
  companion object {
    val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color_id")
    val KEY_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val KEY_LIGHT_THEME = booleanPreferencesKey("light_theme")
    val KEY_ALL_ALARMS_DISABLED = booleanPreferencesKey("all_alarms_disabled")

    // Alarm Settings
    val KEY_ALARM_VIBRATE = booleanPreferencesKey("alarm_vibrate")
    val KEY_ALARM_SILENCE_AFTER = stringPreferencesKey("alarm_silence_after")
    val KEY_ALARM_SNOOZE_LENGTH = intPreferencesKey("alarm_snooze_length")
    val KEY_ALARM_GRADUAL_VOLUME = stringPreferencesKey("alarm_gradual_volume")
    val KEY_ALARM_VOLUME_BUTTONS = stringPreferencesKey("alarm_volume_buttons")

    // Timer Settings
    val KEY_TIMER_SOUND_URI = stringPreferencesKey("timer_sound_uri")
    val KEY_TIMER_SOUND_TITLE = stringPreferencesKey("timer_sound_title")
    val KEY_TIMER_GRADUAL_VOLUME = booleanPreferencesKey("timer_gradual_volume")
    val KEY_TIMER_VIBRATE = booleanPreferencesKey("timer_vibrate")

    // Smart Skip / Location Aware Settings
    val KEY_SMART_SKIP_ENABLED = booleanPreferencesKey("smart_skip_enabled")
    val KEY_SMART_SKIP_MODE = stringPreferencesKey("smart_skip_mode") // "wifi" or "gps"
    val KEY_SMART_SKIP_HOME_WIFI = stringPreferencesKey("smart_skip_home_wifi")
    val KEY_SMART_SKIP_HOME_LAT = androidx.datastore.preferences.core.doublePreferencesKey("smart_skip_home_lat")
    val KEY_SMART_SKIP_HOME_LNG = androidx.datastore.preferences.core.doublePreferencesKey("smart_skip_home_lng")
    val KEY_SMART_SKIP_RADIUS_METERS = intPreferencesKey("smart_skip_radius_meters")
  }

  val selectedAccentId: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_ACCENT_COLOR] ?: "yellow"
  }

  val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_DYNAMIC_COLOR] ?: false
  }

  val isLightTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_LIGHT_THEME] ?: false
  }

  val allAlarmsDisabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_ALL_ALARMS_DISABLED] ?: false
  }

  // Alarm Settings Flows
  val alarmVibrate: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_ALARM_VIBRATE] ?: true
  }

  val alarmSilenceAfter: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_ALARM_SILENCE_AFTER] ?: "10"
  }

  val alarmSnoozeLength: Flow<Int> = context.dataStore.data.map { preferences ->
    preferences[KEY_ALARM_SNOOZE_LENGTH] ?: 10
  }

  val alarmGradualVolume: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_ALARM_GRADUAL_VOLUME] ?: "never"
  }

  val alarmVolumeButtons: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_ALARM_VOLUME_BUTTONS] ?: "snooze"
  }

  // Timer Settings Flows
  val timerSoundUri: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_TIMER_SOUND_URI] ?: ""
  }

  val timerSoundTitle: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_TIMER_SOUND_TITLE] ?: "Default Sound"
  }

  val timerGradualVolume: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_TIMER_GRADUAL_VOLUME] ?: false
  }

  val timerVibrate: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_TIMER_VIBRATE] ?: true
  }

  // Smart Skip Flows
  val smartSkipEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[KEY_SMART_SKIP_ENABLED] ?: false
  }

  val smartSkipMode: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_SMART_SKIP_MODE] ?: "wifi"
  }

  val smartSkipHomeWifi: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_SMART_SKIP_HOME_WIFI] ?: ""
  }

  val smartSkipHomeLat: Flow<Double> = context.dataStore.data.map { preferences ->
    preferences[KEY_SMART_SKIP_HOME_LAT] ?: 0.0
  }

  val smartSkipHomeLng: Flow<Double> = context.dataStore.data.map { preferences ->
    preferences[KEY_SMART_SKIP_HOME_LNG] ?: 0.0
  }

  val smartSkipRadiusMeters: Flow<Int> = context.dataStore.data.map { preferences ->
    preferences[KEY_SMART_SKIP_RADIUS_METERS] ?: 1000
  }

  // Mutators
  suspend fun setAccentColor(accentId: String) {
    context.dataStore.edit { it[KEY_ACCENT_COLOR] = accentId }
  }

  suspend fun setUseDynamicColor(useDynamic: Boolean) {
    context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = useDynamic }
  }

  suspend fun setLightTheme(enabled: Boolean) {
    context.dataStore.edit { it[KEY_LIGHT_THEME] = enabled }
  }

  suspend fun setAllAlarmsDisabled(disabled: Boolean) {
    context.dataStore.edit { it[KEY_ALL_ALARMS_DISABLED] = disabled }
  }

  suspend fun setAlarmVibrate(enabled: Boolean) {
    context.dataStore.edit { it[KEY_ALARM_VIBRATE] = enabled }
  }

  suspend fun setAlarmSilenceAfter(silenceAfter: String) {
    context.dataStore.edit { it[KEY_ALARM_SILENCE_AFTER] = silenceAfter }
  }

  suspend fun setAlarmSnoozeLength(minutes: Int) {
    context.dataStore.edit { it[KEY_ALARM_SNOOZE_LENGTH] = minutes }
  }

  suspend fun setAlarmGradualVolume(seconds: String) {
    context.dataStore.edit { it[KEY_ALARM_GRADUAL_VOLUME] = seconds }
  }

  suspend fun setAlarmVolumeButtons(action: String) {
    context.dataStore.edit { it[KEY_ALARM_VOLUME_BUTTONS] = action }
  }

  suspend fun setTimerSound(uri: String, title: String) {
    context.dataStore.edit {
      it[KEY_TIMER_SOUND_URI] = uri
      it[KEY_TIMER_SOUND_TITLE] = title
    }
  }

  suspend fun setTimerGradualVolume(enabled: Boolean) {
    context.dataStore.edit { it[KEY_TIMER_GRADUAL_VOLUME] = enabled }
  }

  suspend fun setTimerVibrate(enabled: Boolean) {
    context.dataStore.edit { it[KEY_TIMER_VIBRATE] = enabled }
  }

  suspend fun setSmartSkipEnabled(enabled: Boolean) {
    context.dataStore.edit { it[KEY_SMART_SKIP_ENABLED] = enabled }
  }

  suspend fun setSmartSkipMode(mode: String) {
    context.dataStore.edit { it[KEY_SMART_SKIP_MODE] = mode }
  }

  suspend fun setSmartSkipHomeWifi(ssid: String) {
    context.dataStore.edit { it[KEY_SMART_SKIP_HOME_WIFI] = ssid }
  }

  suspend fun setSmartSkipHomeLocation(lat: Double, lng: Double, radiusMeters: Int = 1000) {
    context.dataStore.edit {
      it[KEY_SMART_SKIP_HOME_LAT] = lat
      it[KEY_SMART_SKIP_HOME_LNG] = lng
      it[KEY_SMART_SKIP_RADIUS_METERS] = radiusMeters
    }
  }
}
