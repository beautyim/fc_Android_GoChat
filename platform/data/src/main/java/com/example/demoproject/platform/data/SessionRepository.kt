package com.example.demoproject.platform.data

import com.example.demoproject.platform.network.config.NetworkConfig

/** Thin helper exposing brand channel for demos. Prefer NetworkRuntime APIs for real work. */
class SessionRepository(
    private val networkConfig: NetworkConfig,
) {
    fun currentChannel(): String = networkConfig.channelName
}
