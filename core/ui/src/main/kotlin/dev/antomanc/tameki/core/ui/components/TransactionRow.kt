package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.format.MoneySign
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/**
 * A transaction in a list: category mark, what it was, where and when, and the amount in tabular
 * figures. Spending is ink; only incoming money is leaf, with an explicit plus. The amount is
 * never truncated: the title column gives way first.
 *
 * @param meta the quiet second line: account, time, place. Joined with [note] when both exist.
 * @param containerColor the row's own opaque fill. Defaults to the screen background so a row
 *   never lets a swipe/undo layer underneath show through; callers on another surface pass theirs.
 * @param onClick opens the transaction. Null makes the row inert (e.g. inside a picker preview).
 * @param onDelete when set, adds a "Delete" accessibility action to the row's own merged
 *   semantics, so the node TalkBack focuses can delete without a swipe. Null hides the action.
 * @param amount null when the amount is genuinely unknown (an inbox draft): the row then shows a
 *   placeholder instead of a false €0.00.
 */
@Composable
fun TransactionRow(
    title: String,
    amount: Money?,
    categoryKey: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.background,
    meta: String? = null,
    note: String? = null,
    income: Boolean = false,
    pending: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val deleteLabel = stringResource(R.string.ui_transaction_delete)
    val rowSemantics = Modifier.semantics(mergeDescendants = true) {
        if (onDelete != null) {
            customActions = listOf(
                CustomAccessibilityAction(label = deleteLabel) {
                    onDelete()
                    true
                },
            )
        }
    }
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.rowHeight)
                .padding(vertical = spacing.rowPadding),
            verticalAlignment = Alignment.Top,
        ) {
            CategoryAvatar(categoryKey = categoryKey, size = 40.dp, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(spacing.between))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!meta.isNullOrBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!note.isNullOrBlank()) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (pending) {
                    PendingTag()
                }
            }
            Spacer(Modifier.width(spacing.tight))
            Column(
                modifier = Modifier.widthIn(min = 64.dp).padding(top = 2.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                if (amount != null) {
                    AmountText(
                        amount = amount,
                        style = TamekiType.amountSmall,
                        color = if (income) MaterialTheme.tamekiColors.leafInk else MaterialTheme.colorScheme.onSurface,
                        sign = if (income) MoneySign.EXPLICIT else MoneySign.AUTO,
                        animateChanges = false,
                    )
                } else {
                    // An unknown amount is not €0.00: show a placeholder until it is read.
                    Text(
                        text = "—",
                        style = TamekiType.amountSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.then(rowSemantics),
            color = containerColor,
            content = content,
        )
    } else {
        Box(modifier.then(rowSemantics).background(containerColor)) { content() }
    }
}

@Composable
private fun PendingTag() {
    Surface(
        modifier = Modifier.padding(top = 4.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.tamekiColors.amber.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = stringResource(R.string.ui_transaction_pending),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Preview(name = "Transactions · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun TransactionRowLightPreview() {
    PreviewSurface {
        Column {
            TransactionRow(
                title = "Esselunga",
                amount = Money(4320, CurrencyCode("EUR")),
                categoryKey = "groceries",
                meta = "Conto corrente",
                    note = "18:42",
                onClick = {},
            )
            TransactionRow(
                title = "Stipendio",
                amount = Money(215000, CurrencyCode("EUR")),
                categoryKey = "salary",
                meta = "Conto corrente",
                income = true,
                onClick = {},
            )
            TransactionRow(
                title = "Spritz con Marta",
                amount = Money(1400, CurrencyCode("EUR")),
                categoryKey = "restaurant",
                note = "Aperitivo",
                pending = true,
            )
        }
    }
}

@Preview(name = "Transactions · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun TransactionRowDarkPreview() {
    PreviewSurface(dark = true) {
        Column {
            TransactionRow(
                title = "Spotify",
                amount = Money(1099, CurrencyCode("EUR")),
                categoryKey = "streaming",
                meta = "Carta · 07:00",
                onClick = {},
            )
        }
    }
}
