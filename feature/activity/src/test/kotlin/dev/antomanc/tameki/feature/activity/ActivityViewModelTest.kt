package dev.antomanc.tameki.feature.activity

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.exchange.FakeExchangeRates
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.InboxItem
import dev.antomanc.tameki.core.model.entity.InboxStatus
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.DayTotal
import dev.antomanc.tameki.core.ui.format.TimeOfDay
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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

    private fun viewModel(
        transactions: FakeTransactionRepository,
        clock: Clock = this.clock,
    ): ActivityViewModel =
        ActivityViewModel(
            transactions = transactions,
            categories = FakeCategoryRepository(emptyList()),
            accounts = FakeAccountRepository(emptyList()),
            inbox = FakeInboxRepository(),
            settings = FakeSettingsRepository(eur),
            exchangeRates = FakeExchangeRates(),
            clock = clock,
        )

    /** A clock the test moves by hand: the midnight rollover is a fact about time, not the device. */
    private class MutableClock(var local: LocalDateTime) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = local.toInstant(ZoneOffset.UTC)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a period preset keeps its meaning after midnight`() = runTest {
        val clock = MutableClock(LocalDateTime.of(2026, 11, 30, 23, 40))
        val vm = viewModel(FakeTransactionRepository(), clock)
        val keys = mutableListOf<ActivityViewModel.PagingKey>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.pagingKeys.collect { keys += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.applyFilters(ActivityFilters(period = ActivityPeriod.THIS_MONTH))
        advanceUntilIdle()
        assertThat(keys.last().day).isEqualTo(LocalDate.of(2026, 11, 30))
        assertThat(keys.last().filter.from).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(keys.last().filter.to).isEqualTo(LocalDate.of(2026, 11, 30))

        // Midnight passes while the filter still says "this month".
        clock.local = LocalDateTime.of(2026, 12, 1, 8, 0)
        vm.refreshToday()
        advanceUntilIdle()

        // The chip is still selected and the query follows the clock: December, not yesterday.
        // Before the preset was stored, the chip deselected while the repository kept cutting at
        // 30 November, so the list looked correctly empty and was simply wrong.
        assertThat(vm.uiState.value.filters.period).isEqualTo(ActivityPeriod.THIS_MONTH)
        assertThat(keys.last().day).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(keys.last().filter.from).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(keys.last().filter.to).isEqualTo(LocalDate.of(2026, 12, 1))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun categoryFilterFromAnotherScreenIsApplied() = runTest {
        val transactions = FakeTransactionRepository(mutableMapOf())
        val vm = viewModel(transactions)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.setCategoryFilter(7L)
        advanceUntilIdle()

        assertThat(vm.uiState.value.filters.categoryId).isEqualTo(7L)
    }

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
        val token = checkNotNull(vm.uiState.value.notice).token
        assertThat(vm.uiState.value.notice).isInstanceOf(ActivityNotice.Deleted::class.java)

        // The snackbar was shown and let go: the event must not replay on a tab return or a
        // rotation, and no stale row may come back through a later Undo.
        vm.acknowledgeNotice(token)
        advanceUntilIdle()
        assertThat(vm.uiState.value.notice).isNull()

        vm.undo(token)
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
        val token = checkNotNull(vm.uiState.value.notice).token
        vm.undo(token)
        vm.acknowledgeNotice(token)
        advanceUntilIdle()

        assertThat(transactions.contains(7L)).isTrue()
        assertThat(vm.uiState.value.notice).isNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a merchant edit refreshes the pager key even when day totals do not change`() = runTest {
        val stored = Transaction(
            id = 7L,
            kind = TransactionKind.EXPENSE,
            amount = Money(1_000, eur),
            accountId = 1L,
            merchant = "Esselunga",
            occurredAt = today.atTime(9, 0),
        )
        val transactions = FakeTransactionRepository(mutableMapOf(7L to stored))
        val vm = viewModel(transactions)
        val keys = mutableListOf<ActivityViewModel.PagingKey>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.pagingKeys.collect { keys += it }
        }
        advanceUntilIdle()
        assertThat(keys.map { key -> key.transactions.single().merchant })
            .containsExactly("Esselunga")

        // Same amount and day: filters and day totals are identical, so a key made only of those
        // left the one-shot PagingSource holding the old title. The rows are now part of the key.
        transactions.upsert(stored.copy(merchant = "Esselunga Market"))
        advanceUntilIdle()

        assertThat(keys).hasSize(2)
        assertThat(keys.last().transactions.single().merchant).isEqualTo("Esselunga Market")
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
        // Rows show an expense as "−" and income as "+": the header follows the same sign.
        assertThat((items.first() as ActivityListItem.Header).net).isEqualTo(Money(-1000, eur))
    }

    @Test
    fun buildItemsHeaderAddsIncomeAndSubtractsExpense() = runTest {
        val items = ActivityViewModel.buildItems(
            transactions = listOf(
                Transaction(
                    id = 1,
                    kind = TransactionKind.EXPENSE,
                    amount = Money(1000, eur),
                    accountId = 1,
                    occurredAt = LocalDateTime.of(2026, 11, 18, 10, 0),
                ),
                Transaction(
                    id = 2,
                    kind = TransactionKind.INCOME,
                    amount = Money(2500, eur),
                    accountId = 1,
                    occurredAt = LocalDateTime.of(2026, 11, 18, 11, 0),
                ),
            ),
            categories = emptyList(),
            accounts = emptyList(),
            totals = listOf(
                DayTotal(today, eur, Money(1000, eur), Money(2500, eur)),
            ),
            today = today,
            base = eur,
            rates = FakeExchangeRates(),
            locale = Locale.ITALIAN,
        )
        assertThat((items.first() as ActivityListItem.Header).net).isEqualTo(Money(1500, eur))
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
        // 10,00 EUR + 12,50 USD × 0,9 = 10,00 + 11,25 = 21,25 spent, so the net is negative.
        assertThat(header.net).isEqualTo(Money(-2125, eur))
        // The day's net is negative (spending), so the unconverted line keeps that sign too.
        assertThat(header.unconverted).containsExactly(Money(-500, jpy))
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

    @Test
    fun `an inbox amount is confirmed in one tap and undo restores it`() = runTest {
        val item = InboxItem(
            id = 5,
            draft = TransactionDraft(
                kind = TransactionKind.EXPENSE,
                amount = Money(1250, eur),
                merchant = "Bar Centrale",
                categoryHint = "coffee",
                occurredAt = LocalDateTime.of(2026, 11, 18, 9, 0),
            ),
            origin = TransactionSource.NOTIFICATION,
            rawText = "Pagamento 12,50 EUR",
            receivedAt = Instant.parse("2026-11-18T08:00:00Z"),
        )
        val transactions = FakeTransactionRepository()
        val inbox = FakeInboxRepository(listOf(item))
        val vm = ActivityViewModel(
            transactions = transactions,
            categories = FakeCategoryRepository(
                listOf(Category(3, "Caffè", CategoryKind.EXPENSE, "coffee", 1)),
            ),
            accounts = FakeAccountRepository(listOf(Account(1, "Carta", eur, AccountType.CARD))),
            inbox = inbox,
            settings = FakeSettingsRepository(eur),
            exchangeRates = FakeExchangeRates(),
            clock = clock,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertThat(vm.confirmInbox(item)).isTrue()
        // A second tap while the first is being written is the same confirm, not a second booking.
        assertThat(vm.confirmInbox(item)).isTrue()
        advanceUntilIdle()
        assertThat(inbox.statuses[5]).isEqualTo(InboxStatus.ACCEPTED)
        assertThat(transactions.contains(2)).isFalse()
        val booked = transactions.get(1)
        assertThat(booked?.amount).isEqualTo(Money(1250, eur))
        assertThat(booked?.accountId).isEqualTo(1)
        // The hint names the category the capture already knew: no guessing by layout.
        assertThat(booked?.categoryId).isEqualTo(3)
        assertThat(booked?.source).isEqualTo(TransactionSource.NOTIFICATION)

        vm.undo(checkNotNull(vm.uiState.value.notice).token)
        advanceUntilIdle()
        assertThat(inbox.statuses[5]).isEqualTo(InboxStatus.PENDING)
        assertThat(transactions.contains(1)).isFalse()
    }
}
