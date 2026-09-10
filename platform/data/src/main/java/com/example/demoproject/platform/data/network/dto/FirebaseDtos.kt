package com.example.demoproject.platform.data.network.dto

import kotlinx.serialization.Serializable

/** Body for `POST firebase/token`. */
@Serializable
data class FirebaseTokenRequestDto(
    val token: String,
)
