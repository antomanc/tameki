package dev.antomanc.tameki.feature.subscriptions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SubscriptionsReviewViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(repository: FakeRecurringRepository, reminders: FakeReminderScheduler = FakeReminderScheduler()) =
        SubscriptionsReviewViewModel(
            recurrings = repository,
            reminders = reminders,
            settings = FakeSettingsRepository(),
            clock = clock,
        )

    @Test
    fun `the queue is active subscriptions, the most expensive first`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(1, "Netflix", 1399, today.plusDays(1)),
                recurring(2, "Palestra", 3900, today.plusDays(4)),
                recurring(3, "Affitto", 65000, today.plusDays(2), type = RecurringType.BILL),
                recurring(4, "Vecchio", 999, today.plusDays(5), status = RecurringStatus.CANCELLED),
            ),
        )
        val vm = viewModel(repository)
        vm.start()

        vm.uiState.test {
            val state = awaitUntil { !it.loading }
            assertThat(state.items.map { it.name }).containsExactly("Palestra", "Netflix").inOrder()
            assertThat(state.current?.name).isEqualTo("Palestra")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the queue holds only subscriptions whose rating is missing or stale`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(
                    1,
                    "Netflix",
                    1399,
                    today.plusDays(1),
                    worth = Worth.LOVE,
                    worthRatedOn = today.minusDays(120),
                ),
                recurring(
                    2,
                    "Palestra",
                    3900,
                    today.plusDays(4),
                    worth = Worth.LOVE,
                    worthRatedOn = today.minusDays(30),
                ),
                recurring(3, "Spotify", 1099, today.plusDays(3)),
            ),
        )
        val vm = viewModel(repository)
        vm.start()

        vm.uiState.test {
            val state = awaitUntil { !it.loading }
            assertThat(state.items.map { it.name }).containsExactly("Netflix", "Spotify").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keep and rarely use store a rating, cancel stops the subscription`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(1, "Netflix", 1399, today.plusDays(1)),
                recurring(2, "Palestra", 3900, today.plusDays(4)),
            ),
        )
        val reminders = FakeReminderScheduler()
        val vm = viewModel(repository, reminders)
        vm.start()

        vm.uiState.test {
            awaitUntil { !it.loading && it.current != null }
            vm.choose(ReviewDecision.CANCEL)
            advanceUntilIdle()
            val afterCancel = awaitUntil { it.cancelled == 1 }
            assertThat(afterCancel.current?.name).isEqualTo("Netflix")

            vm.choose(ReviewDecision.KEEP)
            advanceUntilIdle()
            val finished = awaitUntil { it.finished }
            assertThat(finished.kept).isEqualTo(1)
            assertThat(finished.yearlySaving.minor).isEqualTo(46800)
            cancelAndIgnoreRemainingEvents()
        }
        val byName = repository.items.value.associateBy { it.name }
        assertThat(byName.getValue("Palestra").status).isEqualTo(RecurringStatus.CANCELLED)
        assertThat(byName.getValue("Palestra").worth).isEqualTo(Worth.UNUSED)
        assertThat(byName.getValue("Palestra").worthRatedOn).isEqualTo(today)
        assertThat(byName.getValue("Netflix").worth).isEqualTo(Worth.LOVE)
        assertThat(byName.getValue("Netflix").worthRatedOn).isEqualTo(today)
        assertThat(reminders.cancelled).contains(2L)
    }

    @Test
    fun `rarely used keeps the subscription running with a quieter rating`() = runTest {
        val repository = FakeRecurringRepository(listOf(recurring(1, "Netflix", 1399, today.plusDays(1))))
        val vm = viewModel(repository)
        vm.start()

        vm.uiState.test {
            awaitUntil { !it.loading && it.current != null }
            vm.choose(ReviewDecision.RARELY)
            advanceUntilIdle()
            val finished = awaitUntil { it.finished }
            assertThat(finished.rarely).isEqualTo(1)
            assertThat(finished.cancelled).isEqualTo(0)
            assertThat(finished.yearlySaving.isZero).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        val stored = repository.items.value.single()
        assertThat(stored.status).isEqualTo(RecurringStatus.ACTIVE)
        assertThat(stored.worth).isEqualTo(Worth.RARELY)
        assertThat(stored.worthRatedOn).isEqualTo(today)
    }

    @Test
    fun `an empty queue finishes immediately without inventing a saving`() = runTest {
        val vm = viewModel(FakeRecurringRepository())
        vm.start()

        vm.uiState.test {
            val state = awaitUntil { !it.loading }
            assertThat(state.items).isEmpty()
            assertThat(state.finished).isFalse()
            assertThat(state.yearlySaving.isZero).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<SubscriptionsReviewUiState>.awaitUntil(
        predicate: (SubscriptionsReviewUiState) -> Boolean,
    ): SubscriptionsReviewUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }
}
