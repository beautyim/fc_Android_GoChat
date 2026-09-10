package com.example.demoproject.platform.mqtt

data class MqttConfig(
    val mqttHost: String,
    val tcpPort: Int = 0,
    val clientId: String,
    val username: String = "",
    val password: String = "",
    val subscriptions: List<String> = emptyList(),
) {
    val brokerUri: String
        get() = mqttHost.toBrokerUri(tcpPort)
}

private fun String.toBrokerUri(tcpPort: Int): String {
    val host = trim()
    if (host.startsWith("tcp://") ||
        host.startsWith("ssl://") ||
        host.startsWith("ws://") ||
        host.startsWith("wss://")
    ) {
        return host
    }
    return if (tcpPort > 0) "tcp://$host:$tcpPort" else "tcp://$host"
}

data class MqttInboundMessage(
    val topic: String,
    val payload: String,
    val qos: Int = 0,
    val retained: Boolean = false,
)
