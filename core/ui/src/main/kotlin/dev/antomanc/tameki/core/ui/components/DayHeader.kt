package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.ui.theme.TamekiType

/**
 * The sticky separator between days in Activity: the day on the left, its net total on the right.
 * The total is ink, not a verdict; the background is the paper colour so rows scroll underneath
 * without bleeding through.
 *
 * The caller supplies horizontal gutters and sticks the header (a `stickyHeader` in a lazy list).
 */
@Composable
fun DayHeader(
    label: String,
    total: Money,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AmountText(
            amount = total,
            style = TamekiType.amountSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            animateChanges = false,
        )
    }
}

@Preview(name = "Day header · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun DayHeaderLightPreview() {
    PreviewSurface {
        Column {
            DayHeader("Today", Money(4820, CurrencyCode("EUR")))
            DayHeader("Yesterday", Money(12750, CurrencyCode("EUR")))
            DayHeader("12 November", Money(-2500, CurrencyCode("EUR")))
        }
    }
}

@Preview(name = "Day header · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun DayHeaderDarkPreview() {
    PreviewSurface(dark = true) {
        DayHeader("Today", Money(4820, CurrencyCode("EUR")))
    }
}
