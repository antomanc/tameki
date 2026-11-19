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
import dev.antomanc.tameki.core.model.recurring.RecurringLifecycle
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.toBaseOrNull
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
    /** [yearly] in the base currency, or null when no rate is known (never counted as a saving). */
    val yearlyInBase: Money? = yearly,
)

/**
 * Where the review is. Loading → NothingDue, or Loading → Asking → … → Summary; an Undo from the
 * summary steps back into Asking for the restored item.
 */
enum class ReviewPhase { Loading, NothingDue, Asking, Summary }

/**
 * The review flow: one subscription at a time, no list to scroll, and a summary that shows the
 * yearly value of the plans the user decided to cancel — labelled as a possible saving, because
 * marking a row cancelled is not the same as a provider confirming it.
 */
data class SubscriptionsReviewUiState(
    /** The queue, null until it has been loaded once. */
    val queue: List<ReviewItem>? = null,
    val kept: Int = 0,
    val rarely: Int = 0,
    val cancelled: Int = 0,
    val yearlySaving: Money = Money.zero(CurrencyCode("EUR")),
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val decidedIds: Set<Long> = emptySet(),
    /** The cancel the snackbar can still undo, or null. A new token is a new snackbar. */
    val undoToken: Long? = null,
) {
    val items: List<ReviewItem> get() = queue.orEmpty()

    val phase: ReviewPhase
        get() = when {
            queue == null -> ReviewPhase.Loading
            queue.isEmpty() -> ReviewPhase.NothingDue
            queue.all { it.id in decidedIds } -> ReviewPhase.Summary
            else -> ReviewPhase.Asking
        }

    val loading: Boolean get() = phase == ReviewPhase.Loading
    val finished: Boolean get() = phase == ReviewPhase.Summary

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
    private val settings: SettingsRepository,
    private val exchangeRates: ExchangeRates,
    private val clock: Clock,
) : ViewModel() {
    private val items = MutableStateFlow<List<ReviewItem>?>(null)
    private val decisions = MutableStateFlow<Map<Long, ReviewDecision>>(emptyMap())
    private val pendingCancel = MutableStateFlow<PendingCancel?>(null)
    private var nextToken = 0L
    private var started = false

    /** The row a CANCEL replaced, kept while its snackbar can still Undo. */
    private data class PendingCancel(
        val id: Long,
        val original: Recurring,
        val token: Long,
    )

    val uiState: StateFlow<SubscriptionsReviewUiState> = combine(
        items,
        decisions,
        pendingCancel,
        settings.observeBaseCurrency(),
    ) { list, decided, pending, currency ->
        val queue = list.orEmpty()
        val cancelled = queue.filter { decided[it.id] == ReviewDecision.CANCEL }
        // Only amounts in the base currency add up: a plan with no known rate is left out of the
        // saving rather than mixed in (or crashing the sum).
        val saving = cancelled.mapNotNull { it.yearlyInBase }
            .filter { it.currency == currency }
            .fold(Money.zero(currency)) { acc, money -> acc + money }
        SubscriptionsReviewUiState(
            queue = list,
            kept = queue.count { decided[it.id] == ReviewDecision.KEEP },
            rarely = queue.count { decided[it.id] == ReviewDecision.RARELY },
            cancelled = cancelled.size,
            yearlySaving = saving,
            today = LocalDate.now(clock),
            decidedIds = decided.keys,
            undoToken = pending?.token,
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
            val base = settings.observeBaseCurrency().first()
            val today = LocalDate.now(clock)
            items.value = all
                .filter { SubscriptionReview.isDue(it, today) }
                .map {
                    val yearly = CostNormalizer.yearly(it)
                    ReviewItem(
                        id = it.id,
                        name = it.name,
                        brandKey = it.brandKey,
                        amount = it.amount,
                        yearly = yearly,
                        cycle = it.cycle,
                        nextDate = RecurringLifecycle.settle(it, today).nextDate,
                        cancelUrl = it.cancelUrl,
                        yearlyInBase = exchangeRates.toBaseOrNull(yearly, base, today),
                    )
                }
                .sortedByDescending { (it.yearlyInBase ?: it.yearly).minor }
        }
    }

    /**
     * Answers the item on screen. [itemId] is the item the user was looking at when they tapped:
     * a fast double tap would otherwise answer the next subscription without it ever being read.
     */
    fun choose(decision: ReviewDecision, itemId: Long? = null) {
        val current = uiState.value.current ?: return
        if (itemId != null && itemId != current.id) return
        if (current.id in decisions.value) return
        decisions.update { it + (current.id to decision) }
        viewModelScope.launch {
            val recurring = recurrings.get(current.id) ?: return@launch
            val today = LocalDate.now(clock)
            val updated = when (decision) {
                ReviewDecision.KEEP -> recurring.copy(worth = Worth.LOVE, worthRatedOn = today)
                ReviewDecision.RARELY -> recurring.copy(worth = Worth.RARELY, worthRatedOn = today)
                ReviewDecision.CANCEL -> RecurringLifecycle.cancel(recurring).copy(
                    worth = recurring.worth ?: Worth.UNUSED,
                    worthRatedOn = if (recurring.worth == null) today else recurring.worthRatedOn,
                )
            }
            recurrings.upsert(updated)
            if (decision == ReviewDecision.CANCEL) {
                reminders.cancelRecurringReminder(current.id)
                reminders.cancelTrialEnding(current.id)
                pendingCancel.value = PendingCancel(current.id, recurring, ++nextToken)
            }
        }
    }

    /**
     * Steps back from the last cancellation: restores the row exactly as it was (status, worth,
     * rating date) and its reminders, then puts the item back in the queue so the user can answer
     * again. The review screen raises this from the snackbar, per DESIGN's deletes-with-undo rule.
     */
    fun undoCancel(token: Long) {
        val pending = pendingCancel.value?.takeIf { it.token == token } ?: return
        pendingCancel.value = null
        viewModelScope.launch {
            recurrings.upsert(pending.original)
            val plan = RecurringLifecycle.reminders(pending.original, LocalDate.now(clock))
            plan.renewal?.let { (on, days) -> reminders.scheduleRecurringReminder(pending.id, on, days) }
            plan.trialEnding?.let { reminders.scheduleTrialEnding(pending.id, it) }
            decisions.update { it - pending.id }
        }
    }

    /** The snackbar for [token] went away; a later Undo must not resurrect a stale cancel. */
    fun acknowledgeCancelUndo(token: Long) {
        pendingCancel.update { if (it?.token == token) null else it }
    }
}
