package app.metroclock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * One Preferences DataStore instance for the entire application process.
 *
 * This extension must be declared only once in the project. Do not create
 * another Context.dataStore property using the same file name.
 */
private val Context.metroClockDataStore: DataStore<Preferences> by
  preferencesDataStore(
    name = SettingsDataStore.DATASTORE_NAME
  )

/**
 * Persistent application settings for MetroClock.
 *
 * The class stores only the application context so a long-lived DataStore
 * instance cannot retain an Activity, Service, or BroadcastReceiver.
 */
class SettingsDataStore(context: Context) {

  companion object {

    internal const val DATASTORE_NAME =
      "metro_clock_settings"

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    const val DEFAULT_ACCENT_COLOR_ID =
      "yellow"

    const val DEFAULT_ALARM_SILENCE_AFTER =
      "10"

    const val DEFAULT_ALARM_SNOOZE_MINUTES =
      10

    const val DEFAULT_ALARM_GRADUAL_VOLUME =
      "never"

    const val DEFAULT_ALARM_VOLUME_BUTTON_ACTION =
      "snooze"

    const val DEFAULT_TIMER_SOUND_TITLE =
      "Default Sound"

    const val DEFAULT_SMART_SKIP_MODE =
      "wifi"

    const val DEFAULT_SMART_SKIP_RADIUS_METERS =
      1000

    private const val MIN_SNOOZE_MINUTES =
      1

    private const val MAX_SNOOZE_MINUTES =
      1440

    private const val MIN_SMART_SKIP_RADIUS_METERS =
      1

    private const val MAX_SMART_SKIP_RADIUS_METERS =
      100_000

    // -------------------------------------------------------------------------
    // General setting keys
    // -------------------------------------------------------------------------

    private val KEY_ACCENT_COLOR =
      stringPreferencesKey("accent_color_id")

    private val KEY_DYNAMIC_COLOR =
      booleanPreferencesKey("use_dynamic_color")

    private val KEY_LIGHT_THEME =
      booleanPreferencesKey("light_theme")

    private val KEY_ALL_ALARMS_DISABLED =
      booleanPreferencesKey("all_alarms_disabled")

    // -------------------------------------------------------------------------
    // Alarm setting keys
    // -------------------------------------------------------------------------

    private val KEY_ALARM_VIBRATE =
      booleanPreferencesKey("alarm_vibrate")

    private val KEY_ALARM_SILENCE_AFTER =
      stringPreferencesKey("alarm_silence_after")

    private val KEY_ALARM_SNOOZE_LENGTH =
      intPreferencesKey("alarm_snooze_length")

    private val KEY_ALARM_GRADUAL_VOLUME =
      stringPreferencesKey("alarm_gradual_volume")

    private val KEY_ALARM_VOLUME_BUTTONS =
      stringPreferencesKey("alarm_volume_buttons")

    // -------------------------------------------------------------------------
    // Timer setting keys
    // -------------------------------------------------------------------------

    private val KEY_TIMER_SOUND_URI =
      stringPreferencesKey("timer_sound_uri")

    private val KEY_TIMER_SOUND_TITLE =
      stringPreferencesKey("timer_sound_title")

    private val KEY_TIMER_GRADUAL_VOLUME =
      booleanPreferencesKey("timer_gradual_volume")

    private val KEY_TIMER_VIBRATE =
      booleanPreferencesKey("timer_vibrate")

    // -------------------------------------------------------------------------
    // Smart Skip setting keys
    // -------------------------------------------------------------------------

    private val KEY_SMART_SKIP_ENABLED =
      booleanPreferencesKey("smart_skip_enabled")

    private val KEY_SMART_SKIP_MODE =
      stringPreferencesKey("smart_skip_mode")

    private val KEY_SMART_SKIP_HOME_WIFI =
      stringPreferencesKey("smart_skip_home_wifi")

    private val KEY_SMART_SKIP_HOME_LAT =
      doublePreferencesKey("smart_skip_home_lat")

    private val KEY_SMART_SKIP_HOME_LNG =
      doublePreferencesKey("smart_skip_home_lng")

    private val KEY_SMART_SKIP_RADIUS_METERS =
      intPreferencesKey("smart_skip_radius_meters")
  }

  private val appContext: Context =
    context.applicationContext

