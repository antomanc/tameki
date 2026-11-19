package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A horizontally scrolling row of chips with a soft fade at the edges that still have content.
 *
 * A chip clipped flush by the viewport reads as a truncated label ("Cappu", "Tras|") even though
 * its own text was never ellipsized. The fade says "scroll" instead, and disappears at each end
 * once there is nothing more to reach.
 *
 * The fade is painted over the scroll container, not inside it, so it stays put while the chips
 * move. [fadeColor] must match the surface behind the row; the default is the screen surface.
 *
 * Start and end follow [LayoutDirection], not the physical screen: in RTL the row begins at the
 * right, so the fade for "more before" sits on the right and the fade for "more after" on the left.
 */
@Composable
fun FadingScrollRow(
    modifier: Modifier = Modifier,
    fadeColor: Color = MaterialTheme.colorScheme.surface,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    state: ScrollState = rememberScrollState(),
    content: @Composable RowScope.() -> Unit,
) {
    val fadeWidth = 24.dp
    Row(
        modifier = modifier
            .drawWithContent {
                drawContent()
                val edge = fadeWidth.toPx().coerceAtMost(size.width / 2f)
                if (edge <= 0f) return@drawWithContent
                val startIsLeft = layoutDirection == LayoutDirection.Ltr
                // Solid at the outer edge, transparent toward the content.
                val fadeFromLeft = Brush.horizontalGradient(
                    0f to fadeColor,
                    1f to fadeColor.copy(alpha = 0f),
                    startX = 0f,
                    endX = edge,
                )
                val fadeFromRight = Brush.horizontalGradient(
                    0f to fadeColor.copy(alpha = 0f),
                    1f to fadeColor,
                    startX = size.width - edge,
                    endX = size.width,
                )
                val edgeSize = Size(edge, size.height)
                // Something remains before the viewport: fade the start edge.
                if (state.value > 0) {
                    if (startIsLeft) {
                        drawRect(brush = fadeFromLeft, size = edgeSize)
                    } else {
                        drawRect(
                            brush = fadeFromRight,
                            size = edgeSize,
                            topLeft = Offset(size.width - edge, 0f),
                        )
                    }
                }
                // Something remains after the viewport: fade the end edge.
                if (state.value < state.maxValue) {
                    if (startIsLeft) {
                        drawRect(
                            brush = fadeFromRight,
                            size = edgeSize,
                            topLeft = Offset(size.width - edge, 0f),
                        )
                    } else {
                        drawRect(brush = fadeFromLeft, size = edgeSize)
                    }
                }
            }
            .horizontalScroll(state),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}
