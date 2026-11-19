package dev.antomanc.tameki.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Tameki's theme: warm paper as the static base, dynamic colour from the wallpaper when the phone
 * offers it, fixed data and category colours on top, and the app's spacing and motion tokens.
 *
 * @param darkTheme whether to use the dark scheme; follows the system by default.
 * @param dynamicColor let the wallpaper supply surfaces and accents (Android 12+, so every
 *   supported phone). Data colours never follow it.
 * @param motionEnabled whether the amount roll and other authored motion may play. Defaults to
 *   the system animator duration scale, so "remove animations" is honoured everywhere.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TamekiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    motionEnabled: Boolean = rememberSystemMotionEnabled(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> PaperDarkScheme
        else -> PaperLightScheme
    }
    CompositionLocalProvider(
        LocalTamekiColors provides if (darkTheme) PaperDarkColors else PaperLightColors,
        LocalTamekiSpacing provides TamekiSpacing(),
        LocalTamekiMotion provides TamekiMotion(),
        LocalMotionEnabled provides motionEnabled,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TamekiTypography,
            shapes = TamekiShapes,
            // Expressive springs for every M3 component (sheets, FAB menu, chips, switches):
            // one physical feel across the app, scaled by the system animation setting.
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/** The fixed data colours: leaf, amber, over. They never change with the wallpaper. */
val MaterialTheme.tamekiColors: TamekiColors
    @Composable
    @ReadOnlyComposable
    get() = LocalTamekiColors.current

/** The 8 dp spacing tokens. */
val MaterialTheme.spacing: TamekiSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalTamekiSpacing.current

/** The spring and duration tokens. */
val MaterialTheme.motion: TamekiMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalTamekiMotion.current
