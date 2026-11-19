package dev.antomanc.tameki.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.antomanc.tameki.core.model.entity.Worth
import dev.antomanc.tameki.core.model.insight.Insight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The data behind the Today insight card, with no user-visible words in it. Copy is built by
 * [copy] from resources, so Italian and English stop diverging and dates are never ISO strings.
 */
sealed interface HomeInsightKind {
    data class Unused(val name: String?, val worth: Worth) : HomeInsightKind
    data class Overlap(val categoryName: String?, val count: Int) : HomeInsightKind
    data class AnnualPlan(val name: String?) : HomeInsightKind
    data class PriceIncrease(val name: String?) : HomeInsightKind
    data class TrialEnding(val name: String?, val endsOn: LocalDate) : HomeInsightKind
    data class CategorySpike(val categoryName: String?) : HomeInsightKind
    data class SmallFrequent(val label: String, val count: Int) : HomeInsightKind
    data object BudgetAtRisk : HomeInsightKind
    data class NoSpendStreak(val days: Int) : HomeInsightKind
}

/** A localized title, supporting fact and single action label for [InsightCard]. */
data class HomeInsightCopy(val title: String, val body: String, val actionLabel: String)

/** Short copy for the single Today insight card. Restraint: title is the explanation. */
object InsightPresentation {
    fun present(insight: Insight, recurringName: (Long) -> String?, categoryName: (Long) -> String?): HomeInsight {
        val (kind, key) = when (insight) {
            is Insight.UnusedSubscription -> HomeInsightKind.Unused(
                recurringName(insight.recurringId),
                insight.worth,
            ) to "streaming"
            is Insight.OverlappingSubscriptions -> HomeInsightKind.Overlap(
                categoryName(insight.categoryId),
                insight.recurringIds.size,
            ) to "streaming"
            is Insight.AnnualPlanSaving -> HomeInsightKind.AnnualPlan(
                recurringName(insight.recurringId),
            ) to "streaming"
            is Insight.PriceIncrease -> HomeInsightKind.PriceIncrease(
                recurringName(insight.recurringId),
            ) to "streaming"
            is Insight.TrialEnding -> HomeInsightKind.TrialEnding(
                recurringName(insight.recurringId),
                insight.endsOn,
            ) to "streaming"
            is Insight.CategorySpike -> HomeInsightKind.CategorySpike(
                categoryName(insight.categoryId),
            ) to (categoryName(insight.categoryId) ?: "other").lowercase()
            is Insight.SmallFrequentSpend -> HomeInsightKind.SmallFrequent(
                insight.merchantOrCategory,
                insight.count,
            ) to "coffee"
            is Insight.BudgetAtRisk -> HomeInsightKind.BudgetAtRisk to "other"
            is Insight.NoSpendStreak -> HomeInsightKind.NoSpendStreak(insight.days) to "other"
        }
        return HomeInsight(insight = insight, kind = kind, categoryKey = key)
    }
}

/** Turns the structured kind into the one sentence and one fact the card shows, in the UI locale. */
@Composable
fun HomeInsight.localizedCopy(): HomeInsightCopy {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val subscription = stringResource(R.string.home_insight_fallback_subscription)
    val category = stringResource(R.string.home_insight_fallback_category)
    return when (val kind = kind) {
        is HomeInsightKind.Unused -> HomeInsightCopy(
            title = kind.name ?: subscription,
            body = stringResource(
                when (kind.worth) {
                    Worth.UNUSED -> R.string.home_insight_marked_unused
                    Worth.RARELY -> R.string.home_insight_rarely_used
                    else -> R.string.home_insight_low_value
                },
            ),
            actionLabel = stringResource(R.string.home_action_review),
        )
        is HomeInsightKind.Overlap -> HomeInsightCopy(
            title = stringResource(R.string.home_insight_overlap, kind.categoryName ?: category),
            body = pluralStringResource(R.plurals.home_insight_active_count, kind.count, kind.count),
            actionLabel = stringResource(R.string.home_action_review),
        )
        is HomeInsightKind.AnnualPlan -> HomeInsightCopy(
            title = stringResource(R.string.home_insight_yearly_plan, kind.name ?: subscription),
            body = stringResource(R.string.home_insight_cheaper_monthly),
            actionLabel = stringResource(R.string.home_action_see_plan),
        )
        is HomeInsightKind.PriceIncrease -> HomeInsightCopy(
            title = stringResource(R.string.home_insight_price_up, kind.name ?: subscription),
            body = stringResource(R.string.home_insight_compared_previous),
            actionLabel = stringResource(R.string.home_action_review),
        )
        is HomeInsightKind.TrialEnding -> HomeInsightCopy(
            title = stringResource(R.string.home_insight_trial_ends, kind.name ?: subscription),
            body = stringResource(R.string.home_insight_ends_on, formatter.format(kind.endsOn)),
            actionLabel = stringResource(R.string.home_action_decide),
        )
        is HomeInsightKind.CategorySpike -> HomeInsightCopy(
            title = stringResource(R.string.home_insight_category_above, kind.categoryName ?: category),
            body = stringResource(R.string.home_insight_vs_three_months),
            actionLabel = stringResource(R.string.home_action_see_activity),
        )
        is HomeInsightKind.SmallFrequent -> HomeInsightCopy(
            title = kind.label,
            body = pluralStringResource(R.plurals.home_insight_purchases, kind.count, kind.count),
            actionLabel = stringResource(R.string.home_action_see_activity),
        )
        HomeInsightKind.BudgetAtRisk -> HomeInsightCopy(
            title = stringResource(R.string.home_insight_budget_risk),
            body = stringResource(R.string.home_insight_ahead_pace),
            actionLabel = stringResource(R.string.home_action_see_budget),
        )
        is HomeInsightKind.NoSpendStreak -> HomeInsightCopy(
            title = pluralStringResource(R.plurals.home_insight_streak, kind.days, kind.days),
            body = stringResource(R.string.home_insight_no_spending),
            actionLabel = stringResource(R.string.home_action_keep_going),
        )
    }
}
