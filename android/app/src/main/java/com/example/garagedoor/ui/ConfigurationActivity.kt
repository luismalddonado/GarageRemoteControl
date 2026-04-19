package com.example.garagedoor.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.garagedoor.data.BleConfig
import com.example.garagedoor.data.ConfigRepository
import com.example.garagedoor.databinding.ActivityConfigurationBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var configRepository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configRepository = ConfigRepository(this)

        loadCurrentConfig()
        setupListeners()
    }

    private fun loadCurrentConfig() {
        lifecycleScope.launch {
            val config = configRepository.configFlow.first()
            binding.etDeviceName.setText(config.deviceName)
            binding.etServiceUuid.setText(config.serviceUuid)
            binding.etOpenUuid.setText(config.openCharUuid)
            binding.etStatusUuid.setText(config.statusCharUuid)
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val config = BleConfig(
                deviceName = binding.etDeviceName.text.toString(),
                serviceUuid = binding.etServiceUuid.text.toString(),
                openCharUuid = binding.etOpenUuid.text.toString(),
                statusCharUuid = binding.etStatusUuid.text.toString()
            )
            lifecycleScope.launch {
                configRepository.saveConfig(config)
                finish()
            }
        }

        binding.btnReset.setOnClickListener {
            lifecycleScope.launch {
                configRepository.resetConfig()
                loadCurrentConfig()
            }
        }
    }
}
