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
import dev.antomanc.tameki.core.model.entity.DefaultCategories
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * First-launch seed: localized default categories plus Cash and Card in the locale currency.
 * Safe to call repeatedly — no-ops when categories already exist.
 * Runs atomically in one Room transaction; never blocks the main thread.
 */
class DatabaseSeeder {
    suspend fun seedIfEmpty(database: TamekiDatabase, context: Context) {
        withContext(Dispatchers.IO) {
            if (database.categoryDao().count() > 0) return@withContext
            database.withTransaction {
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
    }

    /**
     * Room [onCreate] hook — Room opens the DB off the main thread, so [runBlocking] here is safe.
     * Seeds via SupportSQLiteDatabase so it stays inside the create callback.
     */
    fun seedOnCreate(db: SupportSQLiteDatabase, context: Context) {
        // Room onCreate already runs in a transaction for the schema; we insert defaults with SQL.
        val count = db.query("SELECT COUNT(*) FROM categories").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
        if (count > 0) return
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val language = locale.language
        DefaultCategories.materialize(language).forEach { category ->
            db.execSQL(
                """
                INSERT INTO categories (name, kind, icon, color, parentId, archived)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    category.name,
                    category.kind.name,
                    category.icon,
                    category.color,
                    category.parentId,
                    if (category.archived) 1 else 0,
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
                "INSERT INTO accounts (name, currency, type, archived, sortOrder) VALUES (?, ?, ?, 0, 0)",
                arrayOf(context.getString(R.string.data_account_cash), currency, AccountType.CASH.name),
            )
            db.execSQL(
                "INSERT INTO accounts (name, currency, type, archived, sortOrder) VALUES (?, ?, ?, 0, 1)",
                arrayOf(context.getString(R.string.data_account_card), currency, AccountType.CARD.name),
            )
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
