package com.luismalddonado.garagedoor.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.luismalddonado.garagedoor.R
import com.luismalddonado.garagedoor.ble.BleManager
import com.luismalddonado.garagedoor.ble.ConnectionState
import com.luismalddonado.garagedoor.data.ConfigRepository
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
        if (!hasPermissions()) {
            return MessageTemplate.Builder(carContext.getString(R.string.grant_permissions_on_phone))
                .setTitle(carContext.getString(R.string.main_title))
                .setHeaderAction(Action.APP_ICON)
                .build()
        }

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

        val paneBuilder = Pane.Builder()
            .addAction(action)

        if (state != ConnectionState.CONNECTED) {
            val reconnectAction = Action.Builder()
                .setTitle(carContext.getString(R.string.btn_reconnect))
                .setOnClickListener { bleManager.manualReconnect() }
                .build()
            paneBuilder.addAction(reconnectAction)
        }

        val pane = paneBuilder
            .addRow(Row.Builder().setTitle(statusText).build())
            .build()

        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .setTitle(carContext.getString(R.string.main_title))
            .build()
    }
}
