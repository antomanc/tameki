package dev.antomanc.tameki.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MonthSummaryWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load()
        provideContent {
            GlanceTheme {
                MonthSummaryContent(data)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val eur = CurrencyCode("EUR")
        provideContent {
            GlanceTheme {
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
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = context.getString(R.string.widget_available),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Text(
            text = formatMoney(data.available),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        // Simple pace bar as two stacked rows of weight via fill fraction text
        Text(
            text = context.getString(R.string.widget_pace, (pace * 100).toInt()),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        Spacer(GlanceModifier.height(8.dp))
        val renewal = data.nextRenewalName
        if (renewal != null && data.nextRenewalDate != null) {
            Text(
                text = context.getString(
                    R.string.widget_next_renewal,
                    renewal,
                    data.nextRenewalDate.toString(),
                ),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
                maxLines = 1,
            )
        }
    }
}

private fun formatMoney(money: Money): String {
    val whole = money.minor / 100
    val frac = (money.minor % 100).toString().padStart(2, '0')
    return "€$whole,$frac"
}

class MonthSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthSummaryWidget()
}
