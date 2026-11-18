package dev.antomanc.tameki.store

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.PaceStatus
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.BudgetPeriod
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.insight.Insight
import dev.antomanc.tameki.core.ui.components.RecurringFlag
import dev.antomanc.tameki.feature.activity.ActivityListItem
import dev.antomanc.tameki.feature.activity.ActivityUiState
import dev.antomanc.tameki.feature.activity.AmountDraft
import dev.antomanc.tameki.feature.activity.QuickAddUiState
import dev.antomanc.tameki.feature.home.AvailableToSpend
import dev.antomanc.tameki.feature.home.HeroMode
import dev.antomanc.tameki.feature.home.HomeInsight
import dev.antomanc.tameki.feature.home.HomeRenewal
import dev.antomanc.tameki.feature.home.HomeTransaction
import dev.antomanc.tameki.feature.home.HomeUiState
import dev.antomanc.tameki.feature.insights.BudgetUi
import dev.antomanc.tameki.feature.insights.CategoryOptionUi
import dev.antomanc.tameki.feature.insights.CategorySpend
import dev.antomanc.tameki.feature.insights.GoalUi
import dev.antomanc.tameki.feature.insights.InsightAction
import dev.antomanc.tameki.feature.insights.InsightCardUi
import dev.antomanc.tameki.feature.insights.InsightKind
import dev.antomanc.tameki.feature.insights.InsightsUiState
import dev.antomanc.tameki.feature.insights.MonthSpend
import dev.antomanc.tameki.feature.subscriptions.Brand
import dev.antomanc.tameki.feature.subscriptions.BrandCatalog
import dev.antomanc.tameki.feature.subscriptions.CostPeriod
import dev.antomanc.tameki.feature.subscriptions.RecurringRowUi
import dev.antomanc.tameki.feature.subscriptions.StatusFilter
import dev.antomanc.tameki.feature.subscriptions.StripDay
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsUiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Coherent mid-November 2026 demo ledger for Play Store screenshots. */
enum class StoreLocale {
    IT,
    EN,
}

private val eur = CurrencyCode("EUR")
private fun money(minor: Long) = Money(minor, eur)
private val today: LocalDate = LocalDate.of(2026, 11, 18)

internal fun storeCatalog(): BrandCatalog = BrandCatalog(
    listOf(
        Brand("netflix", "Netflix", "#E50914", "streaming"),
        Brand("spotify", "Spotify", "#1DB954", "music"),
        Brand("icloud", "iCloud+", "#0071E3", "home"),
        Brand("gym", "McFit", "#FFD400", "sport"),
        Brand("insurance", "Generali", "#CC0000", "insurance", type = "BILL"),
        Brand("tim", "TIM", "#0057B8", "phone", type = "BILL"),
        Brand("enel", "Enel Energia", "#E4002B", "utilities", type = "BILL"),
    ),
)

