package dev.antomanc.tameki.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.recurring.CostNormalizer
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import java.time.Clock
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
    val nextDate: java.time.LocalDate,
)

/**
 * The review flow: one subscription at a time, no list to scroll, and a summary that counts only
 * the money the user actually decided to stop paying.
 */
data class SubscriptionsReviewUiState(
    val loading: Boolean = true,
    val items: List<ReviewItem> = emptyList(),
    val kept: Int = 0,
    val rarely: Int = 0,
    val cancelled: Int = 0,
    val yearlySaving: Money = Money.zero(CurrencyCode("EUR")),
    val finished: Boolean = false,
    val today: java.time.LocalDate = java.time.LocalDate.of(1970, 1, 1),
    val decidedIds: Set<Long> = emptySet(),
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
    private var started = false

    val uiState: StateFlow<SubscriptionsReviewUiState> = combine(
        items,
        decisions,
        settings.observeBaseCurrency(),
    ) { list, decided, currency ->
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
            today = java.time.LocalDate.now(clock),
            decidedIds = decided.keys,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionsReviewUiState(),
    )

    /** Loads the queue once: active subscriptions, the most expensive first. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val all = recurrings.observeAll().first()
            items.value = all
                .filter {
                    it.type == RecurringType.SUBSCRIPTION &&
                        (it.status == RecurringStatus.ACTIVE || it.status == RecurringStatus.TRIAL)
                }
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
                    )
                }
        }
    }

    fun choose(decision: ReviewDecision) {
        val current = uiState.value.current ?: return
        decisions.update { it + (current.id to decision) }
        viewModelScope.launch {
            val recurring = recurrings.get(current.id) ?: return@launch
            val updated = when (decision) {
                ReviewDecision.KEEP -> recurring.copy(worth = Worth.LOVE)
                ReviewDecision.RARELY -> recurring.copy(worth = Worth.RARELY)
                ReviewDecision.CANCEL -> recurring.copy(
                    status = RecurringStatus.CANCELLED,
                    worth = recurring.worth ?: Worth.UNUSED,
                )
            }
            recurrings.upsert(updated)
            if (decision == ReviewDecision.CANCEL) {
                reminders.cancelRecurringReminder(current.id)
                reminders.cancelTrialEnding(current.id)
            }
        }
    }
}
