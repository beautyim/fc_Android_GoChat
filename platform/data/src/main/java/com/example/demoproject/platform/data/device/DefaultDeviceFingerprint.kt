package com.example.demoproject.platform.data.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.demoproject.platform.network.crypto.provider.DeviceFingerprint
import java.util.Locale
import java.util.TimeZone

/**
 * Android implementation of [DeviceFingerprint]. Every accessor is permission-free
 * on API 26+ (minSdk) and falls back to a safe empty-string / `0` when an
 * underlying system call misbehaves on a vendor ROM.
 *
 * Scope of signals:
 *  - UA fields match the legacy contract field-for-field.
 *  - Risk signals (`has_proxy`, `has_vpn`, `operator`, `is_simulator`) are the
 *    minimum the server currently requires for the device-risk payload.
 *
 * This class intentionally duplicates a handful of detections already present in
 * [auth feature device provider]:
 * that one serves the profile payload (different field names + shapes), this one
 * serves transport-level UA/risk. Merging them would couple a feature-layer file
 * to the network transport, so they stay separate for now.
 */
class DefaultDeviceFingerprint(
    context: Context,
) : DeviceFingerprint {
    private val context = context.applicationContext


    override fun androidVersion(): String = "Android ${Build.VERSION.RELEASE.orEmpty()}"

    override fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    override fun deviceId(): String = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }.getOrDefault("")

    override fun timeZone(): String = TimeZone.getDefault().id

    override fun systemLanguage(): String = Locale.getDefault().language

    /**
     * Matches the legacy `DeviceUtils.getResolution()` output
     * (`"${widthPixels}|${heightPixels}"`) because the server's UA parser expects
     * TWO separate segments for width / height — the resolution field already
     * contains the `|` separator, so the single-item [DeviceFingerprint.resolution]
     * accessor contributes two fields to the final UA string.
     *
     * Uses [WindowManager.defaultDisplay] explicitly instead of [Context.getDisplay]
     * because the latter returns `null` when called on an [android.app.Application]
     * context, which is the only context this singleton has access to. The
     * `getRealMetrics` deprecation is intentional: [android.view.WindowMetrics]
     * (API 30+) is not a drop-in replacement and the legacy backend expects the
     * `widthPixels|heightPixels` wire format.
     *
     * Falls back to `"1080|1920"` on exotic vendor ROMs where [WindowManager] is
     * unavailable — same fallback as the legacy client.
     */
    @Suppress("DEPRECATION")
    override fun resolution(): String = runCatching {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            ?: return@runCatching "1080|1920"
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        "${metrics.widthPixels}|${metrics.heightPixels}"
    }.getOrDefault("1080|1920")

    override fun model(): String = "${Build.MANUFACTURER.orEmpty()}|${Build.MODEL.orEmpty()}"

    override fun riskSignals(): Map<String, Any> = linkedMapOf(
        "has_proxy" to if (detectProxy()) 1 else 0,
        "has_vpn" to if (detectVpn()) 1 else 0,
        "operator" to detectOperator(),
        "is_simulator" to if (detectEmulator()) 1 else 0,
    )

    private fun detectProxy(): Boolean {
        val host = System.getProperty("http.proxyHost")
        val port = System.getProperty("http.proxyPort")?.toIntOrNull() ?: -1
        return !host.isNullOrBlank() && port != -1
    }

    private fun detectVpn(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val active = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(active) ?: return false
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    }

    private fun detectOperator(): String = runCatching {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return@runCatching ""
        listOf(
            tm.simOperator.orEmpty(),
            tm.simOperatorName.orEmpty(),
            tm.networkOperator.orEmpty(),
            tm.networkOperatorName.orEmpty(),
        ).joinToString("|")
    }.getOrDefault("")

    private fun detectEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
    }
}
