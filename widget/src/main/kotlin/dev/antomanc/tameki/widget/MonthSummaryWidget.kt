package dev.antomanc.tameki.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.budget.HeroMode
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.toBaseOrNull
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionFilter
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MonthSummaryWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load()
        provideContent { MonthSummaryContent(data) }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val eur = CurrencyCode("EUR")
        provideContent {
            MonthSummaryContent(
                MonthSummaryWidgetData(
                    available = Money(42_000, eur),
                    mode = HeroMode.AVAILABLE,
                    spent = Money(58_000, eur),
                    pool = Money(100_000, eur),
                    nextRenewalName = "Netflix",
                    nextRenewalDate = LocalDate.of(2026, 12, 1),
                ),
            )
        }
    }

    private suspend fun load(): MonthSummaryWidgetData {
        val clock = get<Clock>()
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val settings = get<SettingsRepository>()
        val base = settings.observeBaseCurrency().first()
        val txs = get<TransactionRepository>().observe(
            TransactionFilter(from = month.atDay(1), to = month.atEndOfMonth()),
        ).first()
        val budgets = get<BudgetRepository>().observeAll().first()
        val recurrings = get<RecurringRepository>().observeAll().first()
        val rates = get<ExchangeRates>()
        // The widget must read the same figure as Today, including foreign-currency rows.
        val normalizedTx = txs.mapNotNull { tx ->
            val converted = rates.toBaseOrNull(tx.amount, base, tx.occurredAt.toLocalDate())
                ?: return@mapNotNull tx.takeIf { it.amount.currency == base }
            if (converted == tx.amount) tx else tx.copy(amount = converted)
        }
        val normalizedRecurrings = recurrings.mapNotNull { recurring ->
            val converted = rates.toBaseOrNull(recurring.amount, base, today)
                ?: return@mapNotNull recurring.takeIf { it.amount.currency == base }
            if (converted == recurring.amount) recurring else recurring.copy(amount = converted)
        }
        return MonthSummaryModel.compute(budgets, normalizedTx, normalizedRecurrings, today, base)
    }
}

@Composable
private fun MonthSummaryContent(data: MonthSummaryWidgetData) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val pace = MonthSummaryModel.paceFraction(data.spent, data.pool, today)
    val overPace = pace > 1f
    val openApp = Intent().setClassName(context.packageName, "dev.antomanc.tameki.MainActivity").apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetPaper.surface())
            .clickable(actionStartActivity(openApp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = context.getString(
                if (data.mode == HeroMode.SPENT_ONLY) {
                    R.string.widget_spent
                } else {
                    R.string.widget_available
                },
            ),
            style = TextStyle(color = WidgetPaper.muted(), fontSize = 12.sp),
        )
        Text(
            text = formatWidgetMoney(data.available, context.resources.configuration.locales[0]),
            style = TextStyle(
                color = WidgetPaper.ink(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        // Same rule as Today: without a budget or income there is no pace, so do not invent 0%.
        if (data.pool != null) {
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = context.getString(R.string.widget_pace, (pace * 100).toInt().coerceAtMost(999)),
                style = TextStyle(
                    color = if (overPace) WidgetPaper.amber() else WidgetPaper.muted(),
                    fontSize = 12.sp,
                ),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        val renewal = data.nextRenewalName
        if (renewal != null && data.nextRenewalDate != null) {
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(context.resources.configuration.locales[0])
                .format(data.nextRenewalDate)
            Text(
                text = context.getString(R.string.widget_next_renewal, renewal, date),
                style = TextStyle(color = WidgetPaper.ink(), fontSize = 13.sp),
                maxLines = 1,
            )
        }
    }
}

class MonthSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthSummaryWidget()
}
