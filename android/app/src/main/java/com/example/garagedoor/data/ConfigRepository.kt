package com.example.garagedoor.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

data class BleConfig(
    val deviceName: String = "puertagaraje",
    val serviceUuid: String = "180F",
    val openCharUuid: String = "1801",
    val statusCharUuid: String = "1802"
)

class ConfigRepository(private val context: Context) {
    private val DEVICE_NAME = stringPreferencesKey("device_name")
    private val SERVICE_UUID = stringPreferencesKey("service_uuid")
    private val OPEN_CHAR_UUID = stringPreferencesKey("open_char_uuid")
    private val STATUS_CHAR_UUID = stringPreferencesKey("status_char_uuid")

    val configFlow: Flow<BleConfig> = context.dataStore.data.map { preferences ->
        BleConfig(
            deviceName = preferences[DEVICE_NAME] ?: "puertagaraje",
            serviceUuid = preferences[SERVICE_UUID] ?: "180F",
            openCharUuid = preferences[OPEN_CHAR_UUID] ?: "1801",
            statusCharUuid = preferences[STATUS_CHAR_UUID] ?: "1802"
        )
    }

    suspend fun saveConfig(config: BleConfig) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_NAME] = config.deviceName
            preferences[SERVICE_UUID] = config.serviceUuid
            preferences[OPEN_CHAR_UUID] = config.openCharUuid
            preferences[STATUS_CHAR_UUID] = config.statusCharUuid
        }
    }

    suspend fun resetConfig() {
        context.dataStore.edit { it.clear() }
    }
}
