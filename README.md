# Garage Remote Control

A comprehensive solution for controlling a garage door via Bluetooth Low Energy (BLE), featuring an Android application with full Android Auto integration.

## Repository
- **URL**: [https://github.com/luismalddonado/GarageRemoteControl](https://github.com/luismalddonado/GarageRemoteControl)
- **Git Remote**: `git@github.com:luismalddonado/GarageRemoteControl.git`

## Project Structure
- **/android**: The Android Studio project (Kotlin).
- **/openspec**: Project specifications and requirements.

## Quick Start
1. Clone the repository.
2. Open the `android` folder in Android Studio.
3. Configure your ESPHome device with the following parameters:
   - **Device Name**: `puertagaraje`
   - **Service UUID**: `180F`
   - **Open Characteristic**: `1801`
   - **Status Characteristic**: `1802`

## Documentation
Detailed specifications can be found in the `/openspec/specs` directory:
1. [BLE Communication](openspec/specs/01-ble-communication.md)
2. [Android App UI](openspec/specs/02-android-app-ui.md)
3. [Android Auto Integration](openspec/specs/03-android-auto-integration.md)
