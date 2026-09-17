package com.example.demoproject.ui.designsystem

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.example.demoproject.ui.foundation.TextSize

/**
 * Shared [TextAutoSize] presets for prices and single-line localized copy.
 *
 * Callers must give the [androidx.compose.material3.Text] a **bounded** max
 * width (e.g. [androidx.compose.foundation.layout.fillMaxWidth],
 * [androidx.compose.foundation.layout.RowScope.weight]) — otherwise auto-size
 * never shrinks. Prefer `maxLines = 1` + [androidx.compose.ui.text.style.TextOverflow.Ellipsis]
 * and keep `softWrap = true` so measuring sees finite constraints.
 */
object DemoTextAutoSize {
    private val DefaultStep = 0.25.sp

    /**
     * Step-based auto size capped at [maxFontSize] (the Figma / token size).
     * [minFontSize] is clamped so it never exceeds [maxFontSize].
     */
    fun of(
        maxFontSize: TextUnit,
        minFontSize: TextUnit = TextSize.autoSizeMin,
        stepSize: TextUnit = DefaultStep,
    ): TextAutoSize {
        val resolvedMin =
            if (minFontSize.type == maxFontSize.type && minFontSize > maxFontSize) {
                maxFontSize
            } else {
                minFontSize
            }
        return TextAutoSize.StepBased(
            minFontSize = resolvedMin,
            maxFontSize = maxFontSize,
            stepSize = stepSize,
        )
    }

    /** Sale / list / strip prices and coin amounts. */
    fun price(maxFontSize: TextUnit): TextAutoSize =
        of(maxFontSize = maxFontSize, minFontSize = TextSize.autoSizeMin)

    /**
     * Localized single-line labels in chips, badges, and fixed-height CTAs.
     * Uses a tighter floor when the design size is already compact (&lt; 12sp).
     */
    fun label(maxFontSize: TextUnit): TextAutoSize {
        val tight =
            maxFontSize.type == TextUnitType.Sp &&
                maxFontSize < TextSize.xs
        return of(
            maxFontSize = maxFontSize,
            minFontSize = if (tight) TextSize.autoSizeMinTight else TextSize.autoSizeMin,
        )
    }
}
