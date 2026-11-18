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

/**
 * One shared fixture every store screenshot reads from. Dates, amounts and insight rankings agree
 * across Today, Activity, Recurring and Save — the same Netflix never renews "in 3 days" on one
 * screen and "tomorrow" on another.
 */
internal object StoreFixture {
    val eur = CurrencyCode("EUR")
    val today: LocalDate = LocalDate.of(2026, 11, 18)
    private fun money(minor: Long) = Money(minor, eur)

    private data class DemoRecurring(
        val id: Long,
        val nameIt: String,
        val nameEn: String,
        val brandKey: String,
        val monthly: Long,
        val daysUntil: Long,
        val type: RecurringType,
        val flags: Set<RecurringFlag> = emptySet(),
        val worth: Worth? = null,
        val categoryKey: String,
    ) {
        val yearly: Long get() = monthly * 12
        fun name(locale: StoreLocale) = if (locale == StoreLocale.IT) nameIt else nameEn
        fun nextDate(): LocalDate = today.plusDays(daysUntil)
    }

    private data class DemoTx(
        val id: Long,
        val merchantIt: String,
        val merchantEn: String,
        val amount: Long,
        val categoryKey: String,
        val occurredAt: LocalDateTime,
        val cardMeta: Boolean,
    )

    // 4 subscriptions + 2 bills. Monthly sum = hero on Recurring.
    private val ledger = listOf(
        DemoRecurring(
            id = 1, nameIt = "Netflix", nameEn = "Netflix", brandKey = "netflix",
            monthly = 1399, daysUntil = 3, type = RecurringType.SUBSCRIPTION,
            flags = setOf(RecurringFlag.PRICE_UP), categoryKey = "streaming",
        ),
        DemoRecurring(
            id = 2, nameIt = "Spotify", nameEn = "Spotify", brandKey = "spotify",
            monthly = 1099, daysUntil = 5, type = RecurringType.SUBSCRIPTION,
            categoryKey = "music",
        ),
        DemoRecurring(
            id = 3, nameIt = "Prime Video", nameEn = "Prime Video", brandKey = "prime",
            monthly = 1399, daysUntil = 9, type = RecurringType.SUBSCRIPTION,
            flags = setOf(RecurringFlag.RARELY_USED), worth = Worth.RARELY, categoryKey = "streaming",
        ),
        DemoRecurring(
            id = 4, nameIt = "Palestra", nameEn = "Gym", brandKey = "gym",
            monthly = 3999, daysUntil = 12, type = RecurringType.SUBSCRIPTION,
            flags = setOf(RecurringFlag.RARELY_USED), worth = Worth.RARELY, categoryKey = "sport",
        ),
        DemoRecurring(
            id = 5, nameIt = "Assicurazione auto", nameEn = "Car insurance", brandKey = "insurance",
            monthly = 4850, daysUntil = 6, type = RecurringType.BILL, categoryKey = "insurance",
        ),
        DemoRecurring(
            id = 6, nameIt = "TIM", nameEn = "Phone plan", brandKey = "tim",
            monthly = 1499, daysUntil = 24, type = RecurringType.BILL, categoryKey = "phone",
        ),
    )

    private val prime = ledger.first { it.id == 3L }
    private val gym = ledger.first { it.id == 4L }
    private val spotify = ledger.first { it.id == 2L }

    /** Yearly savings shown on cards; hero = deduplicated sum of these. */
    private val primeSaving = money(prime.yearly) // €167.88
    private val gymSaving = money(gym.yearly) // €479.88
    private val spotifyAnnualSaving = money(3480) // ~17% vs monthly plan
    val totalYearlySaving: Money = money(primeSaving.minor + gymSaving.minor + spotifyAnnualSaving.minor)

    val totalMonthly: Money = money(ledger.sumOf { it.monthly })
    val totalYearly: Money = money(ledger.sumOf { it.yearly })
    val subscriptionCount: Int = ledger.count { it.type == RecurringType.SUBSCRIPTION }
    val billCount: Int = ledger.count { it.type == RecurringType.BILL }
    val incomeCount: Int = ledger.count { it.type == RecurringType.INCOME }

