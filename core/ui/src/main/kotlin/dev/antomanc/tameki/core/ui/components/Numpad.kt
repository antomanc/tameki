package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.R
import dev.antomanc.tameki.core.ui.theme.TamekiType
import java.text.DecimalFormatSymbols

/**
 * The quick-add numpad: 1–9, the locale's decimal key, 0 and backspace. Large light keys on tonal
 * paper, a haptic tick on every press, and no "=" because there is nothing to calculate — the
 * amount is what the user types.
 *
 * The decimal key shows the separator the locale uses, so an Italian user taps "," and an English
 * one taps ".". Keys are real buttons with labels for TalkBack.
 *
 * @param onDigit called with the typed character, "0"–"9".
 */
@Composable
fun Numpad(
    onDigit: (Char) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val separator = remember(configuration) {
        DecimalFormatSymbols.getInstance(configuration.locales[0]).decimalSeparator
    }
    val tick = {
        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap), modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    NumpadKey(
                        onClick = {
                            tick()
                            onDigit(digit)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(digit.toString(), style = TamekiType.numpadKey)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap), modifier = Modifier.fillMaxWidth()) {
            NumpadKey(
                onClick = {
                    tick()
                    onDecimal()
                },
                modifier = Modifier.weight(1f),
                contentDescription = stringResource(R.string.ui_decimal_separator),
            ) {
                Text(separator.toString(), style = TamekiType.numpadKey)
            }
            NumpadKey(
                onClick = {
                    tick()
                    onDigit('0')
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("0", style = TamekiType.numpadKey)
            }
            NumpadKey(
                onClick = {
                    tick()
                    onBackspace()
                },
                modifier = Modifier.weight(1f),
                contentDescription = stringResource(R.string.ui_backspace),
            ) {
                Icon(
                    imageVector = TamekiIcons.Backspace,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private val KeyGap: Dp = 8.dp
private val KeyHeight: Dp = 58.dp

@Composable
private fun NumpadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = KeyHeight),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .then(
                    if (contentDescription != null) {
                        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Preview(name = "Numpad · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 340)
@Composable
private fun NumpadLightPreview() {
    PreviewSurface {
        Numpad(onDigit = {}, onDecimal = {}, onBackspace = {})
    }
}

@Preview(name = "Numpad · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 340)
@Composable
private fun NumpadDarkPreview() {
    PreviewSurface(dark = true) {
        Numpad(onDigit = {}, onDecimal = {}, onBackspace = {})
    }
}
