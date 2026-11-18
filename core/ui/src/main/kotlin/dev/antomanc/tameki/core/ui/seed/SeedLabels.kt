package dev.antomanc.tameki.core.ui.seed

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.DefaultAccounts
import dev.antomanc.tameki.core.ui.R

/** Resolves seedKey → string resource for categories and default accounts. */
object SeedLabels {
    @StringRes
    fun categoryRes(seedKey: String): Int? = when (seedKey) {
        "groceries" -> R.string.ui_seed_category_groceries
        "restaurant" -> R.string.ui_seed_category_restaurant
        "coffee" -> R.string.ui_seed_category_coffee
        "transport" -> R.string.ui_seed_category_transport
        "fuel" -> R.string.ui_seed_category_fuel
        "home" -> R.string.ui_seed_category_home
        "utilities" -> R.string.ui_seed_category_utilities
        "phone" -> R.string.ui_seed_category_phone
        "internet" -> R.string.ui_seed_category_internet
        "health" -> R.string.ui_seed_category_health
        "pharmacy" -> R.string.ui_seed_category_pharmacy
        "sport" -> R.string.ui_seed_category_sport
        "clothes" -> R.string.ui_seed_category_clothes
        "entertainment" -> R.string.ui_seed_category_entertainment
        "streaming" -> R.string.ui_seed_category_streaming
        "other_expense" -> R.string.ui_seed_category_other_expense
        "salary" -> R.string.ui_seed_category_salary
        "refund" -> R.string.ui_seed_category_refund
        "investment" -> R.string.ui_seed_category_investment
        "gifts_income" -> R.string.ui_seed_category_gifts_income
        "other_income" -> R.string.ui_seed_category_other_income
        else -> null
    }

    @StringRes
    fun accountRes(seedKey: String): Int? = when (seedKey) {
        DefaultAccounts.CASH -> R.string.ui_seed_account_cash
        DefaultAccounts.CARD -> R.string.ui_seed_account_card
        else -> null
    }

    fun category(context: Context, category: Category): String {
        category.customName?.takeIf { it.isNotBlank() }?.let { return it }
        category.seedKey?.let { key ->
            categoryRes(key)?.let { return context.getString(it) }
        }
        return category.name
    }

    fun account(context: Context, account: Account): String {
        account.customName?.takeIf { it.isNotBlank() }?.let { return it }
        account.seedKey?.let { key ->
            accountRes(key)?.let { return context.getString(it) }
        }
        return account.name
    }
}

@Composable
fun Category.displayName(): String {
    customName?.takeIf { it.isNotBlank() }?.let { return it }
    seedKey?.let { key ->
        SeedLabels.categoryRes(key)?.let { return stringResource(it) }
    }
    return name
}

@Composable
fun Account.displayName(): String {
    customName?.takeIf { it.isNotBlank() }?.let { return it }
    seedKey?.let { key ->
        SeedLabels.accountRes(key)?.let { return stringResource(it) }
    }
    return name
}