    private val upcomingWithin7 = ledger.filter { it.daysUntil in 1L..7L }
    val upcomingRecurring: Money = money(upcomingWithin7.sumOf { it.monthly })

    val spentThisMonth: Money = money(51200)
    val budgetLimit: Money = money(102000)
    val availableToSpend: Money = money(budgetLimit.minor - spentThisMonth.minor - upcomingRecurring.minor)
    val expectedByToday: Money = money(61200)

    private val renewalOffsets: Set<Int> = ledger.map { it.daysUntil.toInt() }.toSet()

    // Visible Activity rows (~€154 over two days); month total €512 is the rest of November.
    private val transactions = listOf(
        DemoTx(10, "Esselunga", "Whole Foods", 4320, "groceries", LocalDateTime.of(2026, 11, 18, 18, 42), true),
        DemoTx(11, "Bar Centrale", "Blue Bottle", 150, "coffee", LocalDateTime.of(2026, 11, 18, 8, 10), false),
        DemoTx(12, "Trenitalia", "Amtrak", 2890, "transport", LocalDateTime.of(2026, 11, 17, 14, 5), true),
        DemoTx(13, "Coop", "Trader Joe's", 3850, "groceries", LocalDateTime.of(2026, 11, 17, 19, 20), true),
        DemoTx(14, "Eni Station", "Shell", 4200, "transport", LocalDateTime.of(2026, 11, 17, 11, 40), true),
    )

    fun catalog(): BrandCatalog = BrandCatalog(
        listOf(
            Brand("netflix", "Netflix", "#E50914", "streaming"),
            Brand("spotify", "Spotify", "#1DB954", "music"),
            Brand("prime", "Prime Video", "#00A8E1", "streaming"),
            Brand("gym", "McFit", "#FFD400", "sport"),
            Brand("insurance", "Generali", "#CC0000", "insurance", type = "BILL"),
            Brand("tim", "TIM", "#0057B8", "phone", type = "BILL"),
            Brand("icloud", "iCloud+", "#0071E3", "home"),
            Brand("enel", "Enel Energia", "#E4002B", "utilities", type = "BILL"),
        ),
    )

    private fun relative(days: Long, locale: StoreLocale): String = when {
        days == 1L && locale == StoreLocale.IT -> "domani"
        days == 1L -> "tomorrow"
        locale == StoreLocale.IT -> "tra $days giorni"
        else -> "in $days days"
    }

    fun homeState(locale: StoreLocale): HomeUiState {
        val it = locale == StoreLocale.IT
        val renewals = upcomingWithin7.sortedBy { it.daysUntil }.map { item ->
            HomeRenewal(
                recurring = Recurring(
                    id = item.id,
                    type = item.type,
                    name = item.name(locale),
                    amount = money(item.monthly),
                    cycle = Cycle(1, CycleUnit.MONTH),
                    startDate = today.minusMonths(8),
                    nextDate = item.nextDate(),
                ),
                relativeDate = relative(item.daysUntil, locale),
                flags = item.flags,
            )
        }
        val recent = transactions.take(3).map { tx ->
            HomeTransaction(
                transaction = Transaction(
                    id = tx.id,
                    kind = TransactionKind.EXPENSE,
                    amount = money(tx.amount),
                    accountId = 1,
                    merchant = if (it) tx.merchantIt else tx.merchantEn,
                    occurredAt = tx.occurredAt,
                ),
                title = if (it) tx.merchantIt else tx.merchantEn,
                categoryKey = tx.categoryKey,
                meta = when {
                    !tx.cardMeta -> null
                    it -> "Carta"
                    else -> "Card"
                },
                income = false,
            )
        }
        return HomeUiState(
            loading = false,
            baseCurrency = eur,
            hero = AvailableToSpend(
                amount = availableToSpend,
                mode = HeroMode.AVAILABLE,
                spentThisMonth = spentThisMonth,
                upcomingRecurring = upcomingRecurring,
                budgetLimit = budgetLimit,
                incomeThisMonth = Money.zero(eur),
            ),
            expectedByToday = expectedByToday,
            renewals = renewals,
            hasMoreRenewals = ledger.size > renewals.size,
            topInsight = HomeInsight(
                insight = Insight.UnusedSubscription(prime.id, Worth.RARELY, primeSaving),
                title = "Prime Video",
                body = if (it) "Lo usi poco" else "Rarely used",
                actionLabel = if (it) "Rivedi" else "Review",
                categoryKey = "streaming",
            ),
            pendingInboxCount = 0,
            recent = recent,
        )
    }

