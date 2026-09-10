package com.example.demoproject.platform.mqtt

import com.example.demoproject.platform.common.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Central MQTT event surface. Broker details stay inside [MqttConnectionManager].
 */
class MqttManager {

    private val _messageFlow = MutableSharedFlow<MqttInboundMessage>(
        extraBufferCapacity = MESSAGE_BUFFER_CAPACITY,
    )
    val messageFlow: Flow<MqttInboundMessage> = _messageFlow.asSharedFlow()

    private val _connectedEvents = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val connectedEvents: Flow<Unit> = _connectedEvents.asSharedFlow()

    fun onConnected() {
        _connectedEvents.tryEmit(Unit)
    }

    fun onMessage(topic: String, payload: String, qos: Int = 0, retained: Boolean = false) {
        if (BuildConfig.ENABLE_VERBOSE_MQTT_LOG) {
            logMqttPayloadPreview(
                tag = TAG,
                messagePrefix = "incoming mqtt topic=$topic qos=$qos retained=$retained len=${payload.length}",
                payload = payload,
            )
        }
        val accepted = _messageFlow.tryEmit(
            MqttInboundMessage(
                topic = topic,
                payload = payload,
                qos = qos,
                retained = retained,
            ),
        )
        if (!accepted) {
            AppLogger.w(TAG, "drop mqtt message topic=$topic len=${payload.length}")
        }
    }

    private companion object {
        const val TAG = "MqttManager"
        const val MESSAGE_BUFFER_CAPACITY = 64
    }
}
