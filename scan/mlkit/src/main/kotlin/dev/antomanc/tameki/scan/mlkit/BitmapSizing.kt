package dev.antomanc.tameki.scan.mlkit

/**
 * Pure helpers for loading receipt photos without blowing memory.
 * [sampleSize] mirrors [android.graphics.BitmapFactory.Options.inSampleSize] (power of two).
 */
object BitmapSizing {
    const val DEFAULT_MAX_SIDE_PX: Int = 2048

    /**
     * Largest power-of-two sample size such that both sides stay ≤ [maxSide] after decoding
     * (or 1 when the image already fits).
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int = DEFAULT_MAX_SIDE_PX): Int {
        require(width > 0 && height > 0) { "width/height must be positive" }
        require(maxSide > 0) { "maxSide must be positive" }
        var sample = 1
        while (width / (sample * 2) >= maxSide || height / (sample * 2) >= maxSide) {
            sample *= 2
        }
        // Also downsample when only one side exceeds maxSide but the /2 loop did not trip.
        while (width / sample > maxSide || height / sample > maxSide) {
            sample *= 2
        }
        return sample
    }

    /** Degrees to rotate a bitmap so it matches EXIF orientation (1–8). */
    fun rotationDegreesForExif(orientation: Int): Int = when (orientation) {
        3, 4 -> 180
        5, 6 -> 90
        7, 8 -> 270
        else -> 0
    }

    /** Whether EXIF orientation also mirrors the image (flip before/after rotate). */
    fun needsHorizontalFlip(orientation: Int): Boolean =
        orientation == 2 || orientation == 4 || orientation == 5 || orientation == 7
}
