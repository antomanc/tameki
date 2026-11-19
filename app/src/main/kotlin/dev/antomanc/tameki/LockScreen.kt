package dev.antomanc.tameki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * The cover shown while the journal is locked. It is a real surface with theme colours, so it is
 * readable in dark mode instead of black text on a dark window; when the phone has no enrolled
 * credential it says so rather than offering an unlock that would silently fail.
 */
@Composable
internal fun LockScreen(
    lockAvailable: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSecuritySettings: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_unlock_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!lockAvailable) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_unlock_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Without a screen lock the prompt can never succeed: offer the one way out instead of
            // an Unlock button that does nothing.
            if (lockAvailable) {
                Button(onClick = onUnlock) {
                    Text(stringResource(R.string.app_unlock_action))
                }
            } else {
                Button(onClick = onOpenSecuritySettings) {
                    Text(stringResource(R.string.app_unlock_open_settings))
                }
            }
        }
    }
}
