package dev.antomanc.tameki.feature.insights

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.exchange.FakeExchangeRates
import dev.antomanc.tameki.core.model.budget.PaceStatus
import dev.antomanc.tameki.core.model.entity.Budget
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Goal
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class InsightsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private fun viewModel(
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        recurrings: FakeRecurringRepository = FakeRecurringRepository(),
        budgets: FakeBudgetRepository = FakeBudgetRepository(),
        goals: FakeGoalRepository = FakeGoalRepository(),
        categories: FakeCategoryRepository = FakeCategoryRepository(),
        snoozes: FakeSnoozeRepository = FakeSnoozeRepository(),
    ) = InsightsViewModel(
        transactions = transactions,
        recurrings = recurrings,
        budgets = budgets,
        goals = goals,
        categories = categories,
        settings = FakeSettingsRepository(),
        snoozes = snoozes,
        exchangeRates = FakeExchangeRates(),
        clock = clock,
    )

    private suspend fun ReceiveTurbine<InsightsUiState>.loaded(): InsightsUiState {
        while (true) {
            val state = awaitItem()
            if (!state.loading) return state
        }
    }

    @Test
    fun `the hero counts a subscription once even when two insights point at it`() = runTest {
        val recurring = Recurring(
            id = 1,
            type = RecurringType.SUBSCRIPTION,
            name = "Spotify",
            amount = money(1099),
            cycle = Cycle(1, CycleUnit.MONTH),
            startDate = today.minusMonths(12),
            nextDate = today.plusDays(3),
            worth = Worth.UNUSED,
            annualAlternative = money(9990),
        )
        val vm = viewModel(recurrings = FakeRecurringRepository(listOf(recurring)))

        vm.uiState.test {
            val state = loaded()
            assertThat(state.insights.map { it.id }).containsAtLeast("unused-1", "annual-1")
            assertThat(state.totalYearlySaving.minor).isEqualTo(13188)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `snoozing a card hides it and lowers the hero saving`() = runTest {
        val recurring = Recurring(
            id = 1,
            type = RecurringType.SUBSCRIPTION,
            name = "Spotify",
            amount = money(1099),
            cycle = Cycle(1, CycleUnit.MONTH),
            startDate = today.minusMonths(12),
            nextDate = today.plusDays(3),
            worth = Worth.RARELY,
        )
        val snoozes = FakeSnoozeRepository()
        val vm = viewModel(recurrings = FakeRecurringRepository(listOf(recurring)), snoozes = snoozes)

        vm.uiState.test {
            val visible = loaded()
            assertThat(visible.insights.map { it.id }).contains("unused-1")
            assertThat(visible.totalYearlySaving.minor).isEqualTo(13188)

            vm.snooze("unused-1")
            val hidden = loaded()
            assertThat(hidden.insights.map { it.id }).doesNotContain("unused-1")
            assertThat(hidden.totalYearlySaving.isZero).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(snoozes.snoozed.value.keys).containsExactly("unused-1")
    }

    @Test
    fun `a snoozed insight returns after its date passes`() = runTest {
        val recurring = Recurring(
            id = 1,
            type = RecurringType.SUBSCRIPTION,
            name = "Spotify",
            amount = money(1099),
            cycle = Cycle(1, CycleUnit.MONTH),
            startDate = today.minusMonths(12),
            nextDate = today.plusDays(3),
            worth = Worth.RARELY,
        )
        val snoozes = FakeSnoozeRepository()
        snoozes.snooze("unused-1", today.minusDays(1))
        val vm = viewModel(recurrings = FakeRecurringRepository(listOf(recurring)), snoozes = snoozes)

        vm.uiState.test {
            assertThat(loaded().insights.map { it.id }).contains("unused-1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `budgets carry spent, expected and pace status`() = runTest {
        val budget = Budget(
            id = 1,
            name = "Alimentari",
            categoryIds = setOf(1),
            limit = money(35000),
            period = BudgetPeriod.MONTH,
        )
        val transactions = FakeTransactionRepository(
            listOf(
                expense(1, 31240, LocalDate.of(2026, 11, 5), categoryId = 1),
                expense(2, 9999, LocalDate.of(2026, 11, 6), categoryId = 2),
            ),
        )
        val vm = viewModel(
            transactions = transactions,
            budgets = FakeBudgetRepository(listOf(budget)),
            categories = FakeCategoryRepository(listOf(category(1, "Alimentari", "groceries"), category(2, "Casa", "home"))),
        )

        vm.uiState.test {
            val row = loaded().budgets.single()
            assertThat(row.spent.minor).isEqualTo(31240)
            assertThat(row.expected.minor).isEqualTo(21000)
            assertThat(row.status).isEqualTo(PaceStatus.AT_RISK)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saving a budget writes the chosen fields`() = runTest {
        val budgets = FakeBudgetRepository()
        val vm = viewModel(budgets = budgets)

        vm.uiState.test {
            loaded()
            vm.saveBudget(null, "Svago", setOf(14), money(12000), BudgetPeriod.WEEK, rollover = true)
            awaitBudget()
            cancelAndIgnoreRemainingEvents()
        }
        val saved = budgets.items.value.single()
        assertThat(saved.name).isEqualTo("Svago")
        assertThat(saved.limit.minor).isEqualTo(12000)
        assertThat(saved.period).isEqualTo(BudgetPeriod.WEEK)
        assertThat(saved.rollover).isTrue()
    }

    @Test
    fun `deleting a budget offers Undo that puts it back`() = runTest {
        val budget = Budget(
            id = 1,
            name = "Alimentari",
            categoryIds = setOf(1),
            limit = money(35000),
            period = BudgetPeriod.MONTH,
        )
        val budgets = FakeBudgetRepository(listOf(budget))
        val vm = viewModel(budgets = budgets)

        vm.uiState.test {
            loaded()
            vm.deleteBudget(1)
            val afterDelete = awaitDelete(1)
            assertThat(afterDelete.budgets).isEmpty()
            assertThat(budgets.items.value).isEmpty()

            vm.undoDeleteBudget()
            val restored = awaitBudget()
            assertThat(restored.name).isEqualTo("Alimentari")
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(budgets.items.value.single().limit.minor).isEqualTo(35000)
    }

    @Test
    fun `deleting a goal hides it and Undo restores the same row`() = runTest {
        val goals = FakeGoalRepository(
            listOf(Goal(1, "Sicilia", target = money(120000), saved = money(68000), icon = "travel")),
        )
        val vm = viewModel(goals = goals)

        vm.uiState.test {
            loaded()
            vm.deleteGoal(1)
            var afterDelete = awaitDelete(1)
            // The generation and the archived goal travel on separate flows; wait for both.
            while (afterDelete.goals.isNotEmpty()) afterDelete = awaitItem()
            assertThat(goals.items.value.single().archived).isTrue()

            vm.undoDelete()
            val restored = awaitGoalTarget(120000)
            assertThat(restored.saved.minor).isEqualTo(68000)
            assertThat(goals.items.value.single().archived).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `acknowledging a budget delete drops its Undo snapshot`() = runTest {
        val budget = Budget(
            id = 1,
            name = "Svago",
            categoryIds = emptySet(),
            limit = money(12000),
            period = BudgetPeriod.MONTH,
        )
        val budgets = FakeBudgetRepository(listOf(budget))
        val vm = viewModel(budgets = budgets)

        vm.uiState.test {
            loaded()
            vm.deleteBudget(1)
            awaitDelete(1)
            // The snackbar resolved without Undo: zero the event so a tab return cannot replay it.
            vm.acknowledgeDelete(1)
            awaitAcknowledged()
            // The snapshot is gone too: a delayed Undo must not resurrect the budget.
            vm.undoDeleteBudget()
            assertThat(budgets.items.value).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `adding money to a goal keeps its target and accumulates`() = runTest {
        val goals = FakeGoalRepository(
            listOf(Goal(1, "Sicilia", target = money(120000), saved = money(68000), icon = "travel")),
        )
        val vm = viewModel(goals = goals)

        vm.uiState.test {
            loaded()
            vm.addToGoal(1, money(5000))
            val after = awaitGoalSaved(73000)
            assertThat(after.saved.minor).isEqualTo(73000)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(goals.items.value.single().target.minor).isEqualTo(120000)
    }

    @Test
    fun `editing a goal keeps the money already saved`() = runTest {
        val goals = FakeGoalRepository(
            listOf(Goal(1, "Sicilia", target = money(120000), saved = money(68000), icon = "travel")),
        )
        val vm = viewModel(goals = goals)

        vm.uiState.test {
            loaded()
            vm.saveGoal(1, "Sicilia 2027", money(150000), today.plusMonths(10))
            val after = awaitGoalTarget(150000)
            assertThat(after.saved.minor).isEqualTo(68000)
            assertThat(after.deadline).isEqualTo(today.plusMonths(10))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trends expose six months and this month's categories`() = runTest {
        val transactions = FakeTransactionRepository(
            listOf(
                expense(1, 10000, LocalDate.of(2026, 6, 10), categoryId = 1),
                expense(2, 20000, LocalDate.of(2026, 11, 3), categoryId = 1),
                expense(3, 5000, LocalDate.of(2026, 11, 4), categoryId = 2),
            ),
        )
        val vm = viewModel(
            transactions = transactions,
            categories = FakeCategoryRepository(listOf(category(1, "Alimentari", "groceries"), category(2, "Casa", "home"))),
        )

        vm.uiState.test {
            val state = loaded()
            assertThat(state.months).hasSize(6)
            assertThat(state.months.first().amount.minor).isEqualTo(10000)
            assertThat(state.months.last().amount.minor).isEqualTo(25000)
            assertThat(state.categories.map { it.label }).containsExactly("Alimentari", "Casa").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `transactions in another currency stay out of every total`() = runTest {
        val transactions = FakeTransactionRepository(
            listOf(
                expense(1, 10000, LocalDate.of(2026, 11, 3), categoryId = 1),
                expense(2, 99999, LocalDate.of(2026, 11, 3), categoryId = 1, currency = dev.antomanc.tameki.core.model.CurrencyCode("USD")),
            ),
        )
        val vm = viewModel(
            transactions = transactions,
            categories = FakeCategoryRepository(listOf(category(1, "Alimentari", "groceries"))),
        )

        vm.uiState.test {
            val state = loaded()
            assertThat(state.months.last().amount.minor).isEqualTo(10000)
            assertThat(state.categories.single().amount.minor).isEqualTo(10000)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<InsightsUiState>.awaitBudget(): BudgetUi {
        while (true) {
            val state = awaitItem()
            state.budgets.firstOrNull()?.let { return it }
        }
    }

    private suspend fun ReceiveTurbine<InsightsUiState>.awaitDelete(generation: Long): InsightsUiState {
        while (true) {
            val state = awaitItem()
            if (state.deleteGeneration == generation) return state
        }
    }

    private suspend fun ReceiveTurbine<InsightsUiState>.awaitAcknowledged() {
        while (true) {
            if (awaitItem().deleteGeneration == 0L) return
        }
    }

    private suspend fun ReceiveTurbine<InsightsUiState>.awaitGoalSaved(minor: Long): GoalUi {
        while (true) {
            val state = awaitItem()
            state.goals.firstOrNull { it.saved.minor == minor }?.let { return it }
        }
    }

    private suspend fun ReceiveTurbine<InsightsUiState>.awaitGoalTarget(minor: Long): GoalUi {
        while (true) {
            val state = awaitItem()
            state.goals.firstOrNull { it.target.minor == minor }?.let { return it }
        }
    }
}
