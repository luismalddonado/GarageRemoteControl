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
    - "Reconnect" button, close to the status indicator, only visible when disconnected
    - "Reconnect" button to do move around while the status indicator is scanning or connecting
    (triggers manual scan/connection attempt).
- **Settings Icon**: Top-right corner to navigate to the `ConfigurationActivity`.

### Interaction Logic
- Use **Kotlin Coroutines** to handle the BLE lifecycle.
- Update the UI reactively when notifications are received from the `repeat_counter_read` characteristic.
- Implement a "Pull to Refresh" or similar mechanism to restart scanning if the device is lost.

## 2. Configuration Activity
### Purpose
Allows the user to override default BLE settings to support different hardware configurations.

### Configurable Fields
- **Device Name**: String (Default: "puertagaraje"). Not editable, just for display. Show it on top of the screen.
- **Action Buttons**:
    - Button  "Start learning new code" button (triggers write command 1803). Enabled only when the device is connected. When pressed the button text should change to "Learning new code..." and stay disabled for 10 seconds.
    - Button "Stop learning new code" button (triggers write command 1804). By default this button is disabled. Once "Start learning new code" button is pressed, this button should be enabled. This button should be triggered automatically 10 seconds after Start Learning process is triggered. This action should have no visual feedback.
    - Text area "Current code" to display the current learned code (triggers read command 1805). Because only the first characters of the code are displayed, display 3 dots "..." after the current code. When the "Start learning new code" button is pressed, the area displays dots which should be animated until the code is read. After the code is read, the three dots should be replaced by the code.

### Persistence
- Settings must be persisted using `SharedPreferences` or `DataStore`.
- A "Reset to Defaults" button should be provided.

## 3. Visual Style
- **Theme**: Material Design 3.
- **Colors**: High contrast for visibility (e.g., dark blue/green for "Open Door").
- **Accessibility**: Ensure all buttons have content descriptions for screen readers.
