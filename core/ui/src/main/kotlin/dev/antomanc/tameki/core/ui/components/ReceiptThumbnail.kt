package dev.antomanc.tameki.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File

/**
 * A receipt thumbnail that never blocks composition.
 *
 * Decoding, EXIF rotation and downsampling happen in Coil off the main thread, and the bitmap is
 * sampled to the size this composable is actually drawn at: a 12 MP camera photo must not become a
 * 48 MB ARGB_8888 allocation on the frame that opens the quick-add sheet. Coil owns the bitmap and
 * returns it to its pool, so callers must not recycle it.
 *
 * @param path the receipt file on disk; a missing file simply renders nothing.
 * @param contentDescription null when the adjacent label already names the receipt.
 */
@Composable
fun ReceiptThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    AsyncImage(
        model = File(path),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}
