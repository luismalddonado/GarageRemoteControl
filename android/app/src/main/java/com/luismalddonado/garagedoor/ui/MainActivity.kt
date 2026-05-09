package com.luismalddonado.garagedoor.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.luismalddonado.garagedoor.R
import androidx.lifecycle.lifecycleScope
import com.luismalddonado.garagedoor.ble.BleManager
import com.luismalddonado.garagedoor.ble.ConnectionState
import com.luismalddonado.garagedoor.data.ConfigRepository
import com.luismalddonado.garagedoor.databinding.ActivityMainBinding
import android.os.Build
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.luismalddonado.garagedoor.ble.BleService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: BleManager
    private lateinit var configRepository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bleManager = BleManager.getInstance(this)
        configRepository = ConfigRepository(this)

        setupListeners()
    }

    private fun startBleService() {
        val serviceIntent = Intent(this, BleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasPermissions()) {
            startBleService()
            observeState()
            lifecycleScope.launch {
                val config = configRepository.configFlow.first()
                bleManager.startClient(config)
            }
        } else {
            requestPermissions()
        }
    }

    override fun onStop() {
        super.onStop()
        bleManager.stopClient()
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    private fun hasPermissions(): Boolean {
        return getRequiredPermissions().all { 
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
        }
    }

    private fun requestPermissions() {
        requestPermissions(getRequiredPermissions(), 101)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            startBleService()
            observeState()
            lifecycleScope.launch {
                val config = configRepository.configFlow.first()
                bleManager.startClient(config)
            }
        }
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, ConfigurationActivity::class.java))
        }

        binding.btnOpen.setOnClickListener {
            bleManager.openDoor()
        }

        binding.btnReconnect.setOnClickListener {
            bleManager.manualReconnect()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            configRepository.configFlow.collectLatest { config ->
                bleManager.startConnection(config)
            }
        }

        lifecycleScope.launch {
            bleManager.connectionState.collectLatest { state ->
                updateUi(state)
            }
        }

        lifecycleScope.launch {
            bleManager.counterValue.collectLatest { value ->
                binding.tvCounter.text = if (value != null) {
                    getString(R.string.operations_count, value)
                } else {
                    getString(R.string.operations_count, 0).replace("0", "--")
                }
            }
        }
    }

    private fun updateUi(state: ConnectionState) {
        val statusResId = when (state) {
            ConnectionState.SCANNING -> R.string.status_scanning
            ConnectionState.CONNECTING -> R.string.status_connecting
            ConnectionState.CONNECTED -> R.string.status_connected
            ConnectionState.DISCONNECTED -> R.string.status_disconnected
        }
        binding.tvStatus.setText(statusResId)
        binding.btnOpen.isEnabled = state == ConnectionState.CONNECTED
        
        // Spec: Reconnect button visible when not connected (Disconnected, Scanning, Connecting)
        binding.btnReconnect.visibility = if (state != ConnectionState.CONNECTED) View.VISIBLE else View.GONE
        
        val color = when (state) {
            ConnectionState.CONNECTED -> getColor(R.color.status_connected)
            ConnectionState.DISCONNECTED -> getColor(R.color.status_disconnected)
            else -> getColor(android.R.color.holo_orange_dark)
        }
        binding.tvStatus.setTextColor(color)
    }

    override fun onDestroy() {
        super.onDestroy()
        // We no longer call bleManager.disconnect() here to allow the foreground service to maintain the connection
    }
}
