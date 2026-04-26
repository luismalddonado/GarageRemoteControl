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
    val statusCharUuid: String = "1802",
    val learnCodeStartCharUuid: String = "1803",
    val learnCodeStopCharUuid: String = "1804",
    val returnLearnCodeCharUuid: String = "1805"
)

class ConfigRepository(private val context: Context) {
    private val DEVICE_NAME = stringPreferencesKey("device_name")
    private val SERVICE_UUID = stringPreferencesKey("service_uuid")
    private val OPEN_CHAR_UUID = stringPreferencesKey("open_char_uuid")
    private val STATUS_CHAR_UUID = stringPreferencesKey("status_char_uuid")
    private val LEARN_START_UUID = stringPreferencesKey("learn_start_uuid")
    private val LEARN_STOP_UUID = stringPreferencesKey("learn_stop_uuid")
    private val LEARN_RETURN_UUID = stringPreferencesKey("learn_return_uuid")

    val configFlow: Flow<BleConfig> = context.dataStore.data.map { preferences ->
        BleConfig(
            deviceName = preferences[DEVICE_NAME] ?: "puertagaraje",
            serviceUuid = preferences[SERVICE_UUID] ?: "180F",
            openCharUuid = preferences[OPEN_CHAR_UUID] ?: "1801",
            statusCharUuid = preferences[STATUS_CHAR_UUID] ?: "1802",
            learnCodeStartCharUuid = preferences[LEARN_START_UUID] ?: "1803",
            learnCodeStopCharUuid = preferences[LEARN_STOP_UUID] ?: "1804",
            returnLearnCodeCharUuid = preferences[LEARN_RETURN_UUID] ?: "1805"
        )
    }

    suspend fun saveConfig(config: BleConfig) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_NAME] = config.deviceName
            preferences[SERVICE_UUID] = config.serviceUuid
            preferences[OPEN_CHAR_UUID] = config.openCharUuid
            preferences[STATUS_CHAR_UUID] = config.statusCharUuid
            preferences[LEARN_START_UUID] = config.learnCodeStartCharUuid
            preferences[LEARN_STOP_UUID] = config.learnCodeStopCharUuid
            preferences[LEARN_RETURN_UUID] = config.returnLearnCodeCharUuid
        }
    }

    suspend fun resetConfig() {
        context.dataStore.edit { it.clear() }
    }
}
