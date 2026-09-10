package com.example.demoproject.platform.callkit

object CallMediaType {
    const val Video = 1
    const val Voice = 2

    fun isVoice(type: Int): Boolean = type == Voice
    fun isVideo(type: Int): Boolean = type == Video
}
