package com.example.demoproject.platform.rtc.api

data class RtcInitConfig(
    val appId: String,
    val logLevel: RtcLogLevel = RtcLogLevel.Info,
)

enum class RtcLogLevel {
    Verbose,
    Debug,
    Info,
    Warn,
    Error,
    None,
}

