package com.example.turismoexplorer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

val OceanBlue = Color(0xFF2E5AAC)
val OceanBlueDark = Color(0xFF244782)
val Amber = Color(0xFFFFCA28)
val AmberDark = Color(0xFFC79D00)
val Teal = Color(0xFF26A69A)
val TealDark = Color(0xFF1E7E77)
val BackgroundLight = Color(0xFFF7F9FC)
val BackgroundDark = Color(0xFF0F141B)

private val ColorWhite = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val ColorBlack = androidx.compose.ui.graphics.Color(0xFF000000)

private val LightColors = lightColorScheme(
    primary = OceanBlue,
    onPrimary = ColorWhite,
    secondary = Amber,
    onSecondary = ColorBlack,
    tertiary = Teal,
    onTertiary = ColorWhite,
    background = BackgroundLight,
    surface = ColorWhite
)

private val DarkColors = darkColorScheme(
    primary = OceanBlueDark,
    onPrimary = ColorWhite,
    secondary = AmberDark,
    onSecondary = ColorBlack,
    tertiary = TealDark,
    onTertiary = ColorWhite,
    background = BackgroundDark,
    surface = BackgroundDark
)

@Composable
fun TurismoExplorerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}



private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

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
fun TurismoExplorerTheme(
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