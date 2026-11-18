package dev.antomanc.tameki.core.data.seed

import android.content.Context
import dev.antomanc.tameki.core.data.R
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.entity.AccountEntity
import dev.antomanc.tameki.core.data.db.entity.CategoryEntity
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.DefaultCategories
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.runBlocking

/**
 * First-launch seed: localized default categories plus Cash and Card in the locale currency.
 * Safe to call repeatedly — no-ops when categories already exist.
 */
class DatabaseSeeder {
    fun seedIfEmpty(database: TamekiDatabase, context: Context) {
        runBlocking {
            if (database.categoryDao().count() > 0) return@runBlocking
            val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
            val language = locale.language
            DefaultCategories.materialize(language).forEach { category ->
                database.categoryDao().upsert(
                    CategoryEntity.from(category.copy(id = 0L)),
                )
            }
            if (database.accountDao().count() == 0) {
                val currency = baseCurrencyFor(locale)
                database.accountDao().upsert(
                    AccountEntity(
                        name = context.getString(R.string.data_account_cash),
                        currency = currency.value,
                        type = AccountType.CASH.name,
                        sortOrder = 0,
                    ),
                )
                database.accountDao().upsert(
                    AccountEntity(
                        name = context.getString(R.string.data_account_card),
                        currency = currency.value,
                        type = AccountType.CARD.name,
                        sortOrder = 1,
                    ),
                )
            }
        }
    }

    companion object {
        fun baseCurrencyFor(locale: Locale): CurrencyCode {
            val code = try {
                Currency.getInstance(locale).currencyCode
            } catch (_: Exception) {
                "EUR"
            }
            return CurrencyCode.parse(code) ?: CurrencyCode("EUR")
        }
    }
}
