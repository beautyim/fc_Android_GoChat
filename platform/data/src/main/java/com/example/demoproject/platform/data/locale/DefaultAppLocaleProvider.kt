package com.example.demoproject.platform.data.locale

import com.example.demoproject.platform.network.provider.AppLocaleProvider
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppLocaleProvider @Inject constructor() : AppLocaleProvider {
    override fun currentLanguageTag(): String {
        val locale = Locale.getDefault()
        val language = locale.language.orEmpty().ifBlank { "en" }
        val country = locale.country.orEmpty()
        return if (country.isBlank()) language else "$language-$country"
    }
}
