package com.overud.ui.theme

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.overud.data.preferences.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF365F25), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EFAE), onPrimaryContainer = Color(0xFF172C0A),
    secondary = Color(0xFF5C6252), secondaryContainer = Color(0xFFE0E6D3),
    tertiary = Color(0xFF665488), tertiaryContainer = Color(0xFFEBDCFF),
    background = Color(0xFFF8FAF0), onBackground = Color(0xFF1B2118),
    surface = Color(0xFFF8FAF0), onSurface = Color(0xFF1B2118),
    surfaceContainer = Color(0xFFEDF0E5), surfaceContainerLow = Color(0xFFF2F5EA),
    surfaceContainerHigh = Color(0xFFE7EBDF), outlineVariant = Color(0xFFC5CBBB),
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB0D58B), onPrimary = Color(0xFF1D370F),
    primaryContainer = Color(0xFF284C18), onPrimaryContainer = Color(0xFFD3EFAE),
    secondaryContainer = Color(0xFF3F4936), onSecondaryContainer = Color(0xFFE0E6D3),
    tertiaryContainer = Color(0xFF4C3D68), onTertiaryContainer = Color(0xFFEBDCFF),
    background = Color(0xFF11160E), onBackground = Color(0xFFE1E8D8),
    surface = Color(0xFF11160E), onSurface = Color(0xFFE1E8D8), surfaceContainer = Color(0xFF1D2419),
    surfaceContainerLow = Color(0xFF181F14), surfaceContainerHigh = Color(0xFF283022),
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
@Composable fun OverudTheme(settings: AppearanceSettings = AppearanceSettings(), content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = appearanceColors(LocalContext.current, settings, settings.isDark(isSystemInDarkTheme())),
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp)),
        typography = Typography(
            displayLarge = TextStyle(fontSize = 58.sp, lineHeight = 64.sp, fontWeight = FontWeight.SemiBold),
            headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
            titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        ), content = content,
    )
}
