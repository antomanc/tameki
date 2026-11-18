package dev.antomanc.tameki.scan.mlkit

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BitmapSizingTest {
    @Test
    fun sampleSize_isOneWhenAlreadyWithinMax() {
        assertThat(BitmapSizing.sampleSize(800, 600, maxSide = 2048)).isEqualTo(1)
    }

    @Test
    fun sampleSize_powersOfTwoUntilBothSidesFit() {
        assertThat(BitmapSizing.sampleSize(4096, 3072, maxSide = 2048)).isEqualTo(2)
        assertThat(BitmapSizing.sampleSize(8192, 1000, maxSide = 2048)).isEqualTo(4)
    }

    @Test
    fun rotationDegreesForExif_matchesCommonOrientations() {
        assertThat(BitmapSizing.rotationDegreesForExif(1)).isEqualTo(0)
        assertThat(BitmapSizing.rotationDegreesForExif(6)).isEqualTo(90)
        assertThat(BitmapSizing.rotationDegreesForExif(3)).isEqualTo(180)
        assertThat(BitmapSizing.rotationDegreesForExif(8)).isEqualTo(270)
    }

    @Test
    fun needsHorizontalFlip_forMirroredOrientations() {
        assertThat(BitmapSizing.needsHorizontalFlip(2)).isTrue()
        assertThat(BitmapSizing.needsHorizontalFlip(1)).isFalse()
    }
}
