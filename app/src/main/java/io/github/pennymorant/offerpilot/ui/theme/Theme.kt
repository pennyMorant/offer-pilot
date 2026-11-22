package io.github.pennymorant.offerpilot.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.pennymorant.offerpilot.data.preferences.AppearanceSettings
import io.github.pennymorant.offerpilot.data.preferences.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF1B6B3A), onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F0D2), onPrimaryContainer = Color(0xFF072111),
    secondary = Color(0xFF4B6353), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8DF), onSecondaryContainer = Color(0xFF15241A),
    tertiary = Color(0xFF335CA8), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E4FF), onTertiaryContainer = Color(0xFF0C1E42),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFFADAD7), onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF4F6F3), onBackground = Color(0xFF161D18),
    surface = Color(0xFFF4F6F3), onSurface = Color(0xFF161D18),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEDF1EC),
    surfaceContainer = Color(0xFFE6EBE5),
    surfaceContainerHigh = Color(0xFFDFE5DD),
    surfaceContainerHighest = Color(0xFFD7DED5),
    outlineVariant = Color(0xFFBFC8BE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FDA9B), onPrimary = Color(0xFF05381A),
    primaryContainer = Color(0xFF0E522A), onPrimaryContainer = Color(0xFFC8F0D2),
    secondary = Color(0xFFB2CCB9), onSecondary = Color(0xFF1E3527),
    secondaryContainer = Color(0xFF344B3C), onSecondaryContainer = Color(0xFFDCE8DF),
    tertiary = Color(0xFFAEC6FF), onTertiary = Color(0xFF0A2E6E),
    tertiaryContainer = Color(0xFF1F458C), onTertiaryContainer = Color(0xFFD9E4FF),
    error = Color(0xFFF2B8B5), errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0F1410), onBackground = Color(0xFFDFE5DE),
    surface = Color(0xFF0F1410), onSurface = Color(0xFFDFE5DE),
    surfaceContainerLowest = Color(0xFF0A0F0B),
    surfaceContainerLow = Color(0xFF161C17),
    surfaceContainer = Color(0xFF1C231D),
    surfaceContainerHigh = Color(0xFF252D26),
    surfaceContainerHighest = Color(0xFF2F3830),
    outlineVariant = Color(0xFF3E4940),
)

fun AppearanceSettings.isDark(systemDark: Boolean) = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

fun appearanceColors(context: Context, settings: AppearanceSettings, dark: Boolean): ColorScheme =
    if (settings.dynamic) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) Dark else Light

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverudTheme(settings: AppearanceSettings = AppearanceSettings(), content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = appearanceColors(LocalContext.current, settings, settings.isDark(isSystemInDarkTheme())),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
