package dev.antomanc.tameki.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import dev.antomanc.tameki.core.model.recurring.SubscriptionReview
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The three answers a review asks for. */
enum class ReviewDecision { KEEP, RARELY, CANCEL }

/** One subscription waiting for its answer. */
data class ReviewItem(
    val id: Long,
    val name: String,
    val brandKey: String?,
    val amount: Money,
    val yearly: Money,
    val cycle: Cycle,
    val nextDate: LocalDate,
    /** The provider's cancellation page, when the bundled catalog knows it. */
    val cancelUrl: String? = null,
)

/**
 * The review flow: one subscription at a time, no list to scroll, and a summary that shows the
 * yearly value of the plans the user decided to cancel — labelled as a possible saving, because
 * marking a row cancelled is not the same as a provider confirming it.
 */
data class SubscriptionsReviewUiState(
    val loading: Boolean = true,
    val items: List<ReviewItem> = emptyList(),
    val kept: Int = 0,
    val rarely: Int = 0,
    val cancelled: Int = 0,
    val yearlySaving: Money = Money.zero(CurrencyCode("EUR")),
    val finished: Boolean = false,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val decidedIds: Set<Long> = emptySet(),
    /**
     * Increments on every cancel, so the screen raises a fresh Undo snackbar even when two
     * cancellations land back to back. Zero before the first one.
     */
    val cancelUndoGeneration: Long = 0L,
) {
    /** The subscription on screen, or null when the flow is empty or finished. */
    val current: ReviewItem? get() = items.firstOrNull { it.id !in decidedIds }
    val answered: Int get() = kept + rarely + cancelled
    val progress: Float
        get() = if (items.isEmpty()) 1f else answered.toFloat() / items.size.toFloat()
}

/**
 * Keeps the answer, applies it to the subscription immediately (so a closed flow never loses an
 * answer) and moves on. Cancelling also removes the renewal reminders.
 */
class SubscriptionsReviewViewModel(
    private val recurrings: RecurringRepository,
    private val reminders: ReminderScheduler,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val items = MutableStateFlow<List<ReviewItem>>(emptyList())
    private val decisions = MutableStateFlow<Map<Long, ReviewDecision>>(emptyMap())
    private val cancelGeneration = MutableStateFlow(0L)
    private var pendingCancel: PendingCancel? = null
    private var started = false

    /** The row a CANCEL replaced, kept while its snackbar can still Undo. */
    private data class PendingCancel(
        val id: Long,
        val original: Recurring,
        val generation: Long,
    )

    val uiState: StateFlow<SubscriptionsReviewUiState> = combine(
        items,
        decisions,
        cancelGeneration,
        settings.observeBaseCurrency(),
    ) { list, decided, generation, currency ->
        val cancelled = list.filter { decided[it.id] == ReviewDecision.CANCEL }
        val saving = cancelled.fold(Money.zero(currency)) { acc, item -> acc + item.yearly }
        SubscriptionsReviewUiState(
            loading = false,
            items = list,
            kept = list.count { decided[it.id] == ReviewDecision.KEEP },
            rarely = list.count { decided[it.id] == ReviewDecision.RARELY },
            cancelled = cancelled.size,
            yearlySaving = saving,
            finished = list.isNotEmpty() && list.all { it.id in decided },
            today = LocalDate.now(clock),
            decidedIds = decided.keys,
            cancelUndoGeneration = generation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionsReviewUiState(),
    )

    /** Loads the queue once: subscriptions due for a review, the most expensive first. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val all = recurrings.observeAll().first()
            val today = LocalDate.now(clock)
            items.value = all
                .filter { SubscriptionReview.isDue(it, today) }
                .sortedByDescending { CostNormalizer.yearly(it).minor }
                .map {
                    ReviewItem(
                        id = it.id,
                        name = it.name,
                        brandKey = it.brandKey,
                        amount = it.amount,
                        yearly = CostNormalizer.yearly(it),
                        cycle = it.cycle,
                        nextDate = it.nextDate,
                        cancelUrl = it.cancelUrl,
                    )
                }
        }
    }

    fun choose(decision: ReviewDecision) {
        val current = uiState.value.current ?: return
        decisions.update { it + (current.id to decision) }
        viewModelScope.launch {
            val recurring = recurrings.get(current.id) ?: return@launch
            val today = LocalDate.now(clock)
            val updated = when (decision) {
                ReviewDecision.KEEP -> recurring.copy(worth = Worth.LOVE, worthRatedOn = today)
                ReviewDecision.RARELY -> recurring.copy(worth = Worth.RARELY, worthRatedOn = today)
                ReviewDecision.CANCEL -> recurring.copy(
                    status = RecurringStatus.CANCELLED,
                    worth = recurring.worth ?: Worth.UNUSED,
                    worthRatedOn = if (recurring.worth == null) today else recurring.worthRatedOn,
                )
            }
            recurrings.upsert(updated)
            if (decision == ReviewDecision.CANCEL) {
                reminders.cancelRecurringReminder(current.id)
                reminders.cancelTrialEnding(current.id)
                val generation = cancelGeneration.value + 1L
                pendingCancel = PendingCancel(current.id, recurring, generation)
                cancelGeneration.value = generation
            }
        }
    }

    /**
     * Steps back from the last cancellation: restores the row exactly as it was (status, worth,
     * rating date) and its reminders, then puts the item back in the queue so the user can answer
     * again. The review screen raises this from the snackbar, per DESIGN's deletes-with-undo rule.
     */
    fun undoCancel() {
        val pending = pendingCancel ?: return
        pendingCancel = null
        viewModelScope.launch {
            recurrings.upsert(pending.original)
            if (pending.original.status == RecurringStatus.ACTIVE ||
                pending.original.status == RecurringStatus.TRIAL
            ) {
                reminders.scheduleRecurringReminder(
                    pending.id,
                    pending.original.nextDate,
                    pending.original.remindDaysBefore,
                )
            }
            pending.original.trialEndsOn?.let { reminders.scheduleTrialEnding(pending.id, it) }
            decisions.update { it - pending.id }
        }
    }

    /** The snackbar for [generation] went away; a later Undo must not resurrect a stale cancel. */
    fun acknowledgeCancelUndo(generation: Long) {
        if (pendingCancel?.generation == generation) pendingCancel = null
    }
}
