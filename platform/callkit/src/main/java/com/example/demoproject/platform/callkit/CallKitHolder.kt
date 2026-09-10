package com.example.demoproject.platform.callkit

/**
 * Process-wide [CallCoordinator] installed by the application after MQTT + Agora are ready.
 */
object CallKitHolder {
    @Volatile
    var coordinator: CallCoordinator? = null
}
