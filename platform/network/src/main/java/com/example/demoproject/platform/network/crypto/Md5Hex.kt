package com.example.demoproject.platform.network.crypto

import java.security.MessageDigest

/** Lowercase hex MD5 of a UTF-8 string (used for password fields before API submit). */
fun String.md5Hex(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(toByteArray(Charsets.UTF_8))
    return buildString(bytes.size * 2) {
        for (b in bytes) {
            val v = b.toInt() and 0xff
            if (v < 0x10) append('0')
            append(Integer.toHexString(v))
        }
    }
}
