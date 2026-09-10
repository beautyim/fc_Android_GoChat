package com.example.demoproject.ui.foundation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Global breakpoint thresholds aligned with Material window size classes.
 * Pages must not invent their own width magic numbers.
 */
object WindowBreakpoints {
    val MediumWidth: Dp = 600.dp
    val ExpandedWidth: Dp = 840.dp

    /** Max readable content width on Medium / Expanded — avoids stretched rows on tablets. */
    val MaxContentWidth: Dp = 640.dp
}

enum class WindowWidthClass { Compact, Medium, Expanded }

enum class WindowHeightClass { Compact, Medium, Expanded }

@Immutable
data class DemoWindowSize(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
) {
    val isCompactWidth: Boolean get() = widthClass == WindowWidthClass.Compact

    /** Very short height (landscape / multi-window) — collapse decorative chrome; keep body scrollable. */
    val isCompactHeight: Boolean get() = heightClass == WindowHeightClass.Compact

    companion object {
        fun from(widthDp: Dp, heightDp: Dp): DemoWindowSize = DemoWindowSize(
            widthClass = when {
                widthDp < WindowBreakpoints.MediumWidth -> WindowWidthClass.Compact
                widthDp < WindowBreakpoints.ExpandedWidth -> WindowWidthClass.Medium
                else -> WindowWidthClass.Expanded
            },
            heightClass = when {
                heightDp < CompactHeight -> WindowHeightClass.Compact
                heightDp < ExpandedHeight -> WindowHeightClass.Medium
                else -> WindowHeightClass.Expanded
            },
        )

        private val CompactHeight: Dp = 480.dp
        private val ExpandedHeight: Dp = 900.dp
    }
}

/**
 * Provided by `:app` from the real window size; falls back to [LocalConfiguration] for Preview/tests.
 */
val LocalWindowSize = staticCompositionLocalOf<DemoWindowSize?> { null }

/**
 * Single entry point for page breakpoints — do not read `screenWidthDp` in feature code.
 */
@Composable
@ReadOnlyComposable
fun currentWindowSize(): DemoWindowSize {
    LocalWindowSize.current?.let { return it }
    val configuration = LocalConfiguration.current
    return DemoWindowSize.from(
        widthDp = configuration.screenWidthDp.dp,
        heightDp = configuration.screenHeightDp.dp,
    )
}

@Composable
fun ProvideWindowSize(windowSize: DemoWindowSize, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWindowSize provides windowSize, content = content)
}

/**
 * Content width: fill on Compact; capped on Medium / Expanded (center via parent alignment).
 */
@Composable
fun Modifier.readableContentWidth(
    max: Dp = WindowBreakpoints.MaxContentWidth,
): Modifier = if (currentWindowSize().isCompactWidth) {
    fillMaxWidth()
} else {
    fillMaxWidth().widthIn(max = max)
}
