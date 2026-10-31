package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AppThemeMode

// 1. Pure #000000 AMOLED / OLED Theme (Electric Cyan)
private val PureOledCyanScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002A32),
    primaryContainer = Color(0xFF002B33),
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = SakuraCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF440B22),
    onSecondaryContainer = OnSakuraCoralContainer,
    tertiary = AmberHighlight,
    onTertiary = Color(0xFF2B1D00),
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = Color.Black,
    onBackground = Color(0xFFF5F8FF),
    surface = Color.Black,
    onSurface = Color(0xFFF5F8FF),
    surfaceVariant = Color(0xFF0D1118),
    onSurfaceVariant = Color(0xFFB0BCD6),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF05070B),
    surfaceContainer = Color(0xFF0B0E15),
    surfaceContainerHigh = Color(0xFF121722),
    surfaceContainerHighest = Color(0xFF1A2232),
    outline = Color(0xFF2B364C),
    outlineVariant = Color(0xFF1A2232)
)

// 2. Cyber-Sakura Pure #000000 OLED Theme
private val PureOledSakuraScheme = darkColorScheme(
    primary = SakuraCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4D0D27),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = ElectricCyan,
    onSecondary = Color(0xFF002A32),
    secondaryContainer = Color(0xFF00313A),
    onSecondaryContainer = OnElectricCyanContainer,
    tertiary = Color(0xFFC792EA),
    onTertiary = Color(0xFF290942),
    background = Color.Black,
    onBackground = Color(0xFFFFF5F8),
    surface = Color.Black,
    onSurface = Color(0xFFFFF5F8),
    surfaceVariant = Color(0xFF130B14),
    onSurfaceVariant = Color(0xFFD6B4C6),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080409),
    surfaceContainer = Color(0xFF100912),
    surfaceContainerHigh = Color(0xFF19101C),
    surfaceContainerHighest = Color(0xFF241729),
    outline = Color(0xFF3E2645),
    outlineVariant = Color(0xFF26172B)
)

// 3. Obsidian Cinema Dark Theme
private val ObsidianDarkScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002A32),
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = SakuraCoral,
    onSecondary = Color.White,
    secondaryContainer = SakuraCoralContainer,
    onSecondaryContainer = OnSakuraCoralContainer,
    tertiary = AmberHighlight,
    onTertiary = Color(0xFF2B1D00),
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = Color(0xFF07090E),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    surfaceContainerHighest = Color(0xFF2C374F),
    outline = OutlineDark,
    outlineVariant = Color(0xFF232C40)
)

// 4. Daylight Light Theme
private val KinoLightColorScheme = lightColorScheme(
    primary = DeepCyanLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4F5FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = DeepCoralLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E3),
    onSecondaryContainer = Color(0xFF3E001A),
    tertiary = Color(0xFF7B5800),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA2),
    onTertiaryContainer = Color(0xFF261900),
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = Color(0xFFEEF2FA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE6ECF7),
    surfaceContainerHighest = Color(0xFFDCE4F2),
    outline = OutlineLight,
    outlineVariant = Color(0xFFD8E0F0)
)

val KinoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun Kino4KTheme(
    themeMode: AppThemeMode = AppThemeMode.OLED_BLACK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.OLED_BLACK -> PureOledCyanScheme
        AppThemeMode.SAKURA_OLED -> PureOledSakuraScheme
        AppThemeMode.OBSIDIAN_DARK -> ObsidianDarkScheme
        AppThemeMode.DAYLIGHT_LIGHT -> KinoLightColorScheme
        AppThemeMode.DYNAMIC_OLED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF060709),
                    surfaceContainer = Color(0xFF0C0E12),
                    surfaceContainerHigh = Color(0xFF14181F),
                    surfaceContainerHighest = Color(0xFF1D222B)
                )
            } else {
                PureOledCyanScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !themeMode.isDark
            insetsController.isAppearanceLightNavigationBars = !themeMode.isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = KinoShapes,
        content = content
    )
}
