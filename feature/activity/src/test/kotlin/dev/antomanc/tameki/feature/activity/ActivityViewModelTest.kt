package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.exchange.FakeExchangeRates
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.ui.format.TimeOfDay
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Runs under Robolectric: day labels come from the platform's ICU data
 * ([dev.antomanc.tameki.core.ui.format.RelativeDates]), which the plain-JVM android.jar stubs do
 * not implement.
 */
@RunWith(RobolectricTestRunner::class)
class ActivityViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val eur = CurrencyCode("EUR")
    private val usd = CurrencyCode("USD")
    private val jpy = CurrencyCode("JPY")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(transactions: FakeTransactionRepository): ActivityViewModel =
        ActivityViewModel(
            transactions = transactions,
            categories = FakeCategoryRepository(emptyList()),
            accounts = FakeAccountRepository(emptyList()),
            inbox = FakeInboxRepository(),
            settings = FakeSettingsRepository(eur),
            exchangeRates = FakeExchangeRates(),
            clock = clock,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun acknowledgeDeleteZeroesTheEventSoItCannotReplayAndDropsUndo() = runTest {
        val transactions = FakeTransactionRepository(
            mutableMapOf(
                7L to Transaction(
                    id = 7L,
                    kind = TransactionKind.EXPENSE,
                    amount = Money(1_000, eur),
                    accountId = 1L,
                    occurredAt = today.atTime(9, 0),
                ),
            ),
        )
        val vm = viewModel(transactions)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.delete(7L)
        advanceUntilIdle()
        assertThat(transactions.contains(7L)).isFalse()
        assertThat(vm.uiState.value.deleteGeneration).isEqualTo(1L)
        assertThat(vm.uiState.value.pendingDeleteId).isEqualTo(7L)

        // The snackbar was shown and let go: the event must not replay on a tab return or a
        // rotation, and no stale row may come back through a later Undo.
        vm.acknowledgeDelete(1L)
        advanceUntilIdle()
        assertThat(vm.uiState.value.deleteGeneration).isEqualTo(0L)
        assertThat(vm.uiState.value.pendingDeleteId).isNull()

        vm.undoDelete()
        advanceUntilIdle()
        assertThat(transactions.contains(7L)).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun undoDeleteBeforeAcknowledgeRestoresTheRow() = runTest {
        val stored = mutableMapOf(
            7L to Transaction(
                id = 7L,
                kind = TransactionKind.EXPENSE,
                amount = Money(1_000, eur),
                accountId = 1L,
                occurredAt = today.atTime(9, 0),
            ),
        )
        val transactions = FakeTransactionRepository(stored)
        val vm = viewModel(transactions)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.delete(7L)
        advanceUntilIdle()
        vm.undoDelete()
        vm.acknowledgeDelete(1L)
        advanceUntilIdle()

        assertThat(transactions.contains(7L)).isTrue()
        assertThat(vm.uiState.value.deleteGeneration).isEqualTo(0L)
    }

    @Test
    fun buildItemsGroupsByDayWithHeaders() = runTest {
        val cats = listOf(Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 1))
        val accounts = listOf(Account(1, "Carta", eur, AccountType.CARD))
        val txs = listOf(
            Transaction(
                id = 1,
                kind = TransactionKind.EXPENSE,
                amount = Money(1000, eur),
                accountId = 1,
                categoryId = 1,
                merchant = "Esselunga",
                occurredAt = LocalDateTime.of(2026, 11, 18, 10, 0),
            ),
            Transaction(
                id = 2,
                kind = TransactionKind.EXPENSE,
                amount = Money(250, eur),
                accountId = 1,
                categoryId = 1,
                merchant = "Bar",
                occurredAt = LocalDateTime.of(2026, 11, 17, 9, 0),
            ),
        )
        val items = ActivityViewModel.buildItems(
            transactions = txs,
            categories = cats,
            accounts = accounts,
            totals = listOf(
                DayTotal(today, eur, Money(1000, eur), Money.zero(eur)),
                DayTotal(today.minusDays(1), eur, Money(250, eur), Money.zero(eur)),
            ),
            today = today,
            base = eur,
            rates = FakeExchangeRates(),
            locale = Locale.ITALIAN,
        )
        assertThat(items.filterIsInstance<ActivityListItem.Header>()).hasSize(2)
        assertThat(items.filterIsInstance<ActivityListItem.Row>()).hasSize(2)
        assertThat(items.filterIsInstance<ActivityListItem.Header>().map { it.label })
            .containsExactly("Oggi", "Ieri")
            .inOrder()
        assertThat((items.first() as ActivityListItem.Header).net).isEqualTo(Money(1000, eur))
    }

    @Test
    fun buildItemsFallsBackToCategoryAccountNoteThenTime() = runTest {
        val cats = listOf(Category(1, "Alimentari", CategoryKind.EXPENSE, "groceries", 1))
        val accounts = listOf(Account(1, "Carta", eur, AccountType.CARD))
        suspend fun titleOf(transaction: Transaction): String = ActivityViewModel.buildItems(
            transactions = listOf(transaction),
            categories = cats,
            accounts = accounts,
            totals = emptyList(),
            today = today,
            base = eur,
            rates = FakeExchangeRates(),
            locale = Locale.ITALIAN,
        ).filterIsInstance<ActivityListItem.Row>().single().title

        val anonymous = Transaction(
            id = 1,
            kind = TransactionKind.EXPENSE,
            amount = Money(1000, eur),
            accountId = 1,
            occurredAt = LocalDateTime.of(2026, 11, 18, 9, 5),
        )
        // No merchant: the category still names the row, and it beats the account.
        assertThat(titleOf(anonymous.copy(categoryId = 1))).isEqualTo("Alimentari")
        // Without a category the account comes next, before the note.
        assertThat(titleOf(anonymous.copy(note = "Caffè"))).isEqualTo("Carta")
        // ...and a row with no known category, account, note or merchant shows its time,
        // never a bare dash.
        val bare = ActivityViewModel.buildItems(
            transactions = listOf(anonymous.copy(accountId = 99)),
            categories = cats,
            accounts = accounts,
            totals = emptyList(),
            today = today,
            base = eur,
            rates = FakeExchangeRates(),
            locale = Locale.ITALIAN,
        ).filterIsInstance<ActivityListItem.Row>().single()
        assertThat(bare.title).isNotEmpty()
        assertThat(bare.title).isNotEqualTo("—")
    }

    @Test
    fun buildItemsConvertsForeignDayTotalsAndKeepsUnconverted() = runTest {
        val rates = FakeExchangeRates()
        rates.setManualRate(usd, eur, "0.9", today)
        val header = ActivityViewModel.buildItems(
            transactions = listOf(
                Transaction(
                    id = 1,
                    kind = TransactionKind.EXPENSE,
                    amount = Money(1000, eur),
                    accountId = 1,
                    occurredAt = LocalDateTime.of(2026, 11, 18, 10, 0),
                ),
            ),
            categories = emptyList(),
            accounts = listOf(Account(1, "Cash", eur, AccountType.CASH)),
            totals = listOf(
                DayTotal(today, eur, Money(1000, eur), Money.zero(eur)),
                DayTotal(today, usd, Money(1250, usd), Money.zero(usd)),
                DayTotal(today, jpy, Money(500, jpy), Money.zero(jpy)),
            ),
            today = today,
            base = eur,
            rates = rates,
            locale = Locale.ITALIAN,
        ).filterIsInstance<ActivityListItem.Header>().single()
        // 10,00 EUR + 12,50 USD × 0,9 = 10,00 + 11,25 = 21,25
        assertThat(header.net).isEqualTo(Money(2125, eur))
        assertThat(header.unconverted).containsExactly(Money(500, jpy))
    }

    @Test
    fun rowMetaCarriesAccountAndTime() = runTest {
        val accounts = listOf(Account(1, "Carta", eur, AccountType.CARD))
        val tx = Transaction(
            id = 1,
            kind = TransactionKind.EXPENSE,
            amount = Money(1000, eur),
            accountId = 1,
            merchant = "Esselunga",
            occurredAt = LocalDateTime.of(2026, 11, 18, 18, 42),
        )
        val row = ActivityViewModel.buildItems(
            transactions = listOf(tx),
            categories = emptyList(),
            accounts = accounts,
            totals = emptyList(),
            today = today,
            base = eur,
            rates = FakeExchangeRates(),
            locale = Locale.ITALIAN,
        ).filterIsInstance<ActivityListItem.Row>().single()
        assertThat(row.meta).isEqualTo("Carta · ${TimeOfDay.short(tx.occurredAt, Locale.ITALIAN)}")
    }
}
