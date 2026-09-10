package com.example.demoproject.platform.mqtt

import com.example.demoproject.platform.common.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttConnectionManager(
    private val prefs: MqttPrefs,
    private val mqttManager: MqttManager,
) {
    private val connectionMutex = Mutex()

    @Volatile private var client: MqttClient? = null
    @Volatile private var latestConfig: MqttConfig? = null
    private var observeJob: Job? = null
    private var reconnectJob: Job? = null
    private var appScope: CoroutineScope? = null

    fun start(scope: CoroutineScope) {
        if (observeJob != null) return
        appScope = scope
        observeJob = scope.launch(Dispatchers.IO) {
            prefs.mqttConfigFlow.collectLatest { config ->
                latestConfig = config
                reconnectJob?.cancel()
                if (config == null) {
                    disconnectCurrent("config cleared")
                } else {
                    connect(config)
                }
            }
        }
    }

    fun stop() {
        reconnectJob?.cancel()
        observeJob?.cancel()
        reconnectJob = null
        observeJob = null
        appScope = null
        latestConfig = null
        val oldClient = client
        client = null
        oldClient?.safeDisconnect("stop")
    }

    private suspend fun connect(config: MqttConfig) {
        if (config.mqttHost.isBlank() || config.clientId.isBlank()) {
            AppLogger.w(TAG, "skip mqtt connect: missing host or clientId")
            disconnectCurrent("invalid config")
            return
        }
        connectionMutex.withLock {
            disconnectCurrentLocked("reconnect")
            withContext(Dispatchers.IO) {
                var nextClient: MqttClient? = null
                runCatching {
                    nextClient = MqttClient(config.brokerUri, config.clientId, MemoryPersistence())
                    nextClient?.setCallback(callbackFor(config))
                    nextClient?.connect(connectOptions(config))
                    client = nextClient
                    val topics = subscriptionTopics(config)
                    if (topics.isNotEmpty()) {
                        nextClient?.subscribe(
                            topics.toTypedArray(),
                            IntArray(topics.size) { MQTT_QOS },
                        )
                        AppLogger.i(TAG, "mqtt subscribed count=${topics.size}")
                    } else {
                        AppLogger.w(TAG, "mqtt connected without explicit subscriptions")
                    }
                    AppLogger.i(TAG, "mqtt connected uri=${config.brokerUri.redactUri()}")
                    mqttManager.onConnected()
                }.onFailure { error ->
                    client = null
                    nextClient?.safeDisconnect("connect failed")
                    AppLogger.w(TAG, "mqtt connect failed uri=${config.brokerUri.redactUri()}", error)
                    scheduleReconnect(config)
                }
            }
        }
    }

    private fun callbackFor(config: MqttConfig): MqttCallback =
        object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                AppLogger.w(TAG, "mqtt connection lost", cause)
                scheduleReconnect(config)
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = message.payload?.toString(Charsets.UTF_8).orEmpty()
                mqttManager.onMessage(
                    topic = topic,
                    payload = payload,
                    qos = message.qos,
                    retained = message.isRetained,
                )
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        }

    private fun connectOptions(config: MqttConfig): MqttConnectOptions =
        MqttConnectOptions().apply {
            isCleanSession = false
            isAutomaticReconnect = false
            keepAliveInterval = KEEP_ALIVE_SECONDS
            connectionTimeout = CONNECTION_TIMEOUT_SECONDS
            if (config.username.isNotBlank()) {
                userName = config.username
            }
            if (config.password.isNotBlank()) {
                password = config.password.toCharArray()
            }
        }

    private fun scheduleReconnect(config: MqttConfig) {
        val scope = appScope ?: return
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(RECONNECT_DELAY_MS)
            if (latestConfig == config) {
                connect(config)
            }
        }
    }

    private suspend fun disconnectCurrent(reason: String) {
        connectionMutex.withLock {
            disconnectCurrentLocked(reason)
        }
    }

    private fun disconnectCurrentLocked(reason: String) {
        val oldClient = client
        client = null
        oldClient?.safeDisconnect(reason)
    }

    private fun MqttClient.safeDisconnect(reason: String) {
        runCatching {
            if (isConnected) disconnect()
            close()
            AppLogger.i(TAG, "mqtt disconnected reason=$reason")
        }.onFailure { error ->
            if (error is MqttException) {
                AppLogger.w(TAG, "mqtt disconnect failed reason=$reason code=${error.reasonCode}", error)
            } else {
                AppLogger.w(TAG, "mqtt disconnect failed reason=$reason", error)
            }
        }
    }

    private fun subscriptionTopics(config: MqttConfig): List<String> =
        config.subscriptions
            .flatMap { topic ->
                val normalized = topic.trim().trimEnd('/')
                listOf(
                    normalized,
                    "$normalized/msg",
                    "$normalized/call",
                    withPrefix("msg", normalized),
                    withPrefix("call", normalized),
                )
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun withPrefix(prefix: String, topic: String): String =
        "$prefix/${topic.trimStart('/')}"

    private fun String.redactUri(): String =
        replace(Regex("(?<=://)[^/@]+:[^/@]+@"), "***:***@")

    private companion object {
        const val TAG = "MqttConnection"
        const val MQTT_QOS = 1
        const val KEEP_ALIVE_SECONDS = 60
        const val CONNECTION_TIMEOUT_SECONDS = 10
        const val RECONNECT_DELAY_MS = 3_000L
    }
}
