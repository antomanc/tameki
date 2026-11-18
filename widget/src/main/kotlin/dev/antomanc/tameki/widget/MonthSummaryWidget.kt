package dev.antomanc.tameki.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
        return MonthSummaryModel.compute(budgets, txs, recurrings, today, base)
    }
}

@Composable
private fun MonthSummaryContent(data: MonthSummaryWidgetData) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val pace = MonthSummaryModel.paceFraction(data.spent, data.pool, today)
    val overPace = pace > 1f
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetPaper.surface())
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = context.getString(R.string.widget_available),
            style = TextStyle(color = WidgetPaper.muted(), fontSize = 12.sp),
        )
        Text(
            text = formatWidgetMoney(data.available, Locale.getDefault()),
            style = TextStyle(
                color = WidgetPaper.ink(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.widget_pace, (pace * 100).toInt().coerceAtMost(999)),
            style = TextStyle(
                color = if (overPace) WidgetPaper.amber() else WidgetPaper.muted(),
                fontSize = 12.sp,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        val renewal = data.nextRenewalName
        if (renewal != null && data.nextRenewalDate != null) {
            val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
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
