package dev.antomanc.tameki.feature.home

import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.insight.Insight

/** Short copy for the single Today insight card. Restraint: title is the explanation. */
object InsightPresentation {
    fun present(insight: Insight, recurringName: (Long) -> String?, categoryName: (Long) -> String?): HomeInsight {
        val (title, body, action, key) = when (insight) {
            is Insight.UnusedSubscription -> {
                val name = recurringName(insight.recurringId) ?: "Subscription"
                val why = when (insight.worth) {
                    Worth.UNUSED -> "Marked unused"
                    Worth.RARELY -> "Rarely used"
                    else -> "Low value"
                }
                Quad("$name", why, "Review", "streaming")
            }
            is Insight.OverlappingSubscriptions -> {
                val cat = categoryName(insight.categoryId) ?: "category"
                Quad("Overlap in $cat", "${insight.recurringIds.size} active", "Review", "streaming")
            }
            is Insight.AnnualPlanSaving -> {
                val name = recurringName(insight.recurringId) ?: "Plan"
                Quad("Yearly $name", "Cheaper than monthly", "See plan", "streaming")
            }
            is Insight.PriceIncrease -> {
                val name = recurringName(insight.recurringId) ?: "Subscription"
                Quad("$name went up", "Compared with the previous price", "Review", "streaming")
            }
            is Insight.TrialEnding -> {
                val name = recurringName(insight.recurringId) ?: "Trial"
                Quad("$name trial ends", "Ends ${insight.endsOn}", "Decide", "streaming")
            }
            is Insight.CategorySpike -> {
                val cat = categoryName(insight.categoryId) ?: "Category"
                Quad("$cat above average", "This month vs last 3", "See activity", cat.lowercase())
            }
            is Insight.SmallFrequentSpend -> {
                Quad(
                    insight.merchantOrCategory,
                    "${insight.count}× this month",
                    "See activity",
                    "coffee",
                )
            }
            is Insight.BudgetAtRisk -> Quad("Budget at risk", "Ahead of pace", "See budget", "other")
            is Insight.NoSpendStreak -> Quad(
                "${insight.days}-day streak",
                "No spending",
                "Keep going",
                "other",
            )
        }
        return HomeInsight(
            insight = insight,
            title = title,
            body = body,
            actionLabel = action,
            categoryKey = key,
        )
    }

    private data class Quad(val title: String, val body: String, val action: String, val key: String)
}
