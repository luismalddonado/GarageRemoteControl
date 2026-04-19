# Spec: BLE Communication Service

## Overview
This specification defines the Bluetooth Low Energy (BLE) interaction between the Android application and the ESPHome-powered garage door controller.

## Device Identification
- **Default Device Name**: `puertagaraje`
- **Default Service UUID**: `180F`

## Characteristics

### 1. Open Door (`open_the_door`)
- **UUID**: `1801`
- **Properties**: `WRITE`, `NOTIFY`
- **Function**: Triggering this characteristic initiates the door opening sequence.
- **Data Format**: 1-byte command (e.g., `0x01` to open).

### 2. Repeat Counter / Status (`repeat_counter_read`)
- **UUID**: `1802`
- **Properties**: `READ`, `NOTIFY`
- **Function**: Provides real-time feedback on the device status and operation count.
- **Data Format**: 4-byte little-endian integer.
- **Decoding**: Must be decoded as a little-endian unsigned integer to display the counter value.

## Connection Flow
1. **Scanning**:
   - Filter by Service UUID or Device Name (`puertagaraje`).
   - Implement a timeout for scanning (e.g., 10 seconds).
2. **Connection**:
   - Establish connection using Android BLE APIs.
   - Use `connectGatt` with `autoConnect = false` for initial pairing.
3. **Service Discovery**:
   - Discover services and verify characteristic UUIDs are present.
4. **Subscription**:
   - Subscribe to notifications for `repeat_counter_read` to receive status updates without polling.

## Reliability and Safety
- **Automatic Reconnection**: If the connection drops, the application must automatically attempt to reconnect to the device.
- **Retry Mechanism**: Use an exponential backoff strategy (1s, 2s, 4s, 8s) for up to 5 attempts.
- **State Resumption**: Once reconnected, the app must automatically re-discover services and resume monitoring the `repeat_counter_read` characteristic notifications.
- **Throttling**: The "Open Door" command must be throttled/debounced (minimum 3 seconds between successive writes) to prevent motor strain.
- **Background Handling**: BLE operations must run in a background thread using Kotlin Coroutines (e.g., `Dispatchers.IO`).
- **Timeout**: Each write operation should have a 5-second timeout.

## Error Handling
- **Device Not Found**: Display "Garage Door not found. Check power and proximity."
- **Connection Lost**: Display "Connection lost. Reconnecting..."
- **Write Failure**: Alert the user if the `open_the_door` command fails to reach the device.
