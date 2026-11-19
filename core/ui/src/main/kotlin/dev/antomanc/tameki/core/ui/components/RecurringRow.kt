package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.format.MoneySign
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/** The small honest things a recurring payment can say about itself. */
enum class RecurringFlag {
    /** A free trial ends soon: the one thing that must not be missed. */
    TRIAL_ENDING,

    /** The price went up since the last charge. */
    PRICE_UP,

    /** Rated as rarely used. */
    RARELY_USED,

    /** Rated as unused. */
    UNUSED,
}

/**
 * A subscription or bill: brand mark, name, when it renews, how much, and at most two attention
 * tags. A positive rating ("worth it", "fine") adds no tag at all: good news does not need a badge.
 * The amount is always the normalised cost of the chosen period; the row never quietly switches
 * monthly to yearly.
 *
 * @param relativeDate localised and relative, e.g. "in 3 days": the screen owns date formatting.
 * @param flags attention flags only; at most two tags are shown so the row stays scannable.
 * @param income an incoming recurring payment: leaf ink and an explicit plus, never the same ink
 *   as a charge.
 * @param converted the same amount in the base currency when [amount] is foreign and a rate is
 *   known; shown under the original as "≈ €9,20". Null means no rate, so none is invented.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringRow(
    name: String,
    amount: Money,
    relativeDate: String,
    modifier: Modifier = Modifier,
    flags: Set<RecurringFlag> = emptySet(),
    income: Boolean = false,
    converted: Money? = null,
    detail: String? = null,
    logo: Painter? = null,
    brandColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val formatter = rememberMoneyFormatter()
    val orderedFlags = flags.sortedBy { attentionOrder(it) }.take(2)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.rowHeight)
                .padding(vertical = spacing.rowPadding),
            verticalAlignment = Alignment.Top,
        ) {
            BrandMark(
                name = name,
                size = 44.dp,
                logo = logo,
                color = brandColor,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.width(spacing.between))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = relativeDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (!detail.isNullOrBlank()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (orderedFlags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        orderedFlags.forEach { flag -> FlagTag(flag) }
                    }
                }
            }
            Spacer(Modifier.width(spacing.tight))
            Column(
                modifier = Modifier.widthIn(min = 64.dp).padding(top = 2.dp),
                horizontalAlignment = Alignment.End,
            ) {
                AmountText(
                    amount = amount,
                    style = TamekiType.amountSmall,
                    color = if (income) MaterialTheme.tamekiColors.leafInk else MaterialTheme.colorScheme.onSurface,
                    sign = if (income) MoneySign.EXPLICIT else MoneySign.AUTO,
                    animateChanges = false,
                )
                if (converted != null && converted.currency != amount.currency) {
                    Text(
                        text = stringResource(
                            R.string.ui_amount_converted,
                            formatter.format(converted),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier.semantics(mergeDescendants = true) {},
            color = Color.Transparent,
            content = content,
        )
    } else {
        Box { content() }
    }
}

private fun attentionOrder(flag: RecurringFlag): Int = when (flag) {
    RecurringFlag.TRIAL_ENDING -> 0
    RecurringFlag.PRICE_UP -> 1
    RecurringFlag.RARELY_USED -> 2
    RecurringFlag.UNUSED -> 3
}

@Composable
private fun FlagTag(flag: RecurringFlag) {
    val label = stringResource(
        when (flag) {
            RecurringFlag.TRIAL_ENDING -> R.string.ui_recurring_trial_ending
            RecurringFlag.PRICE_UP -> R.string.ui_recurring_price_up
            RecurringFlag.RARELY_USED -> R.string.ui_recurring_rarely_used
            RecurringFlag.UNUSED -> R.string.ui_recurring_unused
        },
    )
    val wash = when (flag) {
        RecurringFlag.TRIAL_ENDING, RecurringFlag.PRICE_UP -> MaterialTheme.tamekiColors.amber.copy(alpha = 0.16f)
        RecurringFlag.RARELY_USED, RecurringFlag.UNUSED -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(
        shape = CircleShape,
        color = wash,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Preview(name = "Recurring · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun RecurringRowLightPreview() {
    PreviewSurface {
        Column {
            RecurringRow(
                name = "Netflix",
                amount = Money(1349, CurrencyCode("EUR")),
                relativeDate = "in 3 days",
                flags = setOf(RecurringFlag.PRICE_UP),
                detail = "€161,88 a year",
                onClick = {},
            )
            RecurringRow(
                name = "Prime Video",
                amount = Money(499, CurrencyCode("EUR")),
                relativeDate = "tomorrow",
                flags = setOf(RecurringFlag.TRIAL_ENDING),
                onClick = {},
            )
            RecurringRow(
                name = "Palestra",
                amount = Money(3900, CurrencyCode("EUR")),
                relativeDate = "in 12 days",
                flags = setOf(RecurringFlag.RARELY_USED),
                onClick = {},
            )
        }
    }
}

@Preview(name = "Recurring · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun RecurringRowDarkPreview() {
    PreviewSurface(dark = true) {
        RecurringRow(
            name = "Spotify",
            amount = Money(1099, CurrencyCode("EUR")),
            relativeDate = "in 8 days",
            flags = setOf(RecurringFlag.TRIAL_ENDING),
            onClick = {},
        )
    }
}
