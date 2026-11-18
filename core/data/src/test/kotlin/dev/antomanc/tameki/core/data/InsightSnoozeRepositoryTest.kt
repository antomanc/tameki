package dev.antomanc.tameki.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.snooze.InsightSnoozeRepositoryImpl
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Snoozing is invisible work: ids stay away exactly as long as the user asked, then come back. */
@RunWith(RobolectricTestRunner::class)
class InsightSnoozeRepositoryTest {
    private val today = LocalDate.of(2026, 11, 18)

    private fun repository(): InsightSnoozeRepositoryImpl {
        val file = File.createTempFile("insight-snooze", ".preferences_pb").also { it.delete() }
        val store = PreferenceDataStoreFactory.create(produceFile = { file })
        return InsightSnoozeRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            dataStore = store,
        )
    }

    @Test
    fun `a snoozed insight is hidden until its date passes`() = runTest {
        val repository = repository()
        repository.snooze("unused-1", today.plusDays(30))

        repository.observeSnoozed(today).test {
            assertThat(awaitItem()).containsExactly("unused-1")
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeSnoozed(today.plusDays(29)).test {
            assertThat(awaitItem()).containsExactly("unused-1")
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeSnoozed(today.plusDays(30)).test {
            assertThat(awaitItem()).containsExactly("unused-1")
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeSnoozed(today.plusDays(31)).test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `snoozing again replaces the previous date instead of piling up`() = runTest {
        val repository = repository()
        repository.snooze("unused-1", today.plusDays(1))
        repository.snooze("unused-1", today.plusDays(30))

        repository.observeSnoozed(today.plusDays(10)).test {
            assertThat(awaitItem()).containsExactly("unused-1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing brings the insight back immediately`() = runTest {
        val repository = repository()
        repository.snooze("unused-1", today.plusDays(30))
        repository.snooze("unused-2", today.plusDays(30))
        repository.clear("unused-1")

        repository.observeSnoozed(today).test {
            assertThat(awaitItem()).containsExactly("unused-2")
            cancelAndIgnoreRemainingEvents()
        }
    }

}
