package dev.antomanc.tameki

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import dev.antomanc.tameki.core.ui.components.OneShotSnackbar
import dev.antomanc.tameki.feature.activity.QuickAddNotice
import dev.antomanc.tameki.feature.activity.SavedVariant

/** Renders the quick-add sheet's one-shot notices on the app's snackbar host. */
@Composable
internal fun QuickAddNoticeEffect(
    notice: QuickAddNotice?,
    host: SnackbarHostState,
    savedLabel: String,
    splitsScaledLabel: String,
    splitsDroppedLabel: String,
    deletedLabel: String,
    receiptFailedLabel: String,
    undoLabel: String,
    onUndo: (Long) -> Unit,
    onResolved: (Long) -> Unit,
) {
    val message = when (notice) {
        null -> ""
        is QuickAddNotice.Saved -> when (notice.variant) {
            SavedVariant.Plain -> savedLabel
            SavedVariant.SplitsScaled -> splitsScaledLabel
            SavedVariant.SplitsDropped -> splitsDroppedLabel
        }
        is QuickAddNotice.Deleted -> deletedLabel
        is QuickAddNotice.ReceiptFailed -> receiptFailedLabel
    }
    OneShotSnackbar(
        token = notice?.token,
        host = host,
        message = message,
        actionLabel = if (notice?.undoable == true) undoLabel else null,
        onAction = onUndo,
        onResolved = onResolved,
    )
}
