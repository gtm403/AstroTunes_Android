package com.example.moodmusicgenerator.ui.theme

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import com.example.moodmusicgenerator.R


val SpaceBlack = Color(0xFF0B0C10)
val DeepSpaceBlue = Color(0xFF1F2833)
val NebulaPurple = Color(0xFF3F0E40)
val StarWhite = Color(0xFFC5C6C7)
val RocketRed = Color(0xFF66FCF1)
val PlanetGreen = Color(0xFF45A29E)

private val DarkColorScheme = darkColorScheme(
    primary = RocketRed,
    onPrimary = SpaceBlack,
    secondary = PlanetGreen,
    onSecondary = SpaceBlack,
    background = DeepSpaceBlue,
    onBackground = StarWhite,
    surface = SpaceBlack,
    onSurface = StarWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = RocketRed,
    onPrimary = StarWhite,
    secondary = PlanetGreen,
    onSecondary = StarWhite,
    background = NebulaPurple,
    onBackground = StarWhite,
    surface = StarWhite,
    onSurface = SpaceBlack,

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

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron_black, FontWeight.Bold)
)


@Composable
fun MoodMusicGeneratorTheme(
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

@Composable
fun SpaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}