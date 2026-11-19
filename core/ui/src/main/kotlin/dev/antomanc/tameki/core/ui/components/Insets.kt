package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Bottom room for a sheet or screen that owns a text field: the keyboard while it is open, the
 * navigation bar otherwise. Union, not sum: the IME inset already covers the navigation bar area,
 * so stacking `navigationBarsPadding()` and `imePadding()` would leave a dead gap above the
 * keyboard and push the primary button off a short screen.
 */
@Composable
fun Modifier.imeAwareBottomPadding(): Modifier =
    windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
