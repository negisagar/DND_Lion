# DNDLion

A Kotlin-based Android app to schedule and control Do Not Disturb (DND) mode automatically. Users can choose start/end times and select specific days to repeat. The app also can send auto-response SMS messages to incoming calls while DND is active.

## Features

- Schedule DND mode with custom start and end times
- Repeating schedules on specific days (e.g., Mon, Wed, Fri)
- Automatic SMS replies to callers during DND mode
- Manual “Stop DND” button to exit early if needed
- Persistent state — reactivates scheduled alarms after device reboot

## Requirements

- **Minimum Android SDK**: 21 (Lollipop)  
- **Android Studio**: Flamingo or newer  
- **Kotlin**: 1.8+  
- The device must grant permissions for DND mode, SMS sending, and call logs (if auto-response is enabled).

## Installation & Setup

1. **Clone** this repository:
   ```bash
   git clone https://github.com/negisagar/DNDLion.git
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. If you need a signed release APK:
5. Go to Build > Generate Signed Bundle / APK... and follow the steps.
6. Install/Run on a device or emulator that meets the requirements.

## Usage

1. **Schedule**  
   - Select the start time, end time, and any repeat days.  
   - Tap **Schedule DND Mode** to set up alarms.

2. **Stop DND**  
   - Tap **Stop DND Mode** if you need to exit DND before the scheduled end time.

3. **Permissions**  
   - Make sure you grant `Notification Policy Access` for DND, `SMS` permission for auto-response, and `Call Log` permission if you need to detect incoming calls.

## Contributing

1. Fork this repository and create a new branch for your feature or bugfix.  
2. Make your changes and push to your branch.  
3. Open a Pull Request describing your changes.

## License
MIT License Copyright (c) 2023 Permission is hereby granted...
