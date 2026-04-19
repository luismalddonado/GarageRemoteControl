# Garage Door Android Project

This is a complete Android Studio project for controlling a garage door via BLE, with Android Auto integration.

## Features
- **BLE Communication**: Connects to ESPHome-based garage door controllers.
- **Mobile UI**: Clean Material 3 interface with a large "Open Door" button and status feedback.
- **Configuration**: Easily change Service and Characteristic UUIDs.
- **Android Auto**: Driver-focused interface for safe operation while driving.
- **Reactive UI**: Built with Kotlin Coroutines and StateFlow for real-time updates.

## Setup Instructions
1. Open the `android/` directory in **Android Studio** (Hedgehog or newer recommended).
2. Sync the project with Gradle files.
3. Ensure you have the necessary permissions granted on the device (Bluetooth and Location).
4. Connect your phone to a car or use the **Android Auto Desktop Head Unit (DHU)** to test the car integration.

## BLE Specifications
- **Default Device Name**: `puertagaraje`
- **Service UUID**: `180F`
- **Open Characteristic**: `1801` (Write)
- **Status Characteristic**: `1802` (Read/Notify)

## Project Structure
- `app/src/main/java/com/example/garagedoor/ble`: BLE logic and manager.
- `app/src/main/java/com/example/garagedoor/ui`: Mobile activities.
- `app/src/main/java/com/example/garagedoor/car`: Android Auto integration.
- `app/src/main/java/com/example/garagedoor/data`: Settings persistence via DataStore.
