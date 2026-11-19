package dev.antomanc.tameki.feature.subscriptions

import dev.antomanc.tameki.core.ui.brand.BrandCatalog
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.PricePoint
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An account offered in the form. */
data class AccountOption(val id: Long, val name: String)

/** A category offered in the form; [categoryKey] is the core:ui icon/hue key. */
data class CategoryOption(val id: Long, val name: String, val categoryKey: String)

/** A brand the name field suggests, already reduced to what the form needs. */
data class BrandSuggestion(
    val key: String,
    val name: String,
    val accent: Color,
    val categoryKey: String,
)

/**
 * The add/edit form. It is a plain form state: every change is a function, validation is
 * [canSave], and nothing is written until the user taps Save.
 */
data class RecurringEditUiState(
    val isNew: Boolean = true,
    val loading: Boolean = true,
    val saved: Boolean = false,
    val name: String = "",
    val type: RecurringType = RecurringType.SUBSCRIPTION,
    val amount: Money? = null,
    val cycleEvery: Int = 1,
    val cycleUnit: CycleUnit = CycleUnit.MONTH,
    val startDate: LocalDate = LocalDate.of(1970, 1, 1),
    val nextDate: LocalDate = LocalDate.of(1970, 1, 1),
    val trialEndsOn: LocalDate? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val reminderDays: Int = 2,
    val autoLog: Boolean = false,
    val annualAlternative: Money? = null,
    val sharedWith: Int = 1,
    val cancelUrl: String = "",
    val note: String = "",
    val brandKey: String? = null,
    val currency: CurrencyCode = CurrencyCode("EUR"),
    val accounts: List<AccountOption> = emptyList(),
    val categories: List<CategoryOption> = emptyList(),
    val suggestions: List<BrandSuggestion> = emptyList(),
    val today: LocalDate = LocalDate.of(1970, 1, 1),
) {
    val canSave: Boolean
        get() = name.isNotBlank() && amount != null && amount.minor > 0L

    val amountMissing: Boolean
        get() = amount == null || amount.minor <= 0L
}

/**
 * Backs the add/edit screen. A changed amount appends a [PricePoint] to the existing history, so
 * the detail screen can show the increase instead of losing the old price.
 */
