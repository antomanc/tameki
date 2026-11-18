package dev.antomanc.tameki.core.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.components.AmountText
import dev.antomanc.tameki.core.ui.components.CategoryAvatar
import dev.antomanc.tameki.core.ui.components.CategoryBar
import dev.antomanc.tameki.core.ui.components.CategoryBars
import dev.antomanc.tameki.core.ui.components.CategoryChip
import dev.antomanc.tameki.core.ui.components.CategoryIcon
import dev.antomanc.tameki.core.ui.components.DayHeader
import dev.antomanc.tameki.core.ui.components.EmptyState
import dev.antomanc.tameki.core.ui.components.InsightCard
import dev.antomanc.tameki.core.ui.components.MonthBar
import dev.antomanc.tameki.core.ui.components.MonthlyBars
import dev.antomanc.tameki.core.ui.components.Numpad
import dev.antomanc.tameki.core.ui.components.PaceBar
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.core.ui.components.RecurringRow
import dev.antomanc.tameki.core.ui.components.SectionHeader
import dev.antomanc.tameki.core.ui.components.SegmentedKind
import dev.antomanc.tameki.core.ui.components.TransactionKind
import dev.antomanc.tameki.core.ui.components.TransactionRow
import dev.antomanc.tameki.core.ui.format.MoneySign
import dev.antomanc.tameki.core.ui.theme.CategoryColors
import dev.antomanc.tameki.core.ui.theme.TamekiType
import dev.antomanc.tameki.core.ui.theme.spacing
import dev.antomanc.tameki.core.ui.theme.tamekiColors

