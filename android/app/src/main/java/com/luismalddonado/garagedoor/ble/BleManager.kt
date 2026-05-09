package com.luismalddonado.garagedoor.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.luismalddonado.garagedoor.data.BleConfig
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
class BleManager private constructor(private val context: Context) {
 
    companion object {
        @Volatile
        private var INSTANCE: BleManager? = null
        fun getInstance(context: Context): BleManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BleManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private var gatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _counterValue = MutableStateFlow<Int?>(null)
    val counterValue: StateFlow<Int?> = _counterValue

    private val _learnedCode = MutableStateFlow<String?>(null)
    val learnedCode: StateFlow<String?> = _learnedCode

    private val _isLearning = MutableStateFlow(false)
    val isLearning: StateFlow<Boolean> = _isLearning

    private var currentConfig: BleConfig? = null
    private var retryJob: Job? = null
    private var retryCount = 0
    private val maxRetries = 5
    private val backoffDelays = listOf(1000L, 2000L, 4000L, 8000L, 16000L)
    private var lastWriteTime = 0L
    private val THROTTLE_MS = 3000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionClients = 0

    fun startClient(config: BleConfig) {
        connectionClients++
        startConnection(config)
    }

    fun stopClient() {
        connectionClients--
        if (connectionClients <= 0) {
            connectionClients = 0
            // Delay disconnection to handle orientation changes or quick activity transitions
            scope.launch {
                delay(1000)
                if (connectionClients <= 0) {
                    disconnect()
                }
            }
        }
    }

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
        
        // Timeout scan after 3s as per updated spec
        scope.launch {
            delay(3000)
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
                val delayTime = backoffDelays.getOrElse(retryCount) { 60000L }
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
                retryCount = 0
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
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
            
            // Subscribe to Status updates
            val statusChar = toUuid(config.statusCharUuid)?.let { service?.getCharacteristic(it) }
            if (statusChar != null) {
                gatt.setCharacteristicNotification(statusChar, true)
                val descriptor = statusChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }

            // Subscribe to Learn Code updates
            val learnReturnChar = toUuid(config.returnLearnCodeCharUuid)?.let { service?.getCharacteristic(it) }
            if (learnReturnChar != null) {
                gatt.setCharacteristicNotification(learnReturnChar, true)
                val descriptor = learnReturnChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
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
        val config = currentConfig ?: return
        if (characteristic.uuid == toUuid(config.statusCharUuid)) {
            val value = characteristic.value
            if (value != null && value.size >= 4) {
                val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
                _counterValue.value = buffer.int
            }
        } else if (characteristic.uuid == toUuid(config.returnLearnCodeCharUuid)) {
            val value = characteristic.value
            if (value != null && value.isNotEmpty()) {
                val buffer = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
                val codes = mutableListOf<Long>() // Use Long to handle unsigned if needed, or Int
                while (buffer.remaining() >= 4) {
                    codes.add(buffer.int.toLong())
                }
                _learnedCode.value = codes.joinToString(", ")
            }
        }
    }

    fun readLearnedCode() {
        val config = currentConfig ?: return
        val gatt = gatt ?: return
        val service = toUuid(config.serviceUuid)?.let { gatt.getService(it) }
        val char = toUuid(config.returnLearnCodeCharUuid)?.let { service?.getCharacteristic(it) }
        
        if (char != null) {
            gatt.readCharacteristic(char)
        }
    }

    fun openDoor() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastWriteTime < THROTTLE_MS) {
            Log.d("BleManager", "Throttling open command")
            return
        }
        
        val config = currentConfig ?: return
        val gatt = gatt ?: return
        val service = toUuid(config.serviceUuid)?.let { gatt.getService(it) }
        val openChar = toUuid(config.openCharUuid)?.let { service?.getCharacteristic(it) }
        val statusChar = toUuid(config.statusCharUuid)?.let { service?.getCharacteristic(it) }
        
        if (openChar != null) {
            lastWriteTime = currentTime
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

    fun startLearning() {
        if (_isLearning.value) return
        
        _learnedCode.value = null // Clear previous code
        writeCommand(currentConfig?.learnCodeStartCharUuid)
        _isLearning.value = true
        
        // Spec: Automatically stop after 10 seconds
        scope.launch {
            delay(10000)
            stopLearning()
        }
    }

    fun stopLearning() {
        writeCommand(currentConfig?.learnCodeStopCharUuid)
        _isLearning.value = false
        
        // Refresh learned code after stopping
        scope.launch {
            delay(500)
            readLearnedCode()
        }
    }

    private fun writeCommand(charUuid: String?) {
        val config = currentConfig ?: return
        val gatt = gatt ?: return
        val service = toUuid(config.serviceUuid)?.let { gatt.getService(it) }
        val char = toUuid(charUuid)?.let { service?.getCharacteristic(it) }
        
        if (char != null) {
            char.value = byteArrayOf(0x01)
            gatt.writeCharacteristic(char)
        }
    }
}
