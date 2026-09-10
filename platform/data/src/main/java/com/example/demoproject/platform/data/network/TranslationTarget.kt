package com.example.demoproject.platform.data.network

import android.content.res.Resources
import java.util.Locale

/** Primary system locale tag, e.g. `zh-CN`, `en-US`. */
fun currentSystemLanguageTag(): String {
    val systemLocales = Resources.getSystem().configuration.locales
    if (!systemLocales.isEmpty) {
        return systemLocales[0].toLanguageTag()
    }
    return Locale.getDefault().toLanguageTag()
}

/**
 * Maps a BCP-47 tag to the `/translation/submit` `target` code.
 * When [languageTag] is null/blank, uses the device system language.
 */
fun resolveTranslationTarget(languageTag: String? = null): String {
    val tag = languageTag?.takeIf { it.isNotBlank() } ?: currentSystemLanguageTag()
    val locale = runCatching { Locale.forLanguageTag(tag) }.getOrNull() ?: Locale.getDefault()
    return locale.toTranslationApiTarget()
}

private fun Locale.toTranslationApiTarget(): String =
    when (language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            script.equals("Hant", ignoreCase = true) -> "zh-TW"
            country.equals("TW", ignoreCase = true) -> "zh-TW"
            country.equals("HK", ignoreCase = true) -> "zh-TW"
            country.equals("MO", ignoreCase = true) -> "zh-TW"
            else -> "zh-CN"
        }
        "pt" -> "pt"
        "es" -> "es"
        "ar" -> "ar"
        "tr" -> "tr"
        "en" -> "en"
        "ja" -> "ja"
        else -> toLanguageTag().ifBlank { "en" }
    }
