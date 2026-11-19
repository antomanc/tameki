package dev.antomanc.tameki.feature.subscriptions

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
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import dev.antomanc.tameki.core.model.recurring.RecurrenceCalculator
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One step of the price history, ready to be drawn. */
data class PricePointUi(val amount: Money, val since: LocalDate)

/**
 * Everything the detail screen shows: the plan cost and the user's share, the price history, the
 * next three charges and the actions available for the current status.
 */
data class RecurringDetailUiState(
    val loading: Boolean = true,
    val found: Boolean = false,
    /** The user confirmed Delete; the route leaves the screen once the row is gone. */
    val deleted: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val brandKey: String? = null,
    val type: RecurringType = RecurringType.SUBSCRIPTION,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    /** Increments on every cancel so the screen can raise a fresh Undo snackbar. */
    val cancelUndoGeneration: Long = 0L,
    val amount: Money = Money.zero(CurrencyCode("EUR")),
    val cycle: Cycle = Cycle(1, CycleUnit.MONTH),
    val monthly: Money = Money.zero(CurrencyCode("EUR")),
    val yearly: Money = Money.zero(CurrencyCode("EUR")),
    val yourMonthly: Money = Money.zero(CurrencyCode("EUR")),
    val yourYearly: Money = Money.zero(CurrencyCode("EUR")),
    val sharedWith: Int = 1,
    val priceHistory: List<PricePointUi> = emptyList(),
    val nextDates: List<LocalDate> = emptyList(),
    val trialEndsOn: LocalDate? = null,
    val cancelUrl: String? = null,
    val worth: Worth? = null,
    val accountName: String? = null,
    val categoryName: String? = null,
    val note: String? = null,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
) {
    val isPaused: Boolean get() = status == RecurringStatus.PAUSED
    val isCancelled: Boolean get() = status == RecurringStatus.CANCELLED
}

/**
 * Detail of one recurring payment. Cost figures come from [CostNormalizer], so a family plan shows
 * the whole price and the user's share separately instead of guessing.
 */
