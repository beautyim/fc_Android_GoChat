package com.example.demoproject.ui.designsystem

import androidx.compose.ui.graphics.Brush

object DemoGradients {
    val primaryButton: Brush = Brush.horizontalGradient(
        colors = listOf(DemoColors.gradientStart, DemoColors.gradientEnd),
    )
}
