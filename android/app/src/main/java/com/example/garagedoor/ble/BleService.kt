package com.example.garagedoor.ble

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.garagedoor.ui.MainActivity

class BleService : Service() {

    private val binder = LocalBinder()
    lateinit var bleManager: BleManager

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    override fun onCreate() {
        super.onCreate()
        bleManager = BleManager(this)
        startForeground(1, createNotification())
    }

    private fun createNotification(): Notification {
        val channelId = "ble_service"
        val channel = NotificationChannel(channelId, "BLE Connection", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Garage Door Controller")
            .setContentText("Keeping BLE connection alive...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
