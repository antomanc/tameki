package dev.antomanc.tameki.core.data.seed

import android.content.Context
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.antomanc.tameki.core.data.R
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.db.entity.AccountEntity
import dev.antomanc.tameki.core.data.db.entity.CategoryEntity
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.DefaultAccounts
import dev.antomanc.tameki.core.model.entity.DefaultCategories
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * First-launch seed: default categories (by seedKey) plus Cash and Card.
 * Names are not frozen — the UI localises from [seedKey]. Currency uses the app locale when
 * seeding; onboarding may overwrite account currencies with the chosen base.
 */
class DatabaseSeeder {
    suspend fun seedIfEmpty(
        database: TamekiDatabase,
        context: Context,
        currency: CurrencyCode? = null,
    ) {
        withContext(Dispatchers.IO) {
            if (database.categoryDao().count() > 0) return@withContext
            database.withTransaction {
                val locale = appLocale(context)
                DefaultCategories.materialize(locale.language).forEach { category ->
                    database.categoryDao().upsert(
                        CategoryEntity.from(category.copy(id = 0L)),
                    )
                }
                if (database.accountDao().count() == 0) {
                    val code = currency ?: baseCurrencyFor(locale)
                    insertDefaultAccounts(database, context, code)
                }
            }
        }
    }

    /**
     * Room [onCreate] hook — Room opens the DB off the main thread, so [runBlocking] here is safe.
     * Seeds via SupportSQLiteDatabase so it stays inside the create callback.
     */
    fun seedOnCreate(db: SupportSQLiteDatabase, context: Context) {
        val count = db.query("SELECT COUNT(*) FROM categories").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        if (count > 0) return
        val locale = appLocale(context)
        DefaultCategories.materialize(locale.language).forEach { category ->
            db.execSQL(
                """
                INSERT INTO categories (name, kind, icon, color, parentId, archived, seedKey, customName)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    category.name,
                    category.kind.name,
                    category.icon,
                    category.color,
                    category.parentId,
                    if (category.archived) 1 else 0,
                    category.seedKey,
                    category.customName,
                ),
            )
        }
        val accountCount = db.query("SELECT COUNT(*) FROM accounts").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        if (accountCount == 0) {
            val currency = baseCurrencyFor(locale).value
            db.execSQL(
                "INSERT INTO accounts (name, currency, type, archived, sortOrder, seedKey, customName) VALUES (?, ?, ?, 0, 0, ?, NULL)",
                arrayOf(
                    context.getString(R.string.data_account_cash),
                    currency,
                    AccountType.CASH.name,
                    DefaultAccounts.CASH,
                ),
            )
            db.execSQL(
                "INSERT INTO accounts (name, currency, type, archived, sortOrder, seedKey, customName) VALUES (?, ?, ?, 0, 1, ?, NULL)",
                arrayOf(
                    context.getString(R.string.data_account_card),
                    currency,
                    AccountType.CARD.name,
                    DefaultAccounts.CARD,
                ),
            )
        }
    }

    private suspend fun insertDefaultAccounts(
        database: TamekiDatabase,
        context: Context,
        currency: CurrencyCode,
    ) {
        database.accountDao().upsert(
            AccountEntity(
                name = context.getString(R.string.data_account_cash),
                currency = currency.value,
                type = AccountType.CASH.name,
                sortOrder = 0,
                seedKey = DefaultAccounts.CASH,
            ),
        )
        database.accountDao().upsert(
            AccountEntity(
                name = context.getString(R.string.data_account_card),
                currency = currency.value,
                type = AccountType.CARD.name,
                sortOrder = 1,
                seedKey = DefaultAccounts.CARD,
            ),
        )
    }

    companion object {
        /** App configuration locale (per-app language), not the JVM default. */
        fun appLocale(context: Context): Locale =
            context.resources.configuration.locales[0] ?: Locale.getDefault()

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