/**
 * Debug-only gallery of every component in `core:ui`, in the order a screen would meet them.
 * It is not reachable from the release navigation: the app exposes it from a debug Activity and
 * the Roborazzi screenshot tests render individual components with the same building blocks.
 *
 * Sample names and merchants are placeholders, not real user data.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val eur = CurrencyCode("EUR")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.gutter, vertical = spacing.group),
    ) {
        SectionHeader(title = stringResource(R.string.ui_gallery_amounts))
        AmountText(
            amount = Money(128450, eur),
            style = TamekiType.hero,
            dimMinorUnits = true,
            omitZeroMinorUnits = true,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Available to spend",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.group))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.group)) {
            AmountText(amount = Money(4200, eur), style = TamekiType.amount)
            AmountText(
                amount = Money(215000, eur),
                style = TamekiType.amount,
                sign = MoneySign.EXPLICIT,
                color = MaterialTheme.tamekiColors.leafInk,
            )
            AmountText(
                amount = Money(120000, eur),
                style = TamekiType.amount,
                omitZeroMinorUnits = true,
            )
        }

        SectionHeader(title = stringResource(R.string.ui_gallery_colors))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight)) {
            DataColour(name = "Leaf", color = MaterialTheme.tamekiColors.leaf, modifier = Modifier.weight(1f))
            DataColour(name = "Amber", color = MaterialTheme.tamekiColors.amber, modifier = Modifier.weight(1f))
            DataColour(name = "Over", color = MaterialTheme.tamekiColors.over, modifier = Modifier.weight(1f))
        }

        SectionHeader(title = stringResource(R.string.ui_gallery_categories))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
            verticalArrangement = Arrangement.spacedBy(spacing.tight),
        ) {
            CategoryColors.keys.forEachIndexed { index, key ->
                CategoryChip(
                    name = key.replaceFirstChar { it.uppercaseChar() },
                    categoryKey = key,
                    selected = index == 0,
                    onClick = {},
                )
            }
        }
        Spacer(Modifier.height(spacing.group))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
            verticalArrangement = Arrangement.spacedBy(spacing.tight),
        ) {
            CategoryColors.keys.forEach { key ->
                CategoryAvatar(categoryKey = key)
            }
        }

        SectionHeader(title = stringResource(R.string.ui_gallery_pace))
        PaceBar(
            spent = Money(42000, eur),
            expected = Money(56000, eur),
            budget = Money(92000, eur),
        )
        Spacer(Modifier.height(spacing.group))
        PaceBar(
            spent = Money(64000, eur),
            expected = Money(56000, eur),
            budget = Money(92000, eur),
        )
        Spacer(Modifier.height(spacing.group))
        PaceBar(
            spent = Money(101000, eur),
            expected = Money(56000, eur),
            budget = Money(92000, eur),
        )

        SectionHeader(title = stringResource(R.string.ui_gallery_rows))
        DayHeader(label = "Today", total = Money(4820, eur))
        TransactionRow(
            title = "Esselunga",
            amount = Money(4320, eur),
            categoryKey = "groceries",
            meta = "Conto corrente",
                    note = "18:42",
            onClick = {},
        )
        TransactionRow(
            title = "Stipendio",
            amount = Money(215000, eur),
            categoryKey = "salary",
            meta = "Conto corrente",
            income = true,
            onClick = {},
        )
        TransactionRow(
            title = "Spritz con Marta",
            amount = Money(1400, eur),
            categoryKey = "restaurant",
            note = "Aperitivo",
            pending = true,
        )
        Spacer(Modifier.height(spacing.group))
        RecurringRow(
            name = "Netflix",
            amount = Money(1349, eur),
            relativeDate = "in 3 days",
            flags = setOf(RecurringFlag.PRICE_UP, RecurringFlag.WORTH_IT),
            detail = "€161,88 a year",
            onClick = {},
        )
        RecurringRow(
            name = "Prime Video",
            amount = Money(499, eur),
            relativeDate = "tomorrow",
            flags = setOf(RecurringFlag.TRIAL_ENDING),
            onClick = {},
        )
        RecurringRow(
            name = "Palestra",
            amount = Money(3900, eur),
            relativeDate = "in 12 days",
            flags = setOf(RecurringFlag.RARELY_USED),
            onClick = {},
        )

        SectionHeader(title = stringResource(R.string.ui_gallery_insight))
        InsightCard(
            title = "Switch Spotify to the yearly plan",
            body = "The annual price you entered is 17% cheaper than twelve monthly charges.",
            yearlySaving = Money(3480, eur),
            actionLabel = "See the plan",
            onAction = {},
            onDismiss = {},
            categoryKey = "music",
        )

        SectionHeader(title = stringResource(R.string.ui_gallery_input))
        SegmentedKind(selected = TransactionKind.EXPENSE, onSelect = {})
        Spacer(Modifier.height(spacing.group))
        Numpad(onDigit = {}, onDecimal = {}, onBackspace = {})

        SectionHeader(title = stringResource(R.string.ui_gallery_charts))
        MonthlyBars(
            bars = listOf(
                MonthBar("Jun", Money(91200, eur)),
                MonthBar("Jul", Money(104500, eur)),
                MonthBar("Aug", Money(98700, eur)),
                MonthBar("Sep", Money(128450, eur)),
                MonthBar("Oct", Money(110200, eur)),
                MonthBar("Nov", Money(74200, eur)),
            ),
        )
        Spacer(Modifier.height(spacing.section))
        CategoryBars(
            rows = listOf(
                CategoryBar("Groceries", Money(31240, eur), "groceries"),
                CategoryBar("Home", Money(21500, eur), "home"),
                CategoryBar("Restaurants", Money(14900, eur), "restaurant"),
                CategoryBar("Transport", Money(8600, eur), "transport"),
                CategoryBar("Streaming", Money(3297, eur), "streaming"),
            ),
        )

        SectionHeader(title = stringResource(R.string.ui_gallery_states))
        EmptyState(
            title = "No subscriptions yet",
            body = "Add Netflix, the phone bill or the gym: Tameki turns them into a monthly cost and tells you which one is worth keeping.",
            categoryKey = "streaming",
            actionLabel = "Add a recurring payment",
            onAction = {},
        )
        Spacer(Modifier.height(spacing.section))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(spacing.group))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(categoryKey = "other")
            Spacer(Modifier.width(spacing.tight))
            Text(
                text = "core:ui component gallery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(spacing.section))
    }
}

@Composable
private fun DataColour(name: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(color, RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
