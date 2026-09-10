package com.example.demoproject.platform.data.session

import com.example.demoproject.platform.network.crypto.provider.UidProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Always anonymous uid for AES key derivation (server contract). */
@Singleton
class SessionUidProvider @Inject constructor() : UidProvider {
    override fun currentUid(): Long = UidProvider.ANONYMOUS
}
