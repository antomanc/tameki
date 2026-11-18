package dev.antomanc.tameki.scan.mlkit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

/**
 * Loads a receipt image from disk, downsampled and EXIF-rotated for OCR / GenAI.
 */
object ReceiptBitmapLoader {
    fun load(
        imagePath: String,
        maxSide: Int = BitmapSizing.DEFAULT_MAX_SIDE_PX,
    ): Bitmap {
        val file = File(imagePath)
        require(file.isFile) { "Image not found: $imagePath" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        require(width > 0 && height > 0) { "Cannot decode image bounds: $imagePath" }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = BitmapSizing.sampleSize(width, height, maxSide)
        }
        val raw = BitmapFactory.decodeFile(imagePath, opts)
            ?: error("Cannot decode image: $imagePath")

        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val degrees = BitmapSizing.rotationDegreesForExif(orientation)
        val flip = BitmapSizing.needsHorizontalFlip(orientation)
        if (degrees == 0 && !flip) return raw

        val matrix = Matrix().apply {
            if (flip) preScale(-1f, 1f)
            if (degrees != 0) postRotate(degrees.toFloat())
        }
        val transformed = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (transformed !== raw) raw.recycle()
        return transformed
    }
}
