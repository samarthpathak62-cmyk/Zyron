package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = BedrockBlack,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = DiamondCyan,
    onSecondary = BedrockBlack,
    secondaryContainer = BedrockCard,
    onSecondaryContainer = DiamondCyanLight,
    tertiary = AmethystPurple,
    onTertiary = TextPrimary,
    background = BedrockBlack,
    onBackground = TextPrimary,
    surface = BedrockSurface,
    onSurface = TextPrimary,
    surfaceVariant = BedrockSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BedrockBorder,
    error = RedstoneRed,
    onError = TextPrimary,
  )

private val LightColorScheme =
  darkColorScheme( // Zyron AI is primarily a dark-mode launcher interface
    primary = EmeraldGreen,
    onPrimary = BedrockBlack,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = DiamondCyan,
    onSecondary = BedrockBlack,
    secondaryContainer = BedrockCard,
    onSecondaryContainer = DiamondCyanLight,
    tertiary = AmethystPurple,
    onTertiary = TextPrimary,
    background = BedrockBlack,
    onBackground = TextPrimary,
    surface = BedrockSurface,
    onSurface = TextPrimary,
    surfaceVariant = BedrockSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BedrockBorder,
    error = RedstoneRed,
    onError = TextPrimary,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