    fun activityState(locale: StoreLocale): ActivityUiState = ActivityUiState(
        categories = categories(locale),
        accounts = accounts(locale),
        baseCurrency = eur,
    )

    fun activityItems(locale: StoreLocale): List<ActivityListItem> {
        val it = locale == StoreLocale.IT
        val day = today
        val yesterday = today.minusDays(1)
        fun rowsFor(date: LocalDate): List<ActivityListItem.Row> =
            transactions.filter { it.occurredAt.toLocalDate() == date }.map { tx ->
                ActivityListItem.Row(
                    transaction = Transaction(
                        id = tx.id,
                        kind = TransactionKind.EXPENSE,
                        amount = money(tx.amount),
                        accountId = 1,
                        merchant = if (it) tx.merchantIt else tx.merchantEn,
                        occurredAt = tx.occurredAt,
                    ),
                    title = if (it) tx.merchantIt else tx.merchantEn,
                    categoryKey = tx.categoryKey,
                    meta = when {
                        !tx.cardMeta -> null
                        it -> "Carta"
                        else -> "Card"
                    },
                    income = false,
                )
            }
        val todayRows = rowsFor(day)
        val yesterdayRows = rowsFor(yesterday)
        return buildList {
            add(
                ActivityListItem.Header(
                    date = day,
                    label = if (it) "Oggi" else "Today",
                    net = money(todayRows.sumOf { it.transaction.amount.minor }),
                ),
            )
            addAll(todayRows)
            add(
                ActivityListItem.Header(
                    date = yesterday,
                    label = if (it) "Ieri" else "Yesterday",
                    net = money(yesterdayRows.sumOf { it.transaction.amount.minor }),
                ),
            )
            addAll(yesterdayRows)
        }
    }

    fun subscriptionsState(locale: StoreLocale): SubscriptionsUiState {
        val rows = ledger.sortedBy { it.daysUntil }.map { item ->
            RecurringRowUi(
                id = item.id,
                name = item.name(locale),
                brandKey = item.brandKey,
                amount = money(item.monthly),
                yearly = money(item.yearly),
                cycle = Cycle(1, CycleUnit.MONTH),
                nextDate = item.nextDate(),
                type = item.type,
                status = RecurringStatus.ACTIVE,
                categoryKey = item.categoryKey,
                flags = item.flags,
            )
        }
        return SubscriptionsUiState(
            loading = false,
            currency = eur,
            today = today,
            period = CostPeriod.MONTH,
            totalMonthly = totalMonthly,
            totalYearly = totalYearly,
            subscriptionCount = subscriptionCount,
            billCount = billCount,
            incomeCount = incomeCount,
            days = (0 until 30).map { offset ->
                StripDay(
                    date = today.plusDays(offset.toLong()),
                    hasRenewal = offset in renewalOffsets,
                    isToday = offset == 0,
                    isSelected = false,
                )
            },
            statusFilter = StatusFilter.ACTIVE,
            rows = rows,
            canReview = true,
        )
    }

