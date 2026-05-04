package com.serhaterdem.seaverse.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OceanColors.AquaMarine,
    secondary = OceanColors.SeaGreen,
    tertiary = OceanColors.CoralOrange,
    background = OceanColors.DeepBlue,
    surface = OceanColors.MidnightBlue,
    onPrimary = OceanColors.DeepBlue,
    onSecondary = OceanColors.SurfaceFoam,
    onTertiary = OceanColors.SurfaceFoam,
    onBackground = OceanColors.SurfaceFoam,
    onSurface = OceanColors.SurfaceFoam
)

private val LightColorScheme = lightColorScheme(
    primary = OceanColors.OceanBlue,
    secondary = OceanColors.SeaGreen,
    tertiary = OceanColors.CoralOrange,
    background = OceanColors.SurfaceFoam,
    surface = OceanColors.SunlitWater,
    onPrimary = OceanColors.SurfaceFoam,
    onSecondary = OceanColors.SurfaceFoam,
    onTertiary = OceanColors.SurfaceFoam,
    onBackground = OceanColors.DeepBlue,
    onSurface = OceanColors.DeepBlue

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SeaverseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
