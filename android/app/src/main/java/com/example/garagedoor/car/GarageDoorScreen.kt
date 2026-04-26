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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class GarageDoorScreen(carContext: CarContext) : Screen(carContext) {
 
    private val bleManager = BleManager.getInstance(carContext)
    private val configRepository = ConfigRepository(carContext)
    private var lastCounter: Int? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (hasPermissions()) {
                    startBle()
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                bleManager.stopClient()
            }
        })
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
            bleManager.startClient(config)
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
        val statusResId = when(state) {
            ConnectionState.CONNECTED -> R.string.status_connected
            ConnectionState.DISCONNECTED -> R.string.status_disconnected
            ConnectionState.SCANNING -> R.string.status_scanning
            ConnectionState.CONNECTING -> R.string.status_connecting
        }
        val statusName = carContext.getString(statusResId)
        val statusText = carContext.getString(R.string.status_display, statusName, lastCounter?.toString() ?: "--")

        val action = Action.Builder()
            .setTitle(carContext.getString(R.string.open_door))
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
            .setTitle(carContext.getString(R.string.main_title))
            .build()
    }
}
