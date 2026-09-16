package com.example.demoproject.platform.callkit

import com.example.demoproject.platform.callkit.signaling.CallSignalingClient
import com.example.demoproject.platform.rtc.api.RtcClient

/**
 * Process-wide callkit handles installed by the application after MQTT + Agora are ready.
 */
object CallKitHolder {
    @Volatile
    var coordinator: CallCoordinator? = null

    @Volatile
    var rtc: RtcClient? = null

    @Volatile
    var signaling: CallSignalingClient? = null
}