class RecurringEditViewModel(
    private val recurrings: RecurringRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
    private val catalog: BrandCatalog,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecurringEditUiState())
    val uiState: StateFlow<RecurringEditUiState> = _uiState.asStateFlow()

    private var existing: Recurring? = null
    private var prepared = false

    /** Loads the form once for [id] (null = new recurring). Safe to call on every composition. */
    fun prepare(id: Long?) {
        if (prepared) return
        prepared = true
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val currency = settings.observeBaseCurrency().first()
            val defaultRemindDays = settings.observeDefaultRemindDays().first()
            val accountList = accounts.observeAll(includeArchived = true).first()
                .map { AccountOption(it.id, it.name) }
            val categoryList = categories.observeAll(includeArchived = true).first()
                .map { CategoryOption(it.id, it.name, it.icon) }
            val recurring = id?.let { recurrings.get(it) }
            existing = recurring
            _uiState.value = if (recurring == null) {
                RecurringEditUiState(
                    isNew = true,
                    loading = false,
                    startDate = today,
                    nextDate = today,
                    reminderDays = defaultRemindDays,
                    currency = currency,
                    accounts = accountList,
                    categories = categoryList,
                    today = today,
                )
            } else {
                RecurringEditUiState(
                    isNew = false,
                    loading = false,
                    name = recurring.name,
                    type = recurring.type,
                    amount = recurring.amount,
                    cycleEvery = recurring.cycle.every,
                    cycleUnit = recurring.cycle.unit,
                    startDate = recurring.startDate,
                    nextDate = recurring.nextDate,
                    trialEndsOn = recurring.trialEndsOn,
                    accountId = recurring.accountId,
                    categoryId = recurring.categoryId,
                    reminderDays = recurring.remindDaysBefore,
                    autoLog = recurring.autoLog,
                    annualAlternative = recurring.annualAlternative,
                    sharedWith = recurring.sharedWith,
                    cancelUrl = recurring.cancelUrl.orEmpty(),
                    note = recurring.note.orEmpty(),
                    brandKey = recurring.brandKey,
                    currency = recurring.amount.currency,
                    accounts = accountList,
                    categories = categoryList,
                    today = today,
                )
            }
        }
    }

    fun setName(name: String) = _uiState.update { state ->
        val suggestions = if (state.brandKey != null) emptyList() else {
            catalog.suggest(name).map { BrandSuggestion(it.key, it.name, it.accent, it.category) }
        }
        state.copy(name = name, suggestions = suggestions)
    }

    fun selectBrand(suggestion: BrandSuggestion) = _uiState.update { state ->
        val brand = catalog.find(suggestion.key)
        val category = state.categories.firstOrNull { it.categoryKey == suggestion.categoryKey }
        state.copy(
            name = suggestion.name,
            brandKey = suggestion.key,
            type = brand?.recurringType ?: state.type,
            categoryId = category?.id ?: state.categoryId,
            cancelUrl = state.cancelUrl.ifBlank { brand?.cancelUrl.orEmpty() },
            suggestions = emptyList(),
        )
    }

    fun setType(type: RecurringType) = _uiState.update { it.copy(type = type) }

    fun setAmount(amount: Money?) = _uiState.update { it.copy(amount = amount) }

    fun setCycleEvery(every: Int) = _uiState.update { it.copy(cycleEvery = every.coerceIn(1, 99)) }

    fun setCycleUnit(unit: CycleUnit) = _uiState.update { it.copy(cycleUnit = unit) }

    fun setStartDate(date: LocalDate) = _uiState.update {
        val next = if (it.isNew) date else it.nextDate
        it.copy(startDate = date, nextDate = next)
    }

    fun setNextDate(date: LocalDate) = _uiState.update { it.copy(nextDate = date) }

    fun setTrialEnd(date: LocalDate?) = _uiState.update { it.copy(trialEndsOn = date) }

    fun setAccount(id: Long?) = _uiState.update { it.copy(accountId = id) }

    fun setCategory(id: Long?) = _uiState.update { it.copy(categoryId = id) }

    fun setReminderDays(days: Int) = _uiState.update { it.copy(reminderDays = days) }

    fun setAutoLog(enabled: Boolean) = _uiState.update { it.copy(autoLog = enabled) }

    fun setAnnualAlternative(amount: Money?) = _uiState.update { it.copy(annualAlternative = amount) }

    fun setSharedWith(people: Int) = _uiState.update { it.copy(sharedWith = people.coerceIn(1, 20)) }

    fun setCancelUrl(url: String) = _uiState.update { it.copy(cancelUrl = url) }

    fun setNote(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val previous = existing
            val history = if (
                previous != null &&
                previous.amount.currency == amount.currency &&
                previous.amount.minor != amount.minor
            ) {
                previous.priceHistory + PricePoint(amount, today)
            } else {
                previous?.priceHistory.orEmpty()
            }
            val saved = Recurring(
                id = previous?.id ?: 0L,
                type = state.type,
                name = state.name.trim(),
                amount = amount,
                cycle = Cycle(state.cycleEvery, state.cycleUnit),
                startDate = state.startDate,
                nextDate = state.nextDate,
                trialEndsOn = state.trialEndsOn,
                status = previous?.status ?: RecurringStatus.ACTIVE,
                accountId = state.accountId,
                categoryId = state.categoryId,
                cancelUrl = state.cancelUrl.trim().ifBlank { null },
                note = state.note.trim().ifBlank { null },
                worth = previous?.worth,
                worthRatedOn = previous?.worthRatedOn,
                remindDaysBefore = state.reminderDays,
                autoLog = state.autoLog,
                priceHistory = history.sortedBy { it.since },
                annualAlternative = state.annualAlternative,
                brandKey = state.brandKey,
                sharedWith = state.sharedWith,
            )
            val id = recurrings.upsert(saved)
            if (state.reminderDays >= 0) {
                reminders.scheduleRecurringReminder(id, saved.nextDate, state.reminderDays)
            } else {
                reminders.cancelRecurringReminder(id)
            }
            val trialEnd = saved.trialEndsOn
            if (trialEnd != null) {
                reminders.scheduleTrialEnding(id, trialEnd)
            } else {
                reminders.cancelTrialEnding(id)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
