package com.example.demoproject.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class BrandColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val onBackground: Color,
)

object DemoBrandThemes {
    val default = BrandColors(
        primary = Color(0xFF1565C0),
        secondary = Color(0xFF00838F),
        background = Color(0xFFF5F7FA),
        onBackground = Color(0xFF101418),
    )
}

@Composable
fun DemoTheme(
    brand: BrandColors = DemoBrandThemes.default,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = brand.primary,
            secondary = brand.secondary,
            background = Color(0xFF101418),
            onBackground = Color(0xFFF5F7FA),
        )
    } else {
        lightColorScheme(
            primary = brand.primary,
            secondary = brand.secondary,
            background = brand.background,
            onBackground = brand.onBackground,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
