package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled

/**
 * A Material 3 Expressive connected button group for mutually exclusive options. Segments morph
 * their inner corners on press and selection, animate width slightly under touch, and collapse into
 * an overflow menu when text scales beyond the row width.
 */
@Composable
fun <T> ConnectedToggleGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val motion = rememberMotionEnabled()
    val colors = ToggleButtonDefaults.colors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    ButtonGroup(
        overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState) },
        modifier = modifier.fillMaxWidth(),
        expandedRatio = if (motion) ButtonGroupDefaults.ExpandedRatio else 0f,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selected == option
            customItem(
                buttonGroupContent = {
                    val interactionSource = remember { MutableInteractionSource() }
                    val itemText = label(option)
                    val shapes = when {
                        options.size == 1 -> ToggleButtonShapes(
                            shape = ToggleButtonDefaults.shape,
                            pressedShape = ToggleButtonDefaults.pressedShape,
                            checkedShape = ToggleButtonDefaults.checkedShape,
                        )
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { if (!isSelected) onSelect(option) },
                        modifier = Modifier
                            .weight(1f)
                            .animateWidth(interactionSource)
                            .semantics { role = Role.RadioButton },
                        shapes = shapes,
                        colors = colors,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        interactionSource = interactionSource,
                    ) {
                        Text(
                            text = itemText,
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
                            },
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            softWrap = true,
                        )
                    }
                },
                menuContent = { menuState ->
                    val itemText = label(option)
                    DropdownMenuItem(
                        text = { Text(itemText) },
                        onClick = {
                            if (!isSelected) onSelect(option)
                            menuState.dismiss()
                        },
                    )
                },
            )
        }
    }
}
