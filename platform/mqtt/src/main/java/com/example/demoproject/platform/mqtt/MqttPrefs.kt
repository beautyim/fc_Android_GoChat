package com.example.demoproject.platform.mqtt

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.mqttDataStore by preferencesDataStore(name = "mqtt_conn")

/** Persisted MQTT broker credentials from `/app/init` (`socket_info`). */
class MqttPrefs(
    context: Context,
) {
    private val appContext = context.applicationContext

    private object Keys {
        val MQTT_HOST = stringPreferencesKey("mqtt_host")
        val TCP_PORT = stringPreferencesKey("tcp_port")
        val CLIENT_ID = stringPreferencesKey("client_id")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val SUBSCRIPTIONS = stringPreferencesKey("subscriptions")
    }

    val mqttConfigFlow: Flow<MqttConfig?> = appContext.mqttDataStore.data.map { prefs ->
        readConfig(prefs)
    }.distinctUntilChanged()

    suspend fun currentConfig(): MqttConfig? = mqttConfigFlow.first()

    suspend fun save(config: MqttConfig) {
        appContext.mqttDataStore.edit { prefs ->
            prefs[Keys.MQTT_HOST] = config.mqttHost
            prefs[Keys.TCP_PORT] = config.tcpPort.toString()
            prefs[Keys.CLIENT_ID] = config.clientId
            prefs[Keys.USERNAME] = config.username
            prefs[Keys.PASSWORD] = config.password
            prefs[Keys.SUBSCRIPTIONS] = config.subscriptions.joinToString(separator = ",")
        }
    }

    suspend fun clear() {
        appContext.mqttDataStore.edit { it.clear() }
    }

    private fun readConfig(prefs: Preferences): MqttConfig? {
        val host = prefs[Keys.MQTT_HOST]?.takeIf { it.isNotBlank() } ?: return null
        val portStr = prefs[Keys.TCP_PORT]?.takeIf { it.isNotBlank() }
        val clientId = prefs[Keys.CLIENT_ID]?.takeIf { it.isNotBlank() } ?: return null
        val port = portStr?.toIntOrNull() ?: 0
        val username = prefs[Keys.USERNAME].orEmpty()
        val password = prefs[Keys.PASSWORD].orEmpty()
        val subscriptions = prefs[Keys.SUBSCRIPTIONS]
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return MqttConfig(
            mqttHost = host,
            tcpPort = port,
            clientId = clientId,
            username = username,
            password = password,
            subscriptions = subscriptions,
        )
    }
}
