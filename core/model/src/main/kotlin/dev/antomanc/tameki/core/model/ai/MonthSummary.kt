package dev.antomanc.tameki.core.model.ai

import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.insight.Insight
import java.time.YearMonth

/**
 * Snapshot fed into [AiPrompts.monthReview] so the chatbot sees the same numbers as the app.
 */
data class MonthSummary(
    val month: YearMonth,
    val totalExpenses: Money,
    val totalIncome: Money,
    val byCategory: List<Pair<String, Money>>,
    val subscriptions: List<Pair<String, Money>>,
    val insights: List<Insight>,
) {
    fun toMarkdown(): String = buildString {
        appendLine("# ${month.year}-${month.monthValue.toString().padStart(2, '0')}")
        appendLine()
        appendLine("- Expenses: ${format(totalExpenses)}")
        appendLine("- Income: ${format(totalIncome)}")
        appendLine()
        appendLine("## Categories")
        for ((name, amount) in byCategory) {
            appendLine("- $name: ${format(amount)}")
        }
        appendLine()
        appendLine("## Subscriptions (monthly)")
        for ((name, amount) in subscriptions) {
            appendLine("- $name: ${format(amount)}")
        }
        if (insights.isNotEmpty()) {
            appendLine()
            appendLine("## Insights")
            for (insight in insights) {
                val saving = insight.yearlySaving?.let { format(it) } ?: "—"
                appendLine("- ${insight.id}: yearly saving $saving (${insight.severity})")
            }
        }
    }

    private fun format(money: Money): String {
        val digits = when (money.currency.value) {
            "JPY" -> 0
            else -> 2
        }
        return if (digits == 0) {
            "${money.minor} ${money.currency.value}"
        } else {
            val major = money.minor / 100.0 // money-ok: display-only in Markdown export for chatbots
            String.format(java.util.Locale.US, "%.2f %s", major, money.currency.value)
        }
    }
}
