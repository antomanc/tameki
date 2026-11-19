package dev.antomanc.tameki.core.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Shows one snackbar per [token] on the shared [host], replacing whatever is up (one snackbar at a
 * time: a new event must not queue behind an old one and lose its own Undo).
 *
 * The token is the whole state machine: null means nothing to show; a new value means a new event,
 * even when the message is the same as last time. [onResolved] is always called, also when the
 * screen leaves mid-snackbar, so the owner can expire the event and it cannot replay later.
 */
@Composable
fun OneShotSnackbar(
    token: Long?,
    host: SnackbarHostState,
    message: String,
    actionLabel: String? = null,
    duration: SnackbarDuration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
    onAction: (token: Long) -> Unit = {},
    onResolved: (token: Long) -> Unit,
) {
    val currentMessage by rememberUpdatedState(message)
    val currentAction by rememberUpdatedState(onAction)
    val currentResolved by rememberUpdatedState(onResolved)
    LaunchedEffect(token) {
        val shown = token ?: return@LaunchedEffect
        host.currentSnackbarData?.dismiss()
        try {
            val result = host.showSnackbar(
                message = currentMessage,
                actionLabel = actionLabel,
                withDismissAction = actionLabel != null,
                duration = duration,
            )
            if (result == SnackbarResult.ActionPerformed) currentAction(shown)
        } finally {
            currentResolved(shown)
        }
    }
}
