package dev.antomanc.tameki.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing on an 8 dp grid: 16 dp gutters, 8 dp between related things, 24 dp between groups.
 * Screens should reach for these names instead of literal dp values so rhythm stays consistent.
 */
@Immutable
data class TamekiSpacing(
    /** Distance from the screen edge to content. */
    val gutter: Dp = 16.dp,
    /** Space between two things that belong together (icon and label). */
    val tight: Dp = 8.dp,
    /** Standard gap between rows of a group. */
    val between: Dp = 12.dp,
    /** Gap between groups inside a section. */
    val group: Dp = 16.dp,
    /** Gap between sections. */
    val section: Dp = 32.dp,
    /** Inner padding of a card. */
    val cardPadding: Dp = 20.dp,
    /** Vertical padding inside a list row. */
    val rowPadding: Dp = 12.dp,
    /** Smallest permitted list row height, so every row is comfortably tappable. */
    val rowHeight: Dp = 60.dp,
    /** Minimum touch target, per the Android platform. */
    val touchTarget: Dp = 48.dp,
    /** Height of the hero block's bottom breathing room. */
    val heroBottom: Dp = 28.dp,
)

/** The spacing tokens for the current theme. Read them through [MaterialTheme.spacing]. */
val LocalTamekiSpacing = staticCompositionLocalOf { TamekiSpacing() }
