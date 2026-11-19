package dev.antomanc.tameki

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled

@Composable
internal fun AppNavigationBar(
    selected: AppTab?,
    onSelect: (AppTab) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    NavigationBar {
        AppTab.entries.forEach { tab ->
            val isSelected = selected == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(tab)
                },
                icon = {
                    Icon(
                        if (isSelected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/** What the add control creates. The primary tap creates [primary]; the menu holds the rest. */
internal enum class AddAction { QuickAdd, NewRecurring, ScanReceipt, PasteAi }

/**
 * The add control: one tap on the FAB creates (a movement, or a recurring payment on the Recurring
 * tab); the caret beside it expands a Material 3 FAB menu with the other create actions. A
 * long-press on the FAB opens the same menu.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AddFabMenu(
    onRecurringTab: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (AddAction) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    BackHandler(enabled = expanded) { onExpandedChange(false) }
    val primary = if (onRecurringTab) AddAction.NewRecurring else AddAction.QuickAdd
    val extras = buildList {
        add(if (onRecurringTab) AddAction.QuickAdd else AddAction.NewRecurring)
        add(AddAction.ScanReceipt)
        add(AddAction.PasteAi)
    }
    FloatingActionButtonMenu(
        expanded = expanded,
        horizontalAlignment = Alignment.End,
        button = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                AddFab(
                    onClick = {
                        onExpandedChange(false)
                        onAction(primary)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExpandedChange(true)
                    },
                    label = stringResource(
                        if (onRecurringTab) R.string.fab_new_recurring else R.string.fab_quick_add,
                    ),
                    menuLabel = stringResource(R.string.fab_menu),
                )
                AddMenuButton(
                    expanded = expanded,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onExpandedChange(!expanded)
                    },
                    label = stringResource(R.string.fab_menu),
                )
            }
        },
    ) {
        extras.forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onAction(action)
                },
                text = { Text(stringResource(action.label())) },
                icon = { Icon(action.icon(), contentDescription = null) },
            )
        }
    }
}

private fun AddAction.label(): Int = when (this) {
    AddAction.QuickAdd -> R.string.fab_quick_add
    AddAction.NewRecurring -> R.string.fab_new_recurring
    AddAction.ScanReceipt -> R.string.fab_scan_receipt
    AddAction.PasteAi -> R.string.fab_paste_ai
}

private fun AddAction.icon() = when (this) {
    AddAction.QuickAdd -> Icons.Outlined.EditNote
    AddAction.NewRecurring -> Icons.Outlined.Repeat
    AddAction.ScanReceipt -> Icons.Outlined.DocumentScanner
    AddAction.PasteAi -> Icons.Outlined.AutoAwesome
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AddFab(onClick: () -> Unit, onLongClick: () -> Unit, label: String, menuLabel: String = "") {
    // FloatingActionButton owns its click handler and would swallow taps before a
    // combinedClickable modifier sees them, so the FAB is a Surface with both gestures.
    // primary (not primaryContainer): the dark-theme container was ~1.7:1 on background and
    // hid the one primary action. No decorative shadow: DESIGN.md depth is tonal only.
    Surface(
        shape = FloatingActionButtonDefaults.shape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .size(56.dp)
            .clip(FloatingActionButtonDefaults.shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = menuLabel.ifEmpty { null },
                role = Role.Button,
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Add, contentDescription = label)
        }
    }
}

/**
 * The visible second half of the add control: a 48 dp tonal circle with a caret that expands the
 * FAB menu, turning over when it is open. A long-press on [AddFab] does the same, but a gesture
 * alone is not discoverable.
 */
@Composable
internal fun AddMenuButton(expanded: Boolean, onClick: () -> Unit, label: String) {
    val motion = rememberMotionEnabled()
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (motion) MaterialTheme.motionScheme.fastSpatialSpec() else androidx.compose.animation.core.snap(),
        label = "caret",
    )
    val state = stringResource(if (expanded) R.string.fab_menu_open else R.string.fab_menu_closed)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (expanded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (expanded) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
        // The tuned container alone is ~1.2:1 on paper: the outline is the non-text boundary
        // that makes the caret read as a control without borrowing the FAB's primary fill.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.size(48.dp).semantics { stateDescription = state },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = label,
                modifier = Modifier.size(24.dp).rotate(rotation),
            )
        }
    }
}