internal fun storeHomeState(locale: StoreLocale): HomeUiState {
    val it = locale == StoreLocale.IT
    return HomeUiState(
        loading = false,
        baseCurrency = eur,
        hero = AvailableToSpend(
            amount = money(42850),
            mode = HeroMode.AVAILABLE,
            spentThisMonth = money(51200),
            upcomingRecurring = money(12950),
            budgetLimit = money(102000),
            incomeThisMonth = Money.zero(eur),
        ),
        expectedByToday = money(61200),
        renewals = listOf(
            HomeRenewal(
                recurring = Recurring(
                    id = 1,
                    type = RecurringType.SUBSCRIPTION,
                    name = "Netflix",
                    amount = money(1399),
                    cycle = Cycle(1, CycleUnit.MONTH),
                    startDate = today.minusMonths(8),
                    nextDate = today.plusDays(3),
                ),
                relativeDate = if (it) "tra 3 giorni" else "in 3 days",
                flags = emptySet(),
            ),
            HomeRenewal(
                recurring = Recurring(
                    id = 2,
                    type = RecurringType.SUBSCRIPTION,
                    name = "Spotify",
                    amount = money(1099),
                    cycle = Cycle(1, CycleUnit.MONTH),
                    startDate = today.minusMonths(14),
                    nextDate = today.plusDays(5),
                ),
                relativeDate = if (it) "tra 5 giorni" else "in 5 days",
                flags = emptySet(),
            ),
            HomeRenewal(
                recurring = Recurring(
                    id = 5,
                    type = RecurringType.BILL,
                    name = if (it) "Assicurazione auto" else "Car insurance",
                    amount = money(4850),
                    cycle = Cycle(1, CycleUnit.MONTH),
                    startDate = today.minusMonths(20),
                    nextDate = today.plusDays(6),
                ),
                relativeDate = if (it) "tra 6 giorni" else "in 6 days",
                flags = emptySet(),
            ),
        ),
        hasMoreRenewals = true,
        topInsight = HomeInsight(
            insight = Insight.UnusedSubscription(4, Worth.RARELY, money(16788)),
            title = "Prime Video",
            body = if (it) "Lo usi poco" else "Rarely used",
            actionLabel = if (it) "Rivedi" else "Review",
            categoryKey = "streaming",
        ),
        pendingInboxCount = 0,
        recent = listOf(
            HomeTransaction(
                transaction = Transaction(
                    id = 10,
                    kind = TransactionKind.EXPENSE,
                    amount = money(4320),
                    accountId = 1,
                    merchant = if (it) "Esselunga" else "Whole Foods",
                    occurredAt = LocalDateTime.of(2026, 11, 18, 18, 42),
                ),
                title = if (it) "Esselunga" else "Whole Foods",
                categoryKey = "groceries",
                meta = if (it) "Carta" else "Card",
                income = false,
            ),
            HomeTransaction(
                transaction = Transaction(
                    id = 11,
                    kind = TransactionKind.EXPENSE,
                    amount = money(150),
                    accountId = 1,
                    merchant = if (it) "Bar Centrale" else "Blue Bottle",
                    occurredAt = LocalDateTime.of(2026, 11, 18, 8, 10),
                ),
                title = if (it) "Bar Centrale" else "Blue Bottle",
                categoryKey = "coffee",
                meta = null,
                income = false,
            ),
            HomeTransaction(
                transaction = Transaction(
                    id = 12,
                    kind = TransactionKind.EXPENSE,
                    amount = money(2890),
                    accountId = 1,
                    merchant = if (it) "Trenitalia" else "Amtrak",
                    occurredAt = LocalDateTime.of(2026, 11, 17, 14, 5),
                ),
                title = if (it) "Trenitalia" else "Amtrak",
                categoryKey = "transport",
                meta = if (it) "Carta" else "Card",
                income = false,
            ),
        ),
    )
}

internal fun storeActivityState(locale: StoreLocale): ActivityUiState {
    val it = locale == StoreLocale.IT
    return ActivityUiState(
        categories = storeCategories(locale),
        accounts = storeAccounts(locale),
        baseCurrency = eur,
    )
}

internal fun storeActivityItems(locale: StoreLocale): List<ActivityListItem> {
    val it = locale == StoreLocale.IT
    val day = today
    val yesterday = today.minusDays(1)
    return listOf(
        ActivityListItem.Header(
            date = day,
            label = if (it) "Oggi" else "Today",
            net = money(4470),
        ),
        ActivityListItem.Row(
            transaction = Transaction(
                id = 10,
                kind = TransactionKind.EXPENSE,
                amount = money(4320),
                accountId = 1,
                merchant = if (it) "Esselunga" else "Whole Foods",
                occurredAt = LocalDateTime.of(2026, 11, 18, 18, 42),
            ),
            title = if (it) "Esselunga" else "Whole Foods",
            categoryKey = "groceries",
            meta = if (it) "Carta" else "Card",
            income = false,
        ),
        ActivityListItem.Row(
            transaction = Transaction(
                id = 11,
                kind = TransactionKind.EXPENSE,
                amount = money(150),
                accountId = 1,
                merchant = if (it) "Bar Centrale" else "Blue Bottle",
                occurredAt = LocalDateTime.of(2026, 11, 18, 8, 10),
            ),
            title = if (it) "Bar Centrale" else "Blue Bottle",
            categoryKey = "coffee",
            meta = if (it) "Carta" else "Card",
            income = false,
        ),
        ActivityListItem.Header(
            date = yesterday,
            label = if (it) "Ieri" else "Yesterday",
            net = money(6740),
        ),
        ActivityListItem.Row(
            transaction = Transaction(
                id = 12,
                kind = TransactionKind.EXPENSE,
                amount = money(2890),
                accountId = 1,
                merchant = if (it) "Trenitalia" else "Amtrak",
                occurredAt = LocalDateTime.of(2026, 11, 17, 14, 5),
            ),
            title = if (it) "Trenitalia" else "Amtrak",
            categoryKey = "transport",
            meta = if (it) "Carta" else "Card",
            income = false,
        ),
        ActivityListItem.Row(
            transaction = Transaction(
                id = 13,
                kind = TransactionKind.EXPENSE,
                amount = money(3850),
                accountId = 1,
                merchant = if (it) "Coop" else "Trader Joe's",
                occurredAt = LocalDateTime.of(2026, 11, 17, 19, 20),
            ),
            title = if (it) "Coop" else "Trader Joe's",
            categoryKey = "groceries",
            meta = if (it) "Carta" else "Card",
            income = false,
        ),
        ActivityListItem.Row(
            transaction = Transaction(
                id = 14,
                kind = TransactionKind.EXPENSE,
                amount = money(4200),
                accountId = 1,
                merchant = if (it) "Eni Station" else "Shell",
                occurredAt = LocalDateTime.of(2026, 11, 17, 11, 40),
            ),
            title = if (it) "Eni Station" else "Shell",
            categoryKey = "transport",
            meta = if (it) "Carta" else "Card",
            income = false,
        ),
    )
}

