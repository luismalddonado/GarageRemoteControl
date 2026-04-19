package com.example.garagedoor.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.garagedoor.data.BleConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

enum class ConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED
}

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _counterValue = MutableStateFlow<Int?>(null)
    val counterValue: StateFlow<Int?> = _counterValue

    private var currentConfig: BleConfig? = null
    private var retryJob: Job? = null
    private var retryCount = 0
    private val maxRetries = 5
    private val backoffDelays = listOf(1000L, 2000L, 4000L, 8000L, 16000L)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun toUuid(uuidString: String?): UUID? {
        if (uuidString == null) return null
        return if (uuidString.length == 4) {
            UUID.fromString("0000${uuidString.lowercase()}-0000-1000-8000-00805f9b34fb")
        } else {
            try {
                UUID.fromString(uuidString)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun startConnection(config: BleConfig) {
        currentConfig = config
        if (_connectionState.value != ConnectionState.DISCONNECTED) return
        
        clearRetryQueue()
        startScanning()
    }

    private fun startScanning() {
        val scanner = adapter.bluetoothLeScanner ?: return
        _connectionState.value = ConnectionState.SCANNING

        val filter = ScanFilter.Builder()
            .setServiceUuid(toUuid(currentConfig?.serviceUuid)?.let { ParcelUuid(it) })
            .setDeviceName(currentConfig?.deviceName)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
        
        // Timeout scan after 10s
        scope.launch {
            delay(10000)
            if (_connectionState.value == ConnectionState.SCANNING) {
                stopScanning()
                _connectionState.value = ConnectionState.DISCONNECTED
                handleRetry()
            }
        }
    }

    private fun handleRetry() {
        if (retryCount < maxRetries) {
            retryJob = scope.launch {
                val delayTime = backoffDelays.getOrElse(retryCount) { 16000L }
                Log.d("BleManager", "Retrying connection in ${delayTime}ms (Attempt ${retryCount + 1})")
                delay(delayTime)
                retryCount++
                startScanning()
            }
        } else {
            Log.d("BleManager", "Max retries reached")
        }
    }

    fun clearRetryQueue() {
        retryJob?.cancel()
        retryJob = null
        retryCount = 0
    }

    fun manualReconnect() {
        disconnect()
        clearRetryQueue()
        currentConfig?.let { startConnection(it) }
    }

    private fun stopScanning() {
        adapter.bluetoothLeScanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScanning()
            clearRetryQueue()
            connectToDevice(result.device)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.CONNECTED
                clearRetryQueue()
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.DISCONNECTED
                this@BleManager.gatt = null
                handleRetry()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val config = currentConfig ?: return
            val service = toUuid(config.serviceUuid)?.let { gatt.getService(it) }
            val char = toUuid(config.statusCharUuid)?.let { service?.getCharacteristic(it) }
            
            if (char != null) {
                gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicValue(characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleCharacteristicValue(characteristic)
        }
    }

    private fun handleCharacteristicValue(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.uuid == toUuid(currentConfig?.statusCharUuid)) {
            val value = characteristic.value
            if (value != null && value.size >= 4) {
                val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
                _counterValue.value = buffer.int
            }
        }
    }

    fun openDoor() {
        val config = currentConfig ?: return
        val gatt = gatt ?: return
        val service = toUuid(config.serviceUuid)?.let { gatt.getService(it) }
        val openChar = toUuid(config.openCharUuid)?.let { service?.getCharacteristic(it) }
        val statusChar = toUuid(config.statusCharUuid)?.let { service?.getCharacteristic(it) }
        
        if (openChar != null) {
            openChar.value = byteArrayOf(0x01)
            gatt.writeCharacteristic(openChar)
            
            // Refresh pending operations count after opening
            scope.launch {
                delay(500) // Small delay to let the device update state if needed
                if (statusChar != null) {
                    gatt.readCharacteristic(statusChar)
                }
            }
        }
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