    fun insightsState(locale: StoreLocale): InsightsUiState {
        val it = locale == StoreLocale.IT
        val groceries = if (it) "Alimentari" else "Groceries"
        val funCat = if (it) "Svago" else "Fun"
        val home = if (it) "Casa" else "Home"
        val restaurant = if (it) "Ristorante" else "Dining"
        val transport = if (it) "Trasporti" else "Transport"
        val streaming = "Streaming"
        // First card = Today's top insight (Prime Video), same amount.
        val insights = listOf(
            InsightCardUi(
                id = "unused-${prime.id}",
                kind = InsightKind.Unused("Prime Video", Worth.RARELY),
                saving = primeSaving,
                action = InsightAction.OpenSubscription(prime.id),
                categoryKey = "streaming",
            ),
            InsightCardUi(
                id = "unused-${gym.id}",
                kind = InsightKind.Unused(gym.name(locale), Worth.RARELY),
                saving = gymSaving,
                action = InsightAction.OpenSubscription(gym.id),
                categoryKey = "sport",
            ),
            InsightCardUi(
                id = "annual-${spotify.id}",
                kind = InsightKind.AnnualPlan("Spotify", 17),
                saving = spotifyAnnualSaving,
                action = InsightAction.OpenSubscription(spotify.id),
                categoryKey = "music",
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
        )
        return InsightsUiState(
            loading = false,
            currency = eur,
            today = today,
            totalYearlySaving = totalYearlySaving,
            insights = insights,
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
                MonthSpend(YearMonth.of(2026, 11), spentThisMonth),
            ),
            categories = listOf(
                CategorySpend(1, "groceries", groceries, money(31240)),
                CategorySpend(6, "home", home, money(21500)),
                CategorySpend(2, "restaurant", restaurant, money(14900)),
                CategorySpend(4, "transport", transport, money(8600)),
                CategorySpend(19, "streaming", streaming, money(ledger.filter { it.categoryKey == "streaming" }.sumOf { it.monthly })),
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

    fun quickAddState(locale: StoreLocale): QuickAddUiState {
        val it = locale == StoreLocale.IT
        return QuickAddUiState(
            visible = true,
            amount = AmountDraft.fromMoney(money(1250)),
            currency = eur,
            categories = categories(locale),
            selectedCategoryId = 2,
            accounts = accounts(locale),
            accountId = 1,
            date = today,
            merchant = if (it) "Bar Centrale" else "Blue Bottle",
            canSave = true,
        )
    }

    fun featureTagline(locale: StoreLocale): String = when (locale) {
        StoreLocale.IT -> "Sai quanto puoi ancora spendere."
        StoreLocale.EN -> "Know what you can still spend."
    }

    private fun accounts(locale: StoreLocale): List<Account> {
        val it = locale == StoreLocale.IT
        return listOf(
            Account(1, if (it) "Carta" else "Card", eur, AccountType.CARD),
            Account(2, if (it) "Contanti" else "Cash", eur, AccountType.CASH),
            Account(3, if (it) "Conto" else "Checking", eur, AccountType.BANK),
        )
    }

    private fun categories(locale: StoreLocale): List<Category> {
        val it = locale == StoreLocale.IT
        return listOf(
            Category(1, if (it) "Alimentari" else "Groceries", CategoryKind.EXPENSE, "groceries", 0xFF4CAF50),
            Category(2, if (it) "Caffè" else "Coffee", CategoryKind.EXPENSE, "coffee", 0xFF795548),
            Category(3, if (it) "Trasporti" else "Transport", CategoryKind.EXPENSE, "transport", 0xFF2196F3),
            Category(4, if (it) "Ristorante" else "Dining", CategoryKind.EXPENSE, "restaurant", 0xFFE91E63),
        )
    }
}

internal fun storeCatalog(): BrandCatalog = StoreFixture.catalog()
internal fun storeHomeState(locale: StoreLocale): HomeUiState = StoreFixture.homeState(locale)
internal fun storeActivityState(locale: StoreLocale): ActivityUiState = StoreFixture.activityState(locale)
internal fun storeActivityItems(locale: StoreLocale): List<ActivityListItem> = StoreFixture.activityItems(locale)
internal fun storeSubscriptionsState(locale: StoreLocale): SubscriptionsUiState = StoreFixture.subscriptionsState(locale)
internal fun storeInsightsState(locale: StoreLocale): InsightsUiState = StoreFixture.insightsState(locale)
internal fun storeQuickAddState(locale: StoreLocale): QuickAddUiState = StoreFixture.quickAddState(locale)
internal fun storeFeatureTagline(locale: StoreLocale): String = StoreFixture.featureTagline(locale)
