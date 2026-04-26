# Garage Remote Control

A comprehensive solution for controlling a garage door via Bluetooth Low Energy (BLE), featuring an Android application with full Android Auto integration and background persistence.

## Key Features
- **BLE Communication**: Secure and fast control using standard BLE services.
- **Android Auto**: Full integration with Android Auto (IOT category) for control directly from your vehicle's dashboard.
- **Persistent Connection**: Uses an Android Foreground Service to maintain a stable connection even when the app is in the background.
- **Auto-Reconnect**: Automatically attempts to reconnect to the garage door when in range.
- **Learning Mode**: Interface to manage and retrieve learned codes from the hardware.

## Technical Specifications
The app is built with modern Android standards:
- **Language**: Kotlin
- **Architecture**: MVVM with StateFlow and Coroutines
- **UI**: Material 3 Design
- **Android Auto API**: `androidx.car.app:app:1.7.0`
- **Minimum SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

## Quick Start
1. Clone the repository.
2. Open the `android` folder in Android Studio.
3. Configure your ESPHome/BLE device with the following parameters (defaults):
   - **Device Name**: `puertagaraje`
   - **Service UUID**: `180F`
   - **Open Characteristic**: `1801`
   - **Status Characteristic**: `1802`
   - **Learn Start**: `1803`
   - **Learn Stop**: `1804`
   - **Return Code**: `1805`

## Android 14 Requirements
This app includes necessary configurations for Android 14+:
- `FOREGROUND_SERVICE_CONNECTED_DEVICE` permission.
- Foreground Service Type declaration in the manifest.
- Post-notification permissions for the persistent connection status.

## Documentation
Detailed specifications can be found in the `/openspec/specs` directory:
1. [BLE Communication](openspec/specs/01-ble-communication.md)
2. [Android App UI](openspec/specs/02-android-app-ui.md)
3. [Android Auto Integration](openspec/specs/03-android-auto-integration.md)
