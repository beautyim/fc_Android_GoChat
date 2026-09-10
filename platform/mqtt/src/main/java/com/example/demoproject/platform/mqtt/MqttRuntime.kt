package com.example.demoproject.platform.mqtt

import android.content.Context

/**
 * Manual MQTT graph (same pattern as NetworkRuntime).
 * CallKit / signaling client omitted until `:platform:callkit` exists.
 */
class MqttRuntime private constructor(
    val prefs: MqttPrefs,
    val manager: MqttManager,
    val connectionManager: MqttConnectionManager,
) {
    suspend fun saveConfig(config: MqttConfig) {
        prefs.save(config)
    }

    suspend fun clear() {
        prefs.clear()
    }

    companion object {
        @Volatile
        private var instance: MqttRuntime? = null

        fun get(context: Context): MqttRuntime {
            return instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }
        }

        fun create(context: Context): MqttRuntime {
            val prefs = MqttPrefs(context)
            val manager = MqttManager()
            val connectionManager = MqttConnectionManager(prefs, manager)
            return MqttRuntime(prefs, manager, connectionManager)
        }
    }
}
