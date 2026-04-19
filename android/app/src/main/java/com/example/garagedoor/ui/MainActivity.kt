package com.example.garagedoor.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.garagedoor.R
import androidx.lifecycle.lifecycleScope
import com.example.garagedoor.ble.BleManager
import com.example.garagedoor.ble.ConnectionState
import com.example.garagedoor.data.ConfigRepository
import com.example.garagedoor.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleManager: BleManager
    private lateinit var configRepository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bleManager = BleManager(this)
        configRepository = ConfigRepository(this)

        setupListeners()
        
        if (hasPermissions()) {
            observeState()
        } else {
            requestPermissions()
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        return permissions.all { 
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
        }
    }

    private fun requestPermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        requestPermissions(permissions, 101)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            observeState()
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
                    "Operaciones pendientes: --"
                }
            }
        }
    }

    private fun updateUi(state: ConnectionState) {
        binding.tvStatus.text = state.name
        binding.btnOpen.isEnabled = state == ConnectionState.CONNECTED
        binding.btnReconnect.visibility = if (state == ConnectionState.DISCONNECTED) View.VISIBLE else View.GONE
        
        val color = when (state) {
            ConnectionState.CONNECTED -> getColor(R.color.status_connected)
            ConnectionState.DISCONNECTED -> getColor(R.color.status_disconnected)
            else -> getColor(android.R.color.holo_orange_dark)
        }
        binding.tvStatus.setTextColor(color)
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.disconnect()
    }
}
