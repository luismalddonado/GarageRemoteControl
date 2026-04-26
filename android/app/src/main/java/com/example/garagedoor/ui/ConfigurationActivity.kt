package com.example.garagedoor.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.garagedoor.data.BleConfig
import com.example.garagedoor.data.ConfigRepository
import com.example.garagedoor.databinding.ActivityConfigurationBinding
import com.example.garagedoor.ble.BleManager
import com.example.garagedoor.ble.ConnectionState
import com.example.garagedoor.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigurationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurationBinding
    private lateinit var configRepository: ConfigRepository
    private lateinit var bleManager: BleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configRepository = ConfigRepository(this)
        bleManager = BleManager.getInstance(this)

        loadCurrentConfig()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val config = configRepository.configFlow.first()
            bleManager.startClient(config)
            
            // Trigger a manual read of the current code
            bleManager.readLearnedCode()
        }
        observeLearnedCode()
        observeState()
    }

    override fun onStop() {
        super.onStop()
        bleManager.stopClient()
    }

    private fun observeState() {
        lifecycleScope.launch {
            // Combine connection state and learning state to manage button interactivity
            bleManager.connectionState.collect { state ->
                updateButtons(state, bleManager.isLearning.value)
            }
        }
        
        lifecycleScope.launch {
            bleManager.isLearning.collect { isLearning ->
                updateButtons(bleManager.connectionState.value, isLearning)
            }
        }
    }

    private fun updateButtons(state: ConnectionState, isLearning: Boolean) {
        val isConnected = state == ConnectionState.CONNECTED
        
        if (isLearning) {
            binding.btnStartLearning.setText(R.string.btn_learning_active)
            binding.btnStartLearning.isEnabled = false
            binding.btnStopLearning.isEnabled = true
            if (bleManager.learnedCode.value == null) {
                startDotsAnimation()
            }
        } else {
            binding.btnStartLearning.setText(R.string.btn_start_learning)
            binding.btnStartLearning.isEnabled = isConnected
            binding.btnStopLearning.isEnabled = false
            if (bleManager.learnedCode.value == null) {
                stopDotsAnimation()
                binding.tvCurrentCode.text = getString(R.string.current_code_label, "--")
            }
        }
    }

    private var animationJob: Job? = null

    private fun observeLearnedCode() {
        lifecycleScope.launch {
            bleManager.learnedCode.collect { code ->
                if (code != null) {
                    stopDotsAnimation()
                    binding.tvCurrentCode.text = getString(R.string.current_code_label, "$code ...")
                } else if (bleManager.isLearning.value) {
                    startDotsAnimation()
                } else {
                    stopDotsAnimation()
                    binding.tvCurrentCode.text = getString(R.string.current_code_label, "--")
                }
            }
        }
    }

    private fun startDotsAnimation() {
        if (animationJob?.isActive == true) return
        animationJob = lifecycleScope.launch {
            var dots = 0
            while (true) {
                val dotsStr = ".".repeat(dots + 1)
                binding.tvCurrentCode.text = getString(R.string.current_code_label, dotsStr)
                dots = (dots + 1) % 3
                delay(500)
            }
        }
    }

    private fun stopDotsAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    private fun loadCurrentConfig() {
        lifecycleScope.launch {
            val config = configRepository.configFlow.first()
            binding.etDeviceName.setText(config.deviceName)
            
            // Trigger a manual read of the current code
            bleManager.readLearnedCode()
        }
    }

    private fun setupListeners() {
        binding.btnStartLearning.setOnClickListener {
            bleManager.startLearning()
        }

        binding.btnStopLearning.setOnClickListener {
            bleManager.stopLearning()
        }
    }
}
