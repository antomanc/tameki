package dev.antomanc.tameki.core.ui.format

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Under Robolectric because the labels come from the platform's ICU data; the plain-JVM
 * android.jar stubs do not implement [android.icu.text.RelativeDateTimeFormatter].
 */
@RunWith(RobolectricTestRunner::class)
class RelativeDatesTest {
    @Test
    fun italianDayWordsFollowTheLocale() {
        assertThat(RelativeDates.today(Locale.ITALIAN)).isEqualTo("Oggi")
        assertThat(RelativeDates.yesterday(Locale.ITALIAN)).isEqualTo("Ieri")
        assertThat(RelativeDates.tomorrow(Locale.ITALIAN)).isEqualTo("Domani")
        assertThat(RelativeDates.inDays(3, Locale.ITALIAN)).isEqualTo("Tra 3 giorni")
    }

    @Test
    fun englishDayWordsFollowTheLocale() {
        assertThat(RelativeDates.today(Locale.US)).isEqualTo("Today")
        assertThat(RelativeDates.yesterday(Locale.US)).isEqualTo("Yesterday")
        assertThat(RelativeDates.inDays(3, Locale.US)).isEqualTo("In 3 days")
    }

    @Test
    fun singularDayReadsAsOneDay() {
        assertThat(RelativeDates.inDays(1, Locale.US)).isEqualTo("Tomorrow")
        assertThat(RelativeDates.inDays(0, Locale.ITALIAN)).isEqualTo("Oggi")
    }
}
