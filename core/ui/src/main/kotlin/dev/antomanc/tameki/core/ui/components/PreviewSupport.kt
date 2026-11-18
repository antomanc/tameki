package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.TamekiTheme

/** Preview-only wrapper: the real background, no dynamic colour, no motion. */
@Composable
internal fun PreviewSurface(
    dark: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    TamekiTheme(darkTheme = dark, dynamicColor = false, motionEnabled = false) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) { content() }
            }
        }
    }
}
