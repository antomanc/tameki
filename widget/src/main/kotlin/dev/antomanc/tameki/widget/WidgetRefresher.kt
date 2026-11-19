package dev.antomanc.tameki.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Keeps the month-summary widget in step with the journal. Without it the widget only refreshed
 * on the system's 30-minute tick, so a spend logged in the app was missing from the home screen.
 * Writes are batched (a quick add, its undo and an inbox accept within a second redraw once).
 */
object WidgetRefresher {
    @OptIn(FlowPreview::class)
    fun start(
        context: Context,
        scope: CoroutineScope,
        transactions: TransactionRepository,
        budgets: BudgetRepository,
        recurrings: RecurringRepository,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            combine(transactions.observe(), budgets.observeAll(), recurrings.observeAll()) { _, _, _ -> Unit }
                // The first emission is the state the widget was already drawn from.
                .drop(1)
                .debounce(1_000)
                .collect { runCatching { MonthSummaryWidget().updateAll(appContext) } }
        }
    }
}
