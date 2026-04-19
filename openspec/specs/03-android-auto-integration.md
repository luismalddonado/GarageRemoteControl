# Spec: Android Auto Integration

## Overview
This specification covers the driver-focused interface for Android Auto, designed to be used safely while operating a vehicle.

## Project Configuration
- **Library**: `androidx.car.app:app:1.7.0`
- **Category**: `androidx.car.app.category.PARKING` (Ensures the app is available when the car is in a safe state).
- **Permissions**: Must handle `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions gracefully within the Car App lifecycle.

## User Interface
### 1. Template Choice
- Use the `PaneTemplate` or `ActionStrip` to provide a clean, distraction-free UI.
- **Main Action**: A single, prominent button labeled "Open Garage Door".
- **Status Display**: A small text field showing the connection status and the latest counter value.

### 2. Branding and Theme
- Apply `CarAppTheme` to ensure consistency with the vehicle's head unit.
- Use high-contrast icons that are easily recognizable at a glance.

## Logic and Lifecycle
- **Lifecycle Management**: The Android Auto session should bind to the same background BLE service as the mobile app to ensure consistent state.
- **Safety Restrictions**: The "Open" command must be disabled if the car reports a high speed (if data is available) or based on the `PARKING` category restrictions.
- **Responsiveness**: The UI must adapt to various screen sizes (standard, wide, and portrait head units).

## Testing Requirements
- Must be validated using the **Android Auto Desktop Head Unit (DHU)**.
- Test scenarios:
    - Connection while driving.
    - Behavior when the phone is locked.
    - Behavior when the car's ignition is turned off mid-operation.
