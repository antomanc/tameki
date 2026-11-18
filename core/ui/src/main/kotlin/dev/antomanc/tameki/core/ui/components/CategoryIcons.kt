package dev.antomanc.tameki.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.antomanc.tameki.core.ui.theme.CategoryColors

/*
 * Tameki's icon set. Material's core set does not cover groceries, a pharmacy cross or a fuel pump,
 * and the extended set is huge, so the 30 categories are drawn here as one coherent 24 dp line set:
 * 1.7 dp round strokes, no fills, a 3 dp safe area. They read as one family because they share
 * those three numbers, not because they were downloaded together.
 */

private const val STROKE = 1.7f

private fun strokeIcon(name: String, vararg paths: String): ImageVector =
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
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private val icons: Map<String, ImageVector> by lazy {
    mapOf(
        "groceries" to strokeIcon(
            "groceries",
            "M5.5 9.5h13l-1.05 9.4a1.8 1.8 0 0 1-1.8 1.6H8.35a1.8 1.8 0 0 1-1.8-1.6z",
            "M9 9.5V7.8a3 3 0 0 1 6 0v1.7",
        ),
        "restaurant" to strokeIcon(
            "restaurant",
            "M6.5 3.2v4.3a2.6 2.6 0 0 0 5.2 0V3.2",
            "M9.1 10.1V20.8",
            "M15.4 20.8V3.2c2.5 2.8 2.5 7.4 0 10.4",
        ),
        "coffee" to strokeIcon(
            "coffee",
            "M4.8 8.8h11.4v5.6a5.7 5.7 0 0 1-11.4 0z",
            "M16.2 10.2h1.5a2.6 2.6 0 0 1 0 5.2h-1.9",
            "M3.4 20.4h14.2",
            "M8.6 3.4c-.7 1.1.5 1.9-.1 3.1",
            "M12.2 3.4c-.7 1.1.5 1.9-.1 3.1",
        ),
        "transport" to strokeIcon(
            "transport",
            "M5.2 5.4A2.2 2.2 0 0 1 7.4 3.2h9.2a2.2 2.2 0 0 1 2.2 2.2v10.4a2.2 2.2 0 0 1-2.2 2.2H7.4a2.2 2.2 0 0 1-2.2-2.2z",
            "M5.2 10.6h13.6",
            "M8.4 20.6v-2.6",
            "M15.6 20.6v-2.6",
            "M8.4 14.4h.01",
            "M15.6 14.4h.01",
        ),
        "fuel" to strokeIcon(
            "fuel",
            "M5.4 20.4V5.2a1.8 1.8 0 0 1 1.8-1.8h4a1.8 1.8 0 0 1 1.8 1.8v15.2",
            "M3.6 20.4h11.4",
            "M7.4 6.6h3.6v3.6H7.4z",
            "M13 6.6h3.3a1.6 1.6 0 0 1 1.6 1.6v9.4h2.1",
        ),
        "home" to strokeIcon(
            "home",
            "M3.4 10.6 12 3.4l8.6 7.2",
            "M5.6 9.4V20.4h12.8V9.4",
            "M10.2 20.4v-5.2h3.6v5.2",
        ),
        "utilities" to strokeIcon(
            "utilities",
            "M13.2 2.8 5.2 13.6h6.2l-.8 7.6 8-11h-6.2z",
        ),
        "phone" to strokeIcon(
            "phone",
            "M7.6 2.6h8.8a1.9 1.9 0 0 1 1.9 1.9v15a1.9 1.9 0 0 1-1.9 1.9H7.6a1.9 1.9 0 0 1-1.9-1.9v-15a1.9 1.9 0 0 1 1.9-1.9z",
            "M10.4 5.4h3.2",
            "M12 17.6h.01",
        ),
        "internet" to strokeIcon(
            "internet",
            "M12 3.4a8.6 8.6 0 1 0 0 17.2 8.6 8.6 0 1 0 0-17.2",
            "M3.6 12h16.8",
            "M12 3.4c2.5 2.4 3.7 5.3 3.7 8.6s-1.2 6.2-3.7 8.6c-2.5-2.4-3.7-5.3-3.7-8.6s1.2-6.2 3.7-8.6z",
        ),
        "health" to strokeIcon(
            "health",
            "M12 20.2C9.4 18 3.8 14.2 3.8 9.7a4.1 4.1 0 0 1 8.2-2.4 4.1 4.1 0 0 1 8.2 2.4c0 4.5-5.6 8.3-8.2 10.5z",
        ),
        "pharmacy" to strokeIcon(
            "pharmacy",
            "M5.5 4.5h13A1.5 1.5 0 0 1 20 6v12a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18V6a1.5 1.5 0 0 1 1.5-1.5z",
            "M12 8.2v7.6",
            "M8.2 12h7.6",
        ),
        "sport" to strokeIcon(
            "sport",
            "M3.6 9.4v5.2",
            "M6.6 7.4v9.2",
            "M17.4 7.4v9.2",
            "M20.4 9.4v5.2",
            "M6.6 12h10.8",
        ),
        "clothes" to strokeIcon(
            "clothes",
            "M8.4 3.6 12 6.2l3.6-2.6 4.6 3.2-2.2 3.4-2-1.1v10.3H8V9.1l-2 1.1-2.2-3.4z",
        ),
        "beauty" to strokeIcon(
            "beauty",
            "M11.2 3.6l1.55 5.05L17.8 10.2l-5.05 1.55L11.2 16.8 9.65 11.75 4.6 10.2l5.05-1.55z",
            "M18.2 15.6l.7 2.1 2.1.7-2.1.7-.7 2.1-.7-2.1-2.1-.7 2.1-.7z",
        ),
        "gifts" to strokeIcon(
            "gifts",
            "M4.6 11.4h14.8v8.4a1.2 1.2 0 0 1-1.2 1.2H5.8a1.2 1.2 0 0 1-1.2-1.2z",
            "M3.6 7.6h16.8v3.8H3.6z",
            "M12 7.6v13.4",
            "M12 7.6c-1.9 0-4.2-.8-4.2-2.6 0-1.4 1.5-1.9 2.5-1.2 1.1.8 1.7 2.6 1.7 3.8z",
            "M12 7.6c1.9 0 4.2-.8 4.2-2.6 0-1.4-1.5-1.9-2.5-1.2-1.1.8-1.7 2.6-1.7 3.8z",
        ),
        "travel" to strokeIcon(
            "travel",
            "M10.4 3.2c0-1 .7-1.6 1.6-1.6s1.6.6 1.6 1.6v5.4l7 4.3v2.3l-7-2.2v4.3l2.4 1.8v1.5L12 19.7l-4 1v-1.5l2.4-1.8v-4.3l-7 2.2v-2.3l7-4.3z",
        ),
        "entertainment" to strokeIcon(
            "entertainment",
            "M4.4 8.6V6.4a1.6 1.6 0 0 1 1.6-1.6h12a1.6 1.6 0 0 1 1.6 1.6v2.2a2.6 2.6 0 0 0 0 6.8v2.2a1.6 1.6 0 0 1-1.6 1.6H6a1.6 1.6 0 0 1-1.6-1.6v-2.2a2.6 2.6 0 0 0 0-6.8z",
            "M14.6 6.4v2",
            "M14.6 10.8v2.4",
            "M14.6 15.6v2",
        ),
        "streaming" to strokeIcon(
            "streaming",
            "M5.4 4.4h13.2a2 2 0 0 1 2 2v11.2a2 2 0 0 1-2 2H5.4a2 2 0 0 1-2-2V6.4a2 2 0 0 1 2-2z",
            "M10.2 8.6l5.4 3.4-5.4 3.4z",
        ),
        "music" to strokeIcon(
            "music",
            "M9.2 17.4V6.2l9-2v11",
            "M9.2 17.4a2.2 2.2 0 1 1-4.4 0 2.2 2.2 0 1 1 4.4 0",
            "M18.2 15.4a2.2 2.2 0 1 1-4.4 0 2.2 2.2 0 1 1 4.4 0",
        ),
        "games" to strokeIcon(
            "games",
            "M6.6 7.4h10.8a4.6 4.6 0 0 1 4.5 5.5l-.8 4.2a2.5 2.5 0 0 1-4.4 1.1L14.8 16H9.2l-1.9 2.2a2.5 2.5 0 0 1-4.4-1.1l-.8-4.2a4.6 4.6 0 0 1 4.5-5.5z",
            "M7.2 10.6v4",
            "M5.2 12.6h4",
            "M16.4 11.2h.01",
            "M18.4 13.2h.01",
        ),
        "education" to strokeIcon(
            "education",
            "M2.8 9.6 12 5.2l9.2 4.4L12 14z",
            "M6.4 11.6v4.6c0 1.4 2.5 2.6 5.6 2.6s5.6-1.2 5.6-2.6v-4.6",
            "M20.2 10.4v4.4",
        ),
        "kids" to strokeIcon(
            "kids",
            "M12 5.4a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 1 0 0-4.8",
            "M8.6 20.4v-2.6a3.4 3.4 0 0 1 6.8 0v2.6",
            "M8.6 14.6 6.2 16.6",
            "M15.4 14.6 17.8 16.6",
        ),
        "pets" to strokeIcon(
            "pets",
            "M6.9 9.9a1.35 1.35 0 1 0 0-2.7 1.35 1.35 0 1 0 0 2.7",
            "M10.2 7.7a1.35 1.35 0 1 0 0-2.7 1.35 1.35 0 1 0 0 2.7",
            "M13.8 7.7a1.35 1.35 0 1 0 0-2.7 1.35 1.35 0 1 0 0 2.7",
            "M17.1 9.9a1.35 1.35 0 1 0 0-2.7 1.35 1.35 0 1 0 0 2.7",
            "M12 12.6c2.7 0 4.7 2 4.7 4.2 0 1.8-1.6 3-3.2 2.6-.9-.2-2.1-.2-3 0-1.6.4-3.2-.8-3.2-2.6 0-2.2 2-4.2 4.7-4.2z",
        ),
        "insurance" to strokeIcon(
            "insurance",
            "M12 3.2 19 5.8v5.7c0 4.1-2.9 7.4-7 9.1-4.1-1.7-7-5-7-9.1V5.8z",
            "M9 11.6l2.2 2.2 4-4.2",
        ),
        "taxes" to strokeIcon(
            "taxes",
            "M6.2 3.4h11.6v17.2l-2.3-1.6-2.3 1.6-2.3-1.6-2.3 1.6-2.4-1.6z",
            "M9.2 8.2h5.6",
            "M9.2 12h5.6",
        ),
        "bank" to strokeIcon(
            "bank",
            "M3 9.4 12 4.2l9 5.2z",
            "M4.4 17.6h15.2",
            "M4.4 20.4h15.2",
            "M6.6 11.4v6.2",
            "M10 11.4v6.2",
            "M14 11.4v6.2",
            "M17.4 11.4v6.2",
        ),
        "salary" to strokeIcon(
            "salary",
            "M12 3.4a8.6 8.6 0 1 0 0 17.2 8.6 8.6 0 1 0 0-17.2",
            "M12 16.4v-8.6",
            "M8.6 11.2 12 7.8l3.4 3.4",
        ),
        "refund" to strokeIcon(
            "refund",
            "M4.6 9.6h9.2a4.6 4.6 0 0 1 0 9.2H8.4",
            "M8.4 5.6 4.4 9.6l4 4",
        ),
        "investment" to strokeIcon(
            "investment",
            "M3.6 18 9.2 12l3.4 3.4L20.4 8",
            "M15.4 8h5v5",
        ),
        // A calm ring with a dash — not three dots, which read as an ellipsis in empty avatars.
        "other" to strokeIcon(
            "other",
            "M12 3.4a8.6 8.6 0 1 0 0 17.2 8.6 8.6 0 1 0 0-17.2",
            "M8.4 12h7.2",
        ),
    )
}

