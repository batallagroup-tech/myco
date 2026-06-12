package com.batallagroup.myco.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MycoDarkColorScheme = darkColorScheme(
    primary = MycoGreen,
    onPrimary = MycoBg,
    primaryContainer = MycoGreenDark,
    onPrimaryContainer = MycoGreenLight,
    secondary = MycoGreenDark,
    onSecondary = MycoOnSurface,
    background = MycoBg,
    onBackground = MycoOnSurface,
    surface = MycoSurface,
    onSurface = MycoOnSurface,
    surfaceVariant = MycoSurface2,
    onSurfaceVariant = MycoSubtle,
    error = MycoError,
    outline = MycoSurface3
)

@Composable
fun MycoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MycoDarkColorScheme,
        typography = MycoTypography,
        shapes = MycoShapes,
        content = content
    )
}