class RecurringDetailViewModel(
    private val recurrings: RecurringRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    private val reminders: ReminderScheduler,
    private val clock: Clock,
) : ViewModel() {
    private data class ScreenStatus(val deleted: Boolean = false, val cancelUndo: Long = 0L)

    /** The row a CANCEL replaced, kept while its snackbar can still Undo. */
    private data class PendingCancel(val original: Recurring, val generation: Long)

    private val id = MutableStateFlow<Long?>(null)
    private val status = MutableStateFlow(ScreenStatus())
    private var pendingCancel: PendingCancel? = null

    val uiState: StateFlow<RecurringDetailUiState> = combine(
        id,
        recurrings.observeAll(),
        accounts.observeAll(includeArchived = true),
        categories.observeAll(includeArchived = true),
        status,
    ) { recurringId, all, accountList, categoryList, screenStatus ->
        if (recurringId == null) return@combine RecurringDetailUiState(loading = true)
        val recurring = all.firstOrNull { it.id == recurringId }
            ?: return@combine RecurringDetailUiState(
                loading = false,
                found = false,
                deleted = screenStatus.deleted,
            )
        val today = LocalDate.now(clock)
        RecurringDetailUiState(
            loading = false,
            found = true,
            deleted = screenStatus.deleted,
            cancelUndoGeneration = screenStatus.cancelUndo,
            id = recurring.id,
            name = recurring.name,
            brandKey = recurring.brandKey,
            type = recurring.type,
            status = recurring.status,
            amount = recurring.amount,
            cycle = recurring.cycle,
            monthly = CostNormalizer.monthly(recurring.copy(sharedWith = 1)),
            yearly = CostNormalizer.yearly(recurring.copy(sharedWith = 1)),
            yourMonthly = CostNormalizer.monthly(recurring),
            yourYearly = CostNormalizer.yearly(recurring),
            sharedWith = recurring.sharedWith,
            priceHistory = recurring.priceHistory.sortedBy { it.since }.map { PricePointUi(it.amount, it.since) },
            nextDates = nextDates(recurring, 3),
            trialEndsOn = recurring.trialEndsOn,
            cancelUrl = recurring.cancelUrl,
            worth = recurring.worth,
            accountName = accountList.firstOrNull { it.id == recurring.accountId }?.name,
            categoryName = categoryList.firstOrNull { it.id == recurring.categoryId }?.name,
            note = recurring.note,
            today = today,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurringDetailUiState(),
    )

    fun prepare(id: Long) {
        this.id.value = id
        pendingCancel = null
        status.value = ScreenStatus()
    }

    fun rate(worth: Worth) = update {
        it.copy(worth = worth, worthRatedOn = LocalDate.now(clock))
    }

    fun pause() {
        update { it.copy(status = RecurringStatus.PAUSED) }
        viewModelScope.launch { reminders.cancelRecurringReminder(id.value ?: return@launch) }
    }

    fun resume() {
        update { it.copy(status = RecurringStatus.ACTIVE) }
        val state = uiState.value
        if (state.found) {
            viewModelScope.launch {
                if (state.nextDates.isNotEmpty()) {
                    reminders.scheduleRecurringReminder(state.id, state.nextDates.first(), 2)
                }
            }
        }
    }

    /**
     * Marks the subscription cancelled and keeps the original row until its Undo snackbar goes
     * away, exactly like the review flow: status and reminders come back together.
     * The caller opens [RecurringDetailUiState.cancelUrl] first.
     */
    fun cancel() {
        val recurringId = id.value ?: return
        viewModelScope.launch {
            val original = recurrings.get(recurringId) ?: return@launch
            recurrings.upsert(original.copy(status = RecurringStatus.CANCELLED))
            reminders.cancelRecurringReminder(recurringId)
            reminders.cancelTrialEnding(recurringId)
            val generation = status.value.cancelUndo + 1L
            pendingCancel = PendingCancel(original, generation)
            status.update { it.copy(cancelUndo = generation) }
        }
    }

    /** Steps back from the last cancellation: status, worth and reminders as they were. */
    fun undoCancel() {
        val pending = pendingCancel ?: return
        pendingCancel = null
        viewModelScope.launch {
            recurrings.upsert(pending.original)
            if (pending.original.status == RecurringStatus.ACTIVE ||
                pending.original.status == RecurringStatus.TRIAL
            ) {
                reminders.scheduleRecurringReminder(
                    pending.original.id,
                    pending.original.nextDate,
                    pending.original.remindDaysBefore,
                )
            }
            pending.original.trialEndsOn?.let {
                reminders.scheduleTrialEnding(pending.original.id, it)
            }
        }
    }

    /** The snackbar for [generation] went away; a later Undo must not resurrect a stale cancel. */
    fun acknowledgeCancelUndo(generation: Long) {
        if (pendingCancel?.generation == generation) pendingCancel = null
    }

    /**
     * Removes the recurring row and its reminders immediately. The caller receives the deleted row
     * so it can offer Undo in a snackbar, per DESIGN.md: deletes are never confirmation dialogs.
     */
    fun delete(onDeleted: (Recurring) -> Unit = {}) {
        val recurringId = id.value ?: return
        viewModelScope.launch {
            val snapshot = recurrings.get(recurringId) ?: return@launch
            recurrings.delete(recurringId)
            reminders.cancelRecurringReminder(recurringId)
            reminders.cancelTrialEnding(recurringId)
            onDeleted(snapshot)
            status.update { it.copy(deleted = true) }
        }
    }

    private fun update(transform: (Recurring) -> Recurring) {
        val recurringId = id.value ?: return
        viewModelScope.launch {
            val current = recurrings.get(recurringId) ?: return@launch
            recurrings.upsert(transform(current))
        }
    }

    private fun nextDates(recurring: Recurring, count: Int): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        var cursor = recurring.nextDate
        repeat(count) {
            out += cursor
            cursor = RecurrenceCalculator.next(cursor, recurring.cycle, recurring.startDate.dayOfMonth)
        }
        return out
    }
}
