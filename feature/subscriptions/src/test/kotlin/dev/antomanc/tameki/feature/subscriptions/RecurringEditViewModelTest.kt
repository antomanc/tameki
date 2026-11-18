package dev.antomanc.tameki.feature.subscriptions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.PricePoint
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RecurringEditViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val catalog = BrandCatalog(
        listOf(Brand("netflix", "Netflix", "#E50914", "streaming", cancelUrl = "https://netflix.example/cancel")),
    )

    private fun viewModel(repository: FakeRecurringRepository, reminders: FakeReminderScheduler = FakeReminderScheduler()) =
        RecurringEditViewModel(
            recurrings = repository,
            accounts = FakeAccountRepository(),
            categories = FakeCategoryRepository(),
            settings = FakeSettingsRepository(),
            reminders = reminders,
            catalog = catalog,
            clock = clock,
        )

    @Test
    fun `a new recurring starts today and saves when name and amount are set`() = runTest {
        val repository = FakeRecurringRepository()
        val reminders = FakeReminderScheduler()
        val vm = viewModel(repository, reminders)

        vm.prepare(null)
        vm.uiState.test {
            val loaded = awaitUntil { !it.loading }
            assertThat(loaded.isNew).isTrue()
            assertThat(loaded.startDate).isEqualTo(today)
            assertThat(loaded.nextDate).isEqualTo(today)

            vm.setName("Spotify")
            vm.setAmount(Money(1099, CurrencyCode("EUR")))
            val ready = awaitUntil { it.canSave }
            assertThat(ready.canSave).isTrue()

            vm.save()
            awaitUntil { it.saved }
            cancelAndIgnoreRemainingEvents()
        }
        val stored = repository.items.value.single()
        assertThat(stored.name).isEqualTo("Spotify")
        assertThat(stored.amount.minor).isEqualTo(1099)
        assertThat(stored.priceHistory).isEmpty()
        assertThat(reminders.scheduled).hasSize(1)
    }

    @Test
    fun `changing the amount appends a price point dated today`() = runTest {
        val existing = recurring(
            id = 7,
            name = "Netflix",
            minor = 1399,
            nextDate = today.plusDays(3),
            priceHistory = listOf(PricePoint(Money(1099, CurrencyCode("EUR")), LocalDate.of(2024, 3, 1))),
        )
        val repository = FakeRecurringRepository(listOf(existing))
        val vm = viewModel(repository)

        vm.prepare(7)
        vm.uiState.test {
            awaitUntil { !it.loading && !it.isNew }
            vm.setAmount(Money(1499, CurrencyCode("EUR")))
            awaitUntil { it.amount?.minor == 1499L }
            vm.save()
            awaitUntil { it.saved }
            cancelAndIgnoreRemainingEvents()
        }
        val stored = repository.items.value.single()
        assertThat(stored.priceHistory.map { it.amount.minor }).containsExactly(1099L, 1499L).inOrder()
        assertThat(stored.priceHistory.last().since).isEqualTo(today)
    }

    @Test
    fun `keeping the same amount does not add a price point`() = runTest {
        val existing = recurring(id = 7, name = "Netflix", minor = 1399, nextDate = today.plusDays(3))
        val repository = FakeRecurringRepository(listOf(existing))
        val vm = viewModel(repository)

        vm.prepare(7)
        vm.uiState.test {
            awaitUntil { !it.loading && !it.isNew }
            vm.setName("Netflix Premium")
            vm.save()
            awaitUntil { it.saved }
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repository.items.value.single().priceHistory).isEmpty()
    }

    @Test
    fun `typing a known name suggests the brand and fills category and cancel link`() = runTest {
        val vm = viewModel(FakeRecurringRepository())
        vm.prepare(null)
        vm.uiState.test {
            awaitUntil { !it.loading }
            vm.setName("Netf")
            val suggested = awaitUntil { it.suggestions.isNotEmpty() }
            assertThat(suggested.suggestions.single().key).isEqualTo("netflix")

            vm.selectBrand(suggested.suggestions.single())
            val filled = awaitUntil { it.brandKey == "netflix" }
            assertThat(filled.name).isEqualTo("Netflix")
            assertThat(filled.cancelUrl).isEqualTo("https://netflix.example/cancel")
            assertThat(filled.suggestions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a saved form can be reopened from the repository unchanged`() = runTest {
        val repository = FakeRecurringRepository(
            listOf(
                recurring(
                    id = 1,
                    name = "Palestra",
                    minor = 3900,
                    nextDate = today.plusDays(10),
                    cycle = Cycle(3, CycleUnit.MONTH),
                ),
            ),
        )
        val vm = viewModel(repository)
        vm.prepare(1)
        vm.uiState.test {
            val state = awaitUntil { !it.loading }
            assertThat(state.cycleEvery).isEqualTo(3)
            assertThat(state.cycleUnit).isEqualTo(CycleUnit.MONTH)
            cancelAndIgnoreRemainingEvents()
        }
        // The fake's get is also part of the contract used by the detail screen.
        assertThat(repository.get(1)?.name).isEqualTo("Palestra")
        assertThat(repository.observeAll().first().size).isEqualTo(1)
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<RecurringEditUiState>.awaitUntil(
        predicate: (RecurringEditUiState) -> Boolean,
    ): RecurringEditUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }
}