/** The vector for a category key; the "other" glyph for anything unknown. */
fun categoryIcon(key: String): ImageVector = icons[key] ?: icons.getValue("other")

/** Utility glyphs that are not categories but belong to the same drawn family. */
object TamekiIcons {
    /** Backspace for the numpad: a key with a cross, not a Unicode glyph. */
    val Backspace: ImageVector by lazy {
        strokeIcon(
            "backspace",
            "M9.2 5.4h11.2a1.4 1.4 0 0 1 1.4 1.4v10.4a1.4 1.4 0 0 1-1.4 1.4H9.2L3.4 12z",
            "M12.2 9.6l4.8 4.8",
            "M17 9.6l-4.8 4.8",
        )
    }
}

/**
 * A category glyph in its fixed hue. The icon is decorative unless [contentDescription] is set:
 * rows put the category name in text and never repeat it for TalkBack.
 */
@Composable
fun CategoryIcon(
    categoryKey: String,
    modifier: Modifier = Modifier,
    tint: Color = CategoryColors.hueFor(categoryKey).color(),
    contentDescription: String? = null,
    size: Dp = 22.dp,
) {
    Icon(
        imageVector = categoryIcon(categoryKey),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/** The category glyph in a soft circular wash of its own hue: the leading mark of a list row. */
@Composable
fun CategoryAvatar(
    categoryKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    val tint = CategoryColors.hueFor(categoryKey).color()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        CategoryIcon(
            categoryKey = categoryKey,
            tint = tint,
            contentDescription = contentDescription,
            size = size * 0.5f,
        )
    }
}

@Preview(name = "Icons · light", showBackground = true, backgroundColor = 0xFFF2F0EA, widthDp = 360)
@Composable
private fun CategoryIconsLightPreview() {
    PreviewSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryColors.keys.chunked(10).forEach { row ->
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        CategoryAvatar(categoryKey = key)
                    }
                }
            }
        }
    }
}

@Preview(name = "Icons · dark", showBackground = true, backgroundColor = 0xFF141513, widthDp = 360)
@Composable
private fun CategoryIconsDarkPreview() {
    PreviewSurface(dark = true) {
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("groceries", "restaurant", "coffee", "travel", "streaming", "salary").forEach { key ->
                CategoryAvatar(categoryKey = key)
            }
        }
    }
}
