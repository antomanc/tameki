package dev.antomanc.tameki.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * The loading state of a screen. The local database usually answers within a frame, and a spinner
 * flashing for that frame is noise; only a read slower than [delayMillis] shows the Material 3
 * Expressive loading indicator, faded in.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DelayedLoading(modifier: Modifier = Modifier, delayMillis: Long = 400) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        show = true
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = show, enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec())) {
            LoadingIndicator()
        }
    }
}
