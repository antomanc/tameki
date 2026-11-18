package dev.antomanc.tameki.feature.subscriptions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RecurringDetailViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(repository: FakeRecurringRepository, reminders: FakeReminderScheduler = FakeReminderScheduler()) =
        RecurringDetailViewModel(
            recurrings = repository,
            accounts = FakeAccountRepository(),
            categories = FakeCategoryRepository(),
            reminders = reminders,
            clock = clock,
        )

    @Test
    fun `a family plan shows the whole price and the user's share separately`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(recurring(1, "Famiglia", 1999, today.plusDays(1), sharedWith = 2)),
        )
        val vm = viewModel(repository)
        vm.prepare(1)

        vm.uiState.test {
            val state = skipUntilFound()
            assertThat(state.monthly.minor).isEqualTo(1999)
            assertThat(state.yourMonthly.minor).isEqualTo(1000)
            assertThat(state.yearly.minor).isEqualTo(23988)
            assertThat(state.yourYearly.minor).isEqualTo(12000)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the next three charges follow the cycle`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(
                    1,
                    "Netflix",
                    1399,
                    today.plusDays(1),
                    startDate = today.plusDays(1).minusMonths(12),
                ),
            ),
        )
        val vm = viewModel(repository)
        vm.prepare(1)

        vm.uiState.test {
            val state = skipUntilFound()
            assertThat(state.nextDates).hasSize(3)
            assertThat(state.nextDates[0]).isEqualTo(today.plusDays(1))
            assertThat(state.nextDates[1]).isEqualTo(today.plusMonths(1).plusDays(1))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancelling marks the subscription and drops its reminders`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(recurring(1, "Netflix", 1399, today.plusDays(1))),
        )
        val reminders = FakeReminderScheduler()
        val vm = viewModel(repository, reminders)
        vm.prepare(1)

        vm.uiState.test {
            skipUntilFound()
            vm.cancel()
            val cancelled = skipUntil { it.status == RecurringStatus.CANCELLED }
            assertThat(cancelled.isCancelled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(reminders.cancelled).contains(1L)
        assertThat(reminders.cancelledTrials).contains(1L)
        assertThat(repository.items.value.single().status).isEqualTo(RecurringStatus.CANCELLED)
    }

    @Test
    fun `pausing keeps the amount and can be resumed`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(recurring(1, "Netflix", 1399, today.plusDays(1))),
        )
        val vm = viewModel(repository)
        vm.prepare(1)

        vm.uiState.test {
            skipUntilFound()
            vm.pause()
            val paused = skipUntil { it.status == RecurringStatus.PAUSED }
            assertThat(paused.isPaused).isTrue()
            vm.resume()
            val resumed = skipUntil { it.status == RecurringStatus.ACTIVE }
            assertThat(resumed.isPaused).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rating a subscription is stored as a worth value`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(recurring(1, "Netflix", 1399, today.plusDays(1))),
        )
        val vm = viewModel(repository)
        vm.prepare(1)

        vm.uiState.test {
            skipUntilFound()
            vm.rate(Worth.RARELY)
            val rated = skipUntil { it.worth == Worth.RARELY }
            assertThat(rated.worth).isEqualTo(Worth.RARELY)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repository.items.value.single().worth).isEqualTo(Worth.RARELY)
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<RecurringDetailUiState>.skipUntilFound(): RecurringDetailUiState =
        skipUntil { it.found }

    private suspend fun app.cash.turbine.ReceiveTurbine<RecurringDetailUiState>.skipUntil(
        predicate: (RecurringDetailUiState) -> Boolean,
    ): RecurringDetailUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }
}
