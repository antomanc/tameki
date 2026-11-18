package dev.antomanc.tameki.core.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Bottom-nav icons drawn in the same stroke family as core:ui category icons.
 * Selected variants use a slightly heavier stroke; unselected stay lighter.
 */
object NavIcons {
    private const val STROKE = 1.7f
    private const val STROKE_SELECTED = 2.1f

    val Today: ImageVector by lazy { sun(selected = false) }
    val TodaySelected: ImageVector by lazy { sun(selected = true) }
    val Activity: ImageVector by lazy { list(selected = false) }
    val ActivitySelected: ImageVector by lazy { list(selected = true) }
    val Recurring: ImageVector by lazy { repeat(selected = false) }
    val RecurringSelected: ImageVector by lazy { repeat(selected = true) }
    val Save: ImageVector by lazy { sprout(selected = false) }
    val SaveSelected: ImageVector by lazy { sprout(selected = true) }

    private fun sun(selected: Boolean): ImageVector = strokeIcon(
        name = if (selected) "nav_today_selected" else "nav_today",
        stroke = if (selected) STROKE_SELECTED else STROKE,
        "M12 7.2a4.8 4.8 0 1 0 0 9.6 4.8 4.8 0 0 0 0-9.6z",
        "M12 2.8v1.6",
        "M12 19.6v1.6",
        "M4.4 12H2.8",
        "M21.2 12h-1.6",
        "M6.1 6.1l-1.1-1.1",
        "M19 19l-1.1-1.1",
        "M17.9 6.1l1.1-1.1",
        "M6.1 17.9l-1.1 1.1",
    )

    private fun list(selected: Boolean): ImageVector = strokeIcon(
        name = if (selected) "nav_activity_selected" else "nav_activity",
        stroke = if (selected) STROKE_SELECTED else STROKE,
        "M6.5 5.2h11a1.8 1.8 0 0 1 1.8 1.8v10a1.8 1.8 0 0 1-1.8 1.8h-11a1.8 1.8 0 0 1-1.8-1.8v-10a1.8 1.8 0 0 1 1.8-1.8z",
        "M8.2 9.2h7.6",
        "M8.2 12h7.6",
        "M8.2 14.8h5.2",
    )

    private fun repeat(selected: Boolean): ImageVector = strokeIcon(
        name = if (selected) "nav_recurring_selected" else "nav_recurring",
        stroke = if (selected) STROKE_SELECTED else STROKE,
        "M7.2 8.2A5.5 5.5 0 0 1 17 7.4",
        "M17 4.8v3.2h-3.2",
        "M16.8 15.8A5.5 5.5 0 0 1 7 16.6",
        "M7 19.2v-3.2h3.2",
    )

    private fun sprout(selected: Boolean): ImageVector = strokeIcon(
        name = if (selected) "nav_save_selected" else "nav_save",
        stroke = if (selected) STROKE_SELECTED else STROKE,
        "M12 20.4V11.2",
        "M12 11.2C12 7.6 9.2 4.8 5.8 5.2c.6 3.8 3.2 6 6.2 6",
        "M12 11.2c0-3.6 2.8-6.4 6.2-6  -.6 3.8-3.2 6-6.2 6",
    )

    private fun strokeIcon(name: String, stroke: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
