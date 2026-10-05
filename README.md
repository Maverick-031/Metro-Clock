# Metro Clock

A Windows Phone (Metro UI) styled alarm clock app built with Kotlin and Jetpack Compose.

## Features

- **Alarms**: Set multiple alarms with customizable repeat days, snooze time, and sound selection
- **Timer**: Countdown timer with custom sound selection and vibration
- **Stopwatch**: Track elapsed time with lap functionality
- **World Clock**: View time in different time zones
- **Smart Skip**: Location-aware alarm skipping for vacation mode
- **Calendar Integration**: Skip alarms on days with all-day calendar events

## Design System

- **Typography**: Uses Roboto font family with light/thin weights for large text
- **Colors**: Pure black background (#000000) with Yellow/Orange accent (#FFB900)
- **Animations**: Classic Windows Phone "turnstile" animations for transitions
- **Haptics**: Subtle haptic feedback on all button presses and interactions

## Architecture

- **MVVM Pattern**: ViewModel, Repository, and UI layers
- **Data Persistence**: Room database for alarms, DataStore for preferences
- **Background Work**: AlarmManager for alarms, ForegroundService for timers

## Requirements

- Android 7.0+ (API 24+)
- Required permissions:
  - `SCHEDULE_EXACT_ALARM`
  - `USE_EXACT_ALARM`
  - `POST_NOTIFICATIONS`
  - `VIBRATE`
  - `WAKE_LOCK`
  - `FOREGROUND_SERVICE`
  - `RECEIVE_BOOT_COMPLETED`
  - `READ_CALENDAR` (for calendar integration)
  - `ACCESS_FINE_LOCATION` (for Smart Skip)
  - `ACCESS_WIFI_STATE` (for Smart Skip)

## Building

```bash
./gradlew assembleDebug
```

## Usage

1. **Set an Alarm**: Tap the "+" button, set the time, and configure your preferences
2. **Timer**: Navigate to the Timer tab and set your desired duration
3. **Stopwatch**: Use the Stopwatch tab to track elapsed time
4. **World Clock**: Add cities to track time in different time zones
5. **Settings**: Customize app appearance and behavior

## Smart Skip

Enable Smart Skip in Settings to automatically pause alarms when:
- You're not at your home Wi-Fi network
- Your location indicates you're away from home

## Calendar Integration

Enable "Skip if calendar event" when creating/editing an alarm to automatically skip it on days with all-day calendar events.

## Credits

Inspired by the Windows Phone Metro UI design language.

## License

[MIT License](LICENSE)
