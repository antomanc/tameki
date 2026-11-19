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
import dev.antomanc.tameki.core.model.recurring.RecurringLifecycle
import dev.antomanc.tameki.core.model.recurring.nextRenewals
import dev.antomanc.tameki.core.model.recurring.isRunning
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
/**
 * Where the detail screen is. Loading → Loaded or Missing; Loaded → Deleting → Deleted (the route
 * leaves). A row that disappears while Deleting is the delete landing, never "not found".
 */
enum class DetailPhase { Loading, Loaded, Missing, Deleting, Deleted }

data class RecurringDetailUiState(
    val phase: DetailPhase = DetailPhase.Loading,
    val id: Long = 0,
    val name: String = "",
    val brandKey: String? = null,
    val type: RecurringType = RecurringType.SUBSCRIPTION,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    /** The cancel its snackbar can still undo, or null. A new token is a new snackbar. */
    val undoToken: Long? = null,
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
    val loading: Boolean get() = phase == DetailPhase.Loading
    val found: Boolean get() = phase == DetailPhase.Loaded
    /** The user confirmed Delete; the route leaves the screen once the row is gone. */
    val deleted: Boolean get() = phase == DetailPhase.Deleted

    /** Only a subscription can be cancelled with a provider; a bill or income is paused or deleted. */
    val canCancel: Boolean get() = type == RecurringType.SUBSCRIPTION && !isCancelled
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
    private enum class Removal { None, Deleting, Deleted }

    /** The row a CANCEL replaced, kept while its snackbar can still Undo. */
    private data class PendingCancel(val original: Recurring, val token: Long)

    private val id = MutableStateFlow<Long?>(null)
    private val removal = MutableStateFlow(Removal.None)
    private val pendingCancel = MutableStateFlow<PendingCancel?>(null)
    private var nextToken = 0L

    val uiState: StateFlow<RecurringDetailUiState> = combine(
        id,
        recurrings.observeAll(),
        accounts.observeAll(includeArchived = true),
        categories.observeAll(includeArchived = true),
        combine(removal, pendingCancel) { r, p -> r to p },
    ) { recurringId, all, accountList, categoryList, (removalState, pending) ->
        if (recurringId == null) return@combine RecurringDetailUiState()
        when (removalState) {
            Removal.Deleting -> return@combine RecurringDetailUiState(phase = DetailPhase.Deleting)
            Removal.Deleted -> return@combine RecurringDetailUiState(phase = DetailPhase.Deleted)
            Removal.None -> Unit
        }
        val today = LocalDate.now(clock)
        val stored = all.firstOrNull { it.id == recurringId }
            ?: return@combine RecurringDetailUiState(phase = DetailPhase.Missing)
        val recurring = RecurringLifecycle.settle(stored, today)
        RecurringDetailUiState(
            phase = DetailPhase.Loaded,
            undoToken = pending?.token,
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
            // A paused or cancelled plan has no upcoming charges: listing them would invent money.
            nextDates = if (recurring.isRunning) nextDates(recurring, 3) else emptyList(),
            trialEndsOn = recurring.trialEndsOn,
            cancelUrl = recurring.cancelUrl,
            worth = recurring.worth,
            accountName = accountList.firstOrNull { it.id == recurring.accountId }?.resolvedName(),
            categoryName = categoryList.firstOrNull { it.id == recurring.categoryId }?.resolvedName(),
            note = recurring.note,
            today = today,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurringDetailUiState(),
    )

    /**
     * Binds the screen to [id]. Called again after a rotation or a return from Edit with the same
     * id, which must not drop a pending Undo nor a delete in progress.
     */
    fun prepare(id: Long) {
        if (this.id.value == id) return
        this.id.value = id
        pendingCancel.value = null
        removal.value = Removal.None
    }

    fun rate(worth: Worth) = update {
        it.copy(worth = worth, worthRatedOn = LocalDate.now(clock))
    }

    fun pause() = transition { RecurringLifecycle.pause(it) }

    fun resume() = transition { RecurringLifecycle.resume(it, LocalDate.now(clock)) }

    /** Brings a cancelled plan back; its reminders return with it. */
    fun reactivate() = transition { RecurringLifecycle.reactivate(it, LocalDate.now(clock)) }

    /**
     * Applies a status change and makes the reminders match it exactly: the same rules as review
     * and edit, from [RecurringLifecycle], so a resumed trial keeps its trial reminder and a
     * resumed plan reminds with its own lead time on its real next date.
     */
    private fun transition(change: (Recurring) -> Recurring) {
        val recurringId = id.value ?: return
        viewModelScope.launch {
            val current = recurrings.get(recurringId) ?: return@launch
            val next = change(current)
            if (next == current) return@launch
            recurrings.upsert(next)
            syncReminders(next)
        }
    }

    private suspend fun syncReminders(r: Recurring) {
        val plan = RecurringLifecycle.reminders(r, LocalDate.now(clock))
        val renewal = plan.renewal
        if (renewal != null) {
            reminders.scheduleRecurringReminder(r.id, renewal.first, renewal.second)
        } else {
            reminders.cancelRecurringReminder(r.id)
        }
        val trial = plan.trialEnding
        if (trial != null) reminders.scheduleTrialEnding(r.id, trial) else reminders.cancelTrialEnding(r.id)
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
            val cancelled = RecurringLifecycle.cancel(original)
            recurrings.upsert(cancelled)
            syncReminders(cancelled)
            pendingCancel.value = PendingCancel(original, ++nextToken)
        }
    }

    /** Steps back from the cancellation behind [token]: status, worth and reminders as they were. */
    fun undoCancel(token: Long) {
        val pending = pendingCancel.value?.takeIf { it.token == token } ?: return
        pendingCancel.value = null
        viewModelScope.launch {
            recurrings.upsert(pending.original)
            syncReminders(pending.original)
        }
    }

    /** The snackbar for [token] went away; a later Undo must not resurrect a stale cancel. */
    fun acknowledgeCancelUndo(token: Long) {
        pendingCancel.update { if (it?.token == token) null else it }
    }

    /**
     * Removes the recurring row and its reminders immediately. The caller receives the deleted row
     * so it can offer Undo in a snackbar, per DESIGN.md: deletes are never confirmation dialogs.
     */
    fun delete(onDeleted: (Recurring) -> Unit = {}) {
        val recurringId = id.value ?: return
        if (removal.value != Removal.None) return
        // Deleting before the write: the row vanishing from the database is then read as the
        // delete landing, not as "this plan does not exist".
        removal.value = Removal.Deleting
        viewModelScope.launch {
            val snapshot = recurrings.get(recurringId)
            if (snapshot == null) {
                removal.value = Removal.Deleted
                return@launch
            }
            recurrings.delete(recurringId)
            reminders.cancelRecurringReminder(recurringId)
            reminders.cancelTrialEnding(recurringId)
            onDeleted(snapshot)
            removal.value = Removal.Deleted
        }
    }

    private fun update(transform: (Recurring) -> Recurring) {
        val recurringId = id.value ?: return
        viewModelScope.launch {
            val current = recurrings.get(recurringId) ?: return@launch
            recurrings.upsert(transform(current))
        }
    }

    private fun nextDates(recurring: Recurring, count: Int): List<LocalDate> =
        RecurrenceCalculator.nextRenewals(recurring, LocalDate.now(clock), count)
}
