package dev.antomanc.tameki.feature.subscriptions

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SubscriptionsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private suspend fun ReceiveTurbine<SubscriptionsUiState>.loaded(): SubscriptionsUiState {
        while (true) {
            val state = awaitItem()
            if (!state.loading) return state
        }
    }

    @Test
    fun `monthly total normalises cycles and leaves income out of the cost`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(1, "Netflix", 1399, today.plusDays(1)),
                recurring(2, "Spotify", 12000, today.plusDays(3), cycle = Cycle(1, CycleUnit.YEAR)),
                recurring(3, "Stipendio", 215000, today.plusDays(5), type = RecurringType.INCOME),
            ),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            val state = loaded()
            assertThat(state.totalMonthly.minor).isEqualTo(1399L + 1000L)
            assertThat(state.totalYearly.minor).isEqualTo(16788L + 12000L)
            assertThat(state.subscriptionCount).isEqualTo(2)
            assertThat(state.incomeCount).isEqualTo(1)
            assertThat(state.rows.map { it.name }).containsExactly("Netflix", "Spotify", "Stipendio").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rows are sorted by next charge and the strip marks renewal days`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(1, "Palestra", 3900, today.plusDays(12)),
                recurring(2, "Netflix", 1399, today.plusDays(1)),
            ),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            val state = loaded()
            assertThat(state.rows.map { it.name }).containsExactly("Netflix", "Palestra").inOrder()
            assertThat(state.days).hasSize(30)
            assertThat(state.days.count { it.hasRenewal }).isEqualTo(2)
            assertThat(state.days.first().isToday).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping a day filters the list and tapping it again clears the filter`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(1, "Netflix", 1399, today.plusDays(3)),
                recurring(2, "Palestra", 3900, today.plusDays(12)),
            ),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            loaded()
            viewModel.selectDay(today.plusDays(12))
            val filtered = awaitItem()
            assertThat(filtered.rows.map { it.name }).containsExactly("Palestra")

            viewModel.selectDay(today.plusDays(12))
            val cleared = awaitItem()
            assertThat(cleared.rows).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the yearly toggle swaps the hero total`() = runTest {
        val repository = FakeRecurringRepository(listOf(recurring(1, "Netflix", 1399, today.plusDays(1))))
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            val monthly = loaded()
            viewModel.togglePeriod()
            val yearly = awaitItem()
            assertThat(yearly.period).isEqualTo(CostPeriod.YEAR)
            assertThat(yearly.total).isEqualTo(yearly.totalYearly)
            assertThat(yearly.total.minor).isEqualTo(monthly.totalMonthly.minor * 12)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a paused subscription appears only under the paused filter`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(1, "Netflix", 1399, today.plusDays(1)),
                recurring(2, "Palestra", 3900, today.plusDays(4), status = RecurringStatus.PAUSED),
            ),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            assertThat(loaded().rows.map { it.name }).containsExactly("Netflix")
            viewModel.setStatusFilter(StatusFilter.PAUSED)
            assertThat(awaitItem().rows.map { it.name }).containsExactly("Palestra")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a subscription shared with two people counts only the user's half`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(recurring(1, "Famiglia", 1999, today.plusDays(1), sharedWith = 2)),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            assertThat(loaded().totalMonthly.minor).isEqualTo(1000)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the review entry waits for at least one unrated subscription`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(
                    1,
                    "Netflix",
                    1399,
                    today.plusDays(1),
                    worth = Worth.RARELY,
                    worthRatedOn = today.minusDays(10),
                ),
                recurring(
                    2,
                    "Spotify",
                    1099,
                    today.plusDays(3),
                    worth = Worth.LOVE,
                    worthRatedOn = today.minusDays(20),
                ),
            ),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            assertThat(loaded().canReview).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a rating older than ninety days brings the review entry back`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(
                    1,
                    "Netflix",
                    1399,
                    today.plusDays(1),
                    worth = Worth.LOVE,
                    worthRatedOn = today.minusDays(91),
                ),
                recurring(
                    2,
                    "Spotify",
                    1099,
                    today.plusDays(3),
                    worth = Worth.LOVE,
                    worthRatedOn = today.minusDays(20),
                ),
            ),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            assertThat(loaded().canReview).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a trial is active for counts, totals and the review entry`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(recurring(1, "Disney+", 899, today.plusDays(2), status = RecurringStatus.TRIAL)),
        )
        val viewModel = SubscriptionsViewModel(repository, FakeSettingsRepository(), clock)

        viewModel.uiState.test {
            val state = loaded()
            assertThat(state.subscriptionCount).isEqualTo(1)
            assertThat(state.totalMonthly.minor).isEqualTo(899)
            assertThat(state.canReview).isTrue()
            assertThat(state.rows.single().flags).contains(RecurringFlag.TRIAL_ENDING)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
