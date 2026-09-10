package com.example.demoproject.platform.rtc.agora

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime permissions commonly needed for Agora 1v1 video + Bluetooth audio routes on API 31+.
 * Declare matching `<uses-permission>` in this module's manifest (merged into the app).
 */
object RtcCallPermissions {

    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
            )
        }

    fun allGranted(context: Context): Boolean =
        required().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
}
