package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.money.MoneyParser
import dev.antomanc.tameki.core.ui.format.rememberMoneyFormatter

/**
 * A single amount field for every sheet and screen that asks for money.
 *
 * The keyboard is the locale's decimal keyboard, and the text is parsed by [MoneyParser], so "12,50"
 * on an Italian phone and "12.50" on an English one both become the same 1250 minor units. It never
 * touches a binary float: an unparseable string keeps what the user typed, reports [onValueChange]
 * with null and leaves the decision to the caller.
 *
 * @param value the amount to show when the field is created or reset; edits are local text until
 *   they parse.
 * @param onValueChange called with the parsed amount, or null while the text is empty or invalid.
 * @param label the field's label; the caller supplies the localised string.
 */
@Composable
fun MoneyField(
    value: Money?,
    onValueChange: (Money?) -> Unit,
    currency: CurrencyCode,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val formatter = rememberMoneyFormatter()
    var text by remember { mutableStateOf(value?.let { formatter.format(it) }.orEmpty()) }
    val parsed = if (text.isBlank()) null else MoneyParser.parse(text, currency)
    // Follow an external reset (a sheet reopening, a saved form loading) without fighting the
    // keyboard: while the text already parses to [value], the user's string is left alone.
    LaunchedEffect(value, parsed) {
        if (value != null && value != parsed) text = formatter.format(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            onValueChange(if (raw.isBlank()) null else MoneyParser.parse(raw, currency))
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        label = label?.let { { Text(it) } },
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.titleLarge,
    )
}

@Preview(name = "Money field · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 380)
@Composable
private fun MoneyFieldLightPreview() {
    PreviewSurface {
        MoneyField(
            value = Money(1349, CurrencyCode("EUR")),
            onValueChange = {},
            currency = CurrencyCode("EUR"),
            label = "Amount",
        )
    }
}

@Preview(name = "Money field · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 380)
@Composable
private fun MoneyFieldDarkPreview() {
    PreviewSurface(dark = true) {
        MoneyField(
            value = null,
            onValueChange = {},
            currency = CurrencyCode("EUR"),
            label = "Amount",
            supportingText = "Renews every month",
        )
    }
}