  private val dataStore: DataStore<Preferences>
    get() = appContext.metroClockDataStore

  /**
   * Handles temporary Preferences DataStore read failures.
   *
   * IOException produces empty preferences through each individual setting's
   * default mapping. Unexpected errors are rethrown instead of being hidden.
   */
  private val preferencesFlow: Flow<Preferences> =
    dataStore.data.catch { exception ->
      if (exception is IOException) {
        emit(
          androidx.datastore.preferences.core.emptyPreferences()
        )
      } else {
        throw exception
      }
    }

  // ---------------------------------------------------------------------------
  // General setting flows
  // ---------------------------------------------------------------------------

  val selectedAccentId: Flow<String> =
    preferencesFlow.map { preferences ->
      preferences[KEY_ACCENT_COLOR]
        ?.trim()
        ?.takeIf { value -> value.isNotEmpty() }
        ?: DEFAULT_ACCENT_COLOR_ID
    }

  val useDynamicColor: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_DYNAMIC_COLOR] ?: false
    }

  val isLightTheme: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_LIGHT_THEME] ?: false
    }

  val allAlarmsDisabled: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_ALL_ALARMS_DISABLED] ?: false
    }

  // ---------------------------------------------------------------------------
  // Alarm setting flows
  // ---------------------------------------------------------------------------

  val alarmVibrate: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_ALARM_VIBRATE] ?: true
    }

  val alarmSilenceAfter: Flow<String> =
    preferencesFlow.map { preferences ->
      normalizeDurationSetting(
        value = preferences[KEY_ALARM_SILENCE_AFTER],
        defaultValue = DEFAULT_ALARM_SILENCE_AFTER
      )
    }

  val alarmSnoozeLength: Flow<Int> =
    preferencesFlow.map { preferences ->
      normalizeSnoozeMinutes(
        preferences[KEY_ALARM_SNOOZE_LENGTH]
          ?: DEFAULT_ALARM_SNOOZE_MINUTES
      )
    }

  val alarmGradualVolume: Flow<String> =
    preferencesFlow.map { preferences ->
      normalizeDurationSetting(
        value = preferences[KEY_ALARM_GRADUAL_VOLUME],
        defaultValue = DEFAULT_ALARM_GRADUAL_VOLUME
      )
    }

  val alarmVolumeButtons: Flow<String> =
    preferencesFlow.map { preferences ->
      normalizeVolumeButtonAction(
        preferences[KEY_ALARM_VOLUME_BUTTONS]
      )
    }

  // ---------------------------------------------------------------------------
  // Timer setting flows
  // ---------------------------------------------------------------------------

  /**
   * Empty URI means that SoundPlayer should use the system default sound.
   */
  val timerSoundUri: Flow<String> =
    preferencesFlow.map { preferences ->
      preferences[KEY_TIMER_SOUND_URI]
        ?.trim()
        .orEmpty()
    }

  val timerSoundTitle: Flow<String> =
    preferencesFlow.map { preferences ->
      preferences[KEY_TIMER_SOUND_TITLE]
        ?.trim()
        ?.takeIf { title -> title.isNotEmpty() }
        ?: DEFAULT_TIMER_SOUND_TITLE
    }

  val timerGradualVolume: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_TIMER_GRADUAL_VOLUME] ?: false
    }

  val timerVibrate: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_TIMER_VIBRATE] ?: true
    }

  // ---------------------------------------------------------------------------
  // Smart Skip setting flows
  // ---------------------------------------------------------------------------

  val smartSkipEnabled: Flow<Boolean> =
    preferencesFlow.map { preferences ->
      preferences[KEY_SMART_SKIP_ENABLED] ?: false
    }

  val smartSkipMode: Flow<String> =
    preferencesFlow.map { preferences ->
      normalizeSmartSkipMode(
        preferences[KEY_SMART_SKIP_MODE]
      )
    }

  val smartSkipHomeWifi: Flow<String> =
    preferencesFlow.map { preferences ->
      preferences[KEY_SMART_SKIP_HOME_WIFI]
        ?.trim()
        .orEmpty()
    }

  val smartSkipHomeLat: Flow<Double> =
    preferencesFlow.map { preferences ->
      normalizeLatitude(
        preferences[KEY_SMART_SKIP_HOME_LAT]
          ?: 0.0
      )
    }

  val smartSkipHomeLng: Flow<Double> =
    preferencesFlow.map { preferences ->
      normalizeLongitude(
        preferences[KEY_SMART_SKIP_HOME_LNG]
          ?: 0.0
      )
    }

  val smartSkipRadiusMeters: Flow<Int> =
    preferencesFlow.map { preferences ->
      normalizeRadiusMeters(
        preferences[KEY_SMART_SKIP_RADIUS_METERS]
          ?: DEFAULT_SMART_SKIP_RADIUS_METERS
      )
    }

  // ---------------------------------------------------------------------------
  // General setting mutations
  // ---------------------------------------------------------------------------

  suspend fun setAccentColor(accentId: String) {
    val normalizedAccentId =
      accentId.trim().ifBlank {
        DEFAULT_ACCENT_COLOR_ID
      }

    dataStore.edit { preferences ->
      preferences[KEY_ACCENT_COLOR] =
        normalizedAccentId
    }
  }

  suspend fun setUseDynamicColor(
    useDynamic: Boolean
  ) {
    dataStore.edit { preferences ->
      preferences[KEY_DYNAMIC_COLOR] =
        useDynamic
    }
  }

  suspend fun setLightTheme(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[KEY_LIGHT_THEME] =
        enabled
    }
  }

  suspend fun setAllAlarmsDisabled(
    disabled: Boolean
  ) {
    dataStore.edit { preferences ->
      preferences[KEY_ALL_ALARMS_DISABLED] =
        disabled
    }
  }

  // ---------------------------------------------------------------------------
  // Alarm setting mutations
  // ---------------------------------------------------------------------------

  suspend fun setAlarmVibrate(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[KEY_ALARM_VIBRATE] =
        enabled
    }
  }

  suspend fun setAlarmSilenceAfter(
    silenceAfter: String
  ) {
    val normalizedValue =
      normalizeDurationSetting(
        value = silenceAfter,
        defaultValue = DEFAULT_ALARM_SILENCE_AFTER
      )

    dataStore.edit { preferences ->
      preferences[KEY_ALARM_SILENCE_AFTER] =
        normalizedValue
    }
  }

  suspend fun setAlarmSnoozeLength(
    minutes: Int
  ) {
    val normalizedMinutes =
      normalizeSnoozeMinutes(minutes)

    dataStore.edit { preferences ->
      preferences[KEY_ALARM_SNOOZE_LENGTH] =
        normalizedMinutes
    }
  }

  suspend fun setAlarmGradualVolume(
    seconds: String
  ) {
    val normalizedValue =
      normalizeDurationSetting(
        value = seconds,
        defaultValue = DEFAULT_ALARM_GRADUAL_VOLUME
      )

    dataStore.edit { preferences ->
      preferences[KEY_ALARM_GRADUAL_VOLUME] =
        normalizedValue
    }
  }

  suspend fun setAlarmVolumeButtons(
    action: String
  ) {
    val normalizedAction =
      normalizeVolumeButtonAction(action)

    dataStore.edit { preferences ->
      preferences[KEY_ALARM_VOLUME_BUTTONS] =
        normalizedAction
    }
  }

  // ---------------------------------------------------------------------------
  // Timer setting mutations
  // ---------------------------------------------------------------------------

  /**
   * Saves timer sound URI and title atomically.
   *
   * An empty URI represents the system default sound.
   */
  suspend fun setTimerSound(
    uri: String,
    title: String
  ) {
    val normalizedUri = uri.trim()

    val normalizedTitle =
      title.trim().ifBlank {
        DEFAULT_TIMER_SOUND_TITLE
      }

    dataStore.edit { preferences ->
      preferences[KEY_TIMER_SOUND_URI] =
        normalizedUri

      preferences[KEY_TIMER_SOUND_TITLE] =
        normalizedTitle
    }
  }

  suspend fun setTimerGradualVolume(
    enabled: Boolean
  ) {
    dataStore.edit { preferences ->
      preferences[KEY_TIMER_GRADUAL_VOLUME] =
        enabled
    }
  }

  suspend fun setTimerVibrate(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[KEY_TIMER_VIBRATE] =
        enabled
    }
  }

  // ---------------------------------------------------------------------------
  // Smart Skip setting mutations
  // ---------------------------------------------------------------------------

  suspend fun setSmartSkipEnabled(
    enabled: Boolean
  ) {
    dataStore.edit { preferences ->
      preferences[KEY_SMART_SKIP_ENABLED] =
        enabled
    }
  }

  suspend fun setSmartSkipMode(mode: String) {
    val normalizedMode =
      normalizeSmartSkipMode(mode)

    dataStore.edit { preferences ->
      preferences[KEY_SMART_SKIP_MODE] =
        normalizedMode
    }
  }

  suspend fun setSmartSkipHomeWifi(
    ssid: String
  ) {
    dataStore.edit { preferences ->
      preferences[KEY_SMART_SKIP_HOME_WIFI] =
        ssid.trim()
    }
  }

  suspend fun setSmartSkipHomeLocation(
    lat: Double,
    lng: Double,
    radiusMeters: Int = DEFAULT_SMART_SKIP_RADIUS_METERS
  ) {
    val normalizedLat =
      normalizeLatitude(lat)

    val normalizedLng =
      normalizeLongitude(lng)

    val normalizedRadius =
      normalizeRadiusMeters(radiusMeters)

    /*
     * Save the complete home location atomically so SmartSkipManager never
     * observes a new latitude with an old longitude or radius.
     */
    dataStore.edit { preferences ->
      preferences[KEY_SMART_SKIP_HOME_LAT] =
        normalizedLat

      preferences[KEY_SMART_SKIP_HOME_LNG] =
        normalizedLng

      preferences[KEY_SMART_SKIP_RADIUS_METERS] =
        normalizedRadius
    }
  }

  // ---------------------------------------------------------------------------
  // Validation helpers
  // ---------------------------------------------------------------------------

  /**
   * Normalizes duration settings accepted by SoundPlayer.
   *
   * Supported values are:
   *
   * - "never"
   * - A non-negative whole number stored as a string
   */
  private fun normalizeDurationSetting(
    value: String?,
    defaultValue: String
  ): String {
    val normalized =
      value
        ?.trim()
        ?.lowercase()
        .orEmpty()

    if (
      normalized.isEmpty() ||
      normalized == "never"
    ) {
      return if (normalized == "never") {
        "never"
      } else {
        defaultValue
      }
    }

    val numericValue =
      normalized.toLongOrNull()
        ?: return defaultValue

    return numericValue
      .coerceAtLeast(0L)
      .toString()
  }

  private fun normalizeSnoozeMinutes(
    minutes: Int
  ): Int {
    return minutes.coerceIn(
      MIN_SNOOZE_MINUTES,
      MAX_SNOOZE_MINUTES
    )
  }

  /**
   * Supported volume button actions:
   *
   * - "snooze"
   * - "stop"
   * - "do nothing"
   */
  private fun normalizeVolumeButtonAction(
    action: String?
  ): String {
    return when (
      action
        ?.trim()
        ?.lowercase()
    ) {
      "snooze" -> "snooze"

      "stop",
      "dismiss" -> "stop"

      "do nothing",
      "nothing",
      "ignore" -> "do nothing"

      else -> DEFAULT_ALARM_VOLUME_BUTTON_ACTION
    }
  }

  /**
   * Supported Smart Skip modes:
   *
   * - "wifi"
   * - "gps"
   */
  private fun normalizeSmartSkipMode(
    mode: String?
  ): String {
    return when (
      mode
        ?.trim()
        ?.lowercase()
    ) {
      "gps",
      "location" -> "gps"

      "wifi",
      "wi-fi" -> "wifi"

      else -> DEFAULT_SMART_SKIP_MODE
    }
  }

  private fun normalizeLatitude(
    latitude: Double
  ): Double {
    if (!latitude.isFinite()) {
      return 0.0
    }

    return latitude.coerceIn(
      -90.0,
      90.0
    )
  }

  private fun normalizeLongitude(
    longitude: Double
  ): Double {
    if (!longitude.isFinite()) {
      return 0.0
    }

    return longitude.coerceIn(
      -180.0,
      180.0
    )
  }

  private fun normalizeRadiusMeters(
    radiusMeters: Int
  ): Int {
    return radiusMeters.coerceIn(
      MIN_SMART_SKIP_RADIUS_METERS,
      MAX_SMART_SKIP_RADIUS_METERS
    )
  }
}
