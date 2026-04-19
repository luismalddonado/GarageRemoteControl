package com.example.garagedoor.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.example.garagedoor.R
import com.example.garagedoor.ble.BleManager
import com.example.garagedoor.ble.ConnectionState
import com.example.garagedoor.data.ConfigRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GarageDoorScreen(carContext: CarContext) : Screen(carContext) {

    private val bleManager = BleManager(carContext)
    private val configRepository = ConfigRepository(carContext)
    private var lastCounter: Int? = null

    init {
        if (hasPermissions()) {
            startBle()
        }
        
        observeState()
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            listOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all { carContext.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
    }

    private fun startBle() {
        lifecycleScope.launch {
            val config = configRepository.configFlow.first()
            bleManager.startConnection(config)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            bleManager.connectionState.collectLatest { invalidate() }
        }

        lifecycleScope.launch {
            bleManager.counterValue.collectLatest { 
                lastCounter = it
                invalidate() 
            }
        }
    }

    override fun onGetTemplate(): Template {
        val state = bleManager.connectionState.value
        val statusName = when(state) {
            ConnectionState.CONNECTED -> "Conectado"
            ConnectionState.DISCONNECTED -> "Desconectado"
            ConnectionState.SCANNING -> "Escaneando"
            ConnectionState.CONNECTING -> "Conectando"
        }
        val statusText = "Estado: $statusName | Pendientes: ${lastCounter ?: "--"}"

        val action = Action.Builder()
            .setTitle("Abrir Garaje")
            .setBackgroundColor(CarColor.GREEN)
            .setOnClickListener { bleManager.openDoor() }
            .setEnabled(state == ConnectionState.CONNECTED)
            .build()

        val pane = Pane.Builder()
            .addAction(action)
            .addRow(Row.Builder().setTitle(statusText).build())
            .build()

        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .setTitle("Puerta Garaje")
            .build()
    }
}
