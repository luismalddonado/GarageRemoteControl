# Spec: Android Application UI

## Overview
The Android application consists of two main activities: the `MainActivity` for daily operation and a `ConfigurationActivity` for setup.

## 1. MainActivity
### Layout Elements
- **Title**: "Garage Door Controller"
- **Status Indicator**: A text area displaying:
    - Current connection state (Scanning, Connecting, Connected, Disconnected).
    - Value of the `repeat_counter_read` characteristic (e.g., "Pending operations: 124").
    - When the "Open Door" button is pressed, the pending operations count is refreshed (requests a read from the BLE device).
- **Action Buttons**:
    - Large "Open Door" button (triggers write command).
    - "Reconnect" button, close to the status indicator, only visible when disconnected(triggers manual scan/connection attempt).
- **Settings Icon**: Top-right corner to navigate to the `ConfigurationActivity`.

### Interaction Logic
- Use **Kotlin Coroutines** to handle the BLE lifecycle.
- Update the UI reactively when notifications are received from the `repeat_counter_read` characteristic.
- Implement a "Pull to Refresh" or similar mechanism to restart scanning if the device is lost.

## 2. Configuration Activity
### Purpose
Allows the user to override default BLE settings to support different hardware configurations.

### Configurable Fields
- **Device Name**: String (Default: "puertagaraje").
- **Service UUID**: String (Default: `180F`).
- **Open Characteristic UUID**: String (Default: `1801`).
- **Status Characteristic UUID**: String (Default: `1802`).

### Persistence
- Settings must be persisted using `SharedPreferences` or `DataStore`.
- A "Reset to Defaults" button should be provided.

## 3. Visual Style
- **Theme**: Material Design 3.
- **Colors**: High contrast for visibility (e.g., dark blue/green for "Open Door").
- **Accessibility**: Ensure all buttons have content descriptions for screen readers.