internal fun storeSubscriptionsState(locale: StoreLocale): SubscriptionsUiState {
    val it = locale == StoreLocale.IT
    val gym = if (it) "Palestra" else "Gym"
    val insurance = if (it) "Assicurazione auto" else "Car insurance"
    return SubscriptionsUiState(
        loading = false,
        currency = eur,
        today = today,
        period = CostPeriod.MONTH,
        totalMonthly = money(22146),
        totalYearly = money(265752),
        subscriptionCount = 4,
        billCount = 2,
        incomeCount = 0,
        days = (0 until 30).map { offset ->
            val date = today.plusDays(offset.toLong())
            StripDay(
                date = date,
                hasRenewal = offset in setOf(1, 3, 6, 12, 18, 24),
                isToday = offset == 0,
                isSelected = false,
            )
        },
        statusFilter = StatusFilter.ACTIVE,
        rows = listOf(
            RecurringRowUi(
                1, "Netflix", "netflix", money(1399), money(16788),
                Cycle(1, CycleUnit.MONTH), today.plusDays(1),
                RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, "streaming",
                setOf(RecurringFlag.PRICE_UP),
            ),
            RecurringRowUi(
                2, "Spotify", "spotify", money(1099), money(13188),
                Cycle(1, CycleUnit.MONTH), today.plusDays(3),
                RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, "music",
                emptySet(),
            ),
            RecurringRowUi(
                3, "iCloud+", "icloud", money(299), money(3588),
                Cycle(1, CycleUnit.MONTH), today.plusDays(6),
                RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, "home",
                emptySet(),
            ),
            RecurringRowUi(
                4, gym, "gym", money(3999), money(47988),
                Cycle(1, CycleUnit.MONTH), today.plusDays(12),
                RecurringType.SUBSCRIPTION, RecurringStatus.ACTIVE, "sport",
                setOf(RecurringFlag.RARELY_USED),
            ),
            RecurringRowUi(
                5, insurance, "insurance", money(4850), money(58200),
                Cycle(1, CycleUnit.MONTH), today.plusDays(18),
                RecurringType.BILL, RecurringStatus.ACTIVE, "insurance",
                emptySet(),
            ),
            RecurringRowUi(
                6, if (it) "TIM" else "Phone plan", "tim", money(1499), money(17988),
                Cycle(1, CycleUnit.MONTH), today.plusDays(24),
                RecurringType.BILL, RecurringStatus.ACTIVE, "phone",
                emptySet(),
            ),
        ),
        canReview = true,
    )
}

