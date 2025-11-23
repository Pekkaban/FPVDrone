ArduBluetooth - minimal Android Studio project template (BLE + ArduPilot MAVLink)
-------------------------------------------------------------------------------
What's included:
- app module with Kotlin files: MainActivity, BleManager, MavlinkHandler
- minimal UI (scan/connect button, telemetry text)
- gradle files

Notes:
- This template expects a BLE module exposing Nordic UART Service (NUS) UUIDs.
- If your module is HC-05 (classic BT), you'll need to adapt BleManager to use RFCOMM sockets.
- The project uses io.dronefleet.mavlink library; ensure you have network access to download from Maven Central.
- Open this folder in Android Studio (File -> Open) to import the project.

Quick steps:
1. Copy project to your machine or download the zip (provided).
2. Open in Android Studio.
3. Let Gradle sync and install dependencies.
4. Build & run on an Android device (minimum SDK 24).
