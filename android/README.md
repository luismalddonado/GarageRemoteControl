# Garage Door Android Project

This is a complete Android Studio project for controlling a garage door via BLE, featuring high-reliability connection management and full Android Auto integration.

## Key Features
- **Reliable BLE Connectivity**: Custom connection manager with automatic retry and orientation-change resilience.
- **Background Persistence**: Foreground Service ensures the connection stays alive for immediate response via phone or car.
- **Android Auto Integration**: Dedicated "IOT" category service for controlling your garage from the car's infotainment system.
- **Material 3 UI**: Modern, accessible interface with real-time status feedback.
- **Learning Mode**: Special configuration screen to trigger and capture learned codes from the hardware.

## Technical Architecture
- **BleManager**: A thread-safe singleton managing the GATT connection, client tracking, and command throttling.
- **BleService**: A Foreground Service that acts as a persistent client, keeping the connection active in the background.
- **DataStore**: Modern reactive settings storage for device and characteristic configuration.
- **CarAppService**: Integration with `androidx.car.app` for the automotive experience.

## Setup & Testing
1. **Permissions**: The app requests Bluetooth Scan/Connect, Location, and Post Notifications.
2. **Foreground Service**: On Android 14+, the `FOREGROUND_SERVICE_CONNECTED_DEVICE` permission is used.
3. **Testing Android Auto**: Use the **Desktop Head Unit (DHU)**. The app is registered as an IOT category app.
4. **Boot Persistence**: The `BootReceiver` ensures the service restarts automatically after a device reboot.

## BLE Profile
| Characteristic | UUID | Property |
| :--- | :--- | :--- |
| Service | `180F` | - |
| Open Door | `1801` | Write |
| Status/Counter | `1802` | Notify |
| Start Learning | `1803` | Write |
| Stop Learning | `1804` | Write |
| Return Code | `1805` | Notify |

## Development Requirements
- Android Studio Iguana+
- Kotlin 1.9+
- Android SDK 34 (Target)