internal fun storeInsightsState(locale: StoreLocale): InsightsUiState {
    val it = locale == StoreLocale.IT
    val groceries = if (it) "Alimentari" else "Groceries"
    val funCat = if (it) "Svago" else "Fun"
    val home = if (it) "Casa" else "Home"
    val restaurant = if (it) "Ristorante" else "Dining"
    val transport = if (it) "Trasporti" else "Transport"
    val streaming = "Streaming"
    return InsightsUiState(
        loading = false,
        currency = eur,
        today = today,
        totalYearlySaving = money(78516),
        insights = listOf(
            InsightCardUi(
                id = "unused-4",
                kind = InsightKind.Unused(if (it) "Palestra" else "Gym", Worth.RARELY),
                saving = money(47988),
                action = InsightAction.OpenSubscription(4),
                categoryKey = "sport",
            ),
            InsightCardUi(
                id = "annual-2",
                kind = InsightKind.AnnualPlan("Spotify", 17),
                saving = money(3480),
                action = InsightAction.OpenSubscription(2),
                categoryKey = "music",
            ),
            InsightCardUi(
                id = "trial-prime",
                kind = InsightKind.Unused("Prime Video", Worth.UNUSED),
                saving = money(5988),
                action = InsightAction.OpenSubscription(7),
                categoryKey = "streaming",
            ),
            InsightCardUi(
                id = "budget-1",
                kind = InsightKind.BudgetAtRisk(
                    budgetName = groceries,
                    spent = money(31240),
                    limit = money(35000),
                    expected = money(21000),
                ),
                saving = null,
                action = InsightAction.SetBudget(1),
            ),
        ),
        budgets = listOf(
            BudgetUi(
                id = 1,
                name = groceries,
                spent = money(31240),
                limit = money(35000),
                expected = money(24500),
                status = PaceStatus.AT_RISK,
                period = BudgetPeriod.MONTH,
                categoryIds = setOf(1L),
                rollover = false,
            ),
            BudgetUi(
                id = 2,
                name = funCat,
                spent = money(4800),
                limit = money(12000),
                expected = money(8400),
                status = PaceStatus.ON_TRACK,
                period = BudgetPeriod.MONTH,
                categoryIds = setOf(14L),
                rollover = true,
            ),
        ),
        goals = listOf(
            GoalUi(1, if (it) "Vacanza" else "Holiday", money(68000), money(120000), today.plusMonths(8)),
            GoalUi(2, if (it) "Fondo emergenza" else "Emergency fund", money(32000), money(150000), null),
        ),
        months = listOf(
            MonthSpend(YearMonth.of(2026, 6), money(91200)),
            MonthSpend(YearMonth.of(2026, 7), money(104500)),
            MonthSpend(YearMonth.of(2026, 8), money(98700)),
            MonthSpend(YearMonth.of(2026, 9), money(128450)),
            MonthSpend(YearMonth.of(2026, 10), money(110200)),
            MonthSpend(YearMonth.of(2026, 11), money(74200)),
        ),
        categories = listOf(
            CategorySpend(1, "groceries", groceries, money(31240)),
            CategorySpend(6, "home", home, money(21500)),
            CategorySpend(2, "restaurant", restaurant, money(14900)),
            CategorySpend(4, "transport", transport, money(8600)),
            CategorySpend(19, "streaming", streaming, money(3297)),
        ),
        categoryOptions = listOf(
            CategoryOptionUi(1, groceries, "groceries"),
            CategoryOptionUi(2, restaurant, "restaurant"),
            CategoryOptionUi(6, home, "home"),
            CategoryOptionUi(14, funCat, "entertainment"),
            CategoryOptionUi(19, streaming, "streaming"),
        ),
    )
}

internal fun storeQuickAddState(locale: StoreLocale): QuickAddUiState {
    val it = locale == StoreLocale.IT
    return QuickAddUiState(
        visible = true,
        amount = AmountDraft.fromMoney(money(1250)),
        currency = eur,
        categories = storeCategories(locale),
        selectedCategoryId = 2,
        accounts = storeAccounts(locale),
        accountId = 1,
        date = LocalDate.now(),
        merchant = if (it) "Bar Centrale" else "Blue Bottle",
        canSave = true,
    )
}

internal fun storeFeatureTagline(locale: StoreLocale): String = when (locale) {
    StoreLocale.IT -> "Sai quanto puoi ancora spendere."
    StoreLocale.EN -> "Know what you can still spend."
}

private fun storeAccounts(locale: StoreLocale): List<Account> {
    val it = locale == StoreLocale.IT
    return listOf(
        Account(1, if (it) "Carta" else "Card", eur, AccountType.CARD),
        Account(2, if (it) "Contanti" else "Cash", eur, AccountType.CASH),
        Account(3, if (it) "Conto" else "Checking", eur, AccountType.BANK),
    )
}

private fun storeCategories(locale: StoreLocale): List<Category> {
    val it = locale == StoreLocale.IT
    return listOf(
        Category(1, if (it) "Alimentari" else "Groceries", CategoryKind.EXPENSE, "groceries", 0xFF4CAF50),
        Category(2, if (it) "Caffè" else "Coffee", CategoryKind.EXPENSE, "coffee", 0xFF795548),
        Category(3, if (it) "Trasporti" else "Transport", CategoryKind.EXPENSE, "transport", 0xFF2196F3),
        Category(4, if (it) "Ristorante" else "Dining", CategoryKind.EXPENSE, "restaurant", 0xFFE91E63),
    )
}
