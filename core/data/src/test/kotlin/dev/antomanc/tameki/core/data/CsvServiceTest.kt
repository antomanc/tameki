package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.csv.CsvServiceImpl
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.repository.CsvColumns
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CsvServiceTest : DbTestRule() {
    private lateinit var csv: CsvServiceImpl
    private val eur = CurrencyCode("EUR")

    @Before
    fun initCsv() {
        val settings = SettingsRepositoryImpl(context, defaultCurrency = eur)
        csv = CsvServiceImpl(transactions, accounts, categories, settings)
    }

    @Test
    fun importsItalianBankExport() = runTest {
        accounts.upsert(Account(name = "Carta", currency = eur, type = AccountType.CARD))
        categories.upsert(
            Category(name = "Spesa", kind = CategoryKind.EXPENSE, icon = "groceries", color = 1L),
        )
        val file = File(context.cacheDir, "it.csv")
        file.writeText(
            """
            Data;Importo;Valuta;Descrizione;Categoria;Note;Conto
            18/11/2026;12,50;EUR;Esselunga;Spesa;latte;Carta
            17/11/2026;1.234,56;EUR;Ikea;Spesa;;Carta
            """.trimIndent(),
        )
        val mapping = mapOf(
            CsvColumns.DATE to "Data",
            CsvColumns.AMOUNT to "Importo",
            CsvColumns.CURRENCY to "Valuta",
            CsvColumns.MERCHANT to "Descrizione",
            CsvColumns.CATEGORY to "Categoria",
            CsvColumns.NOTE to "Note",
            CsvColumns.ACCOUNT to "Conto",
        )
        val preview = csv.previewImport(file, mapping)
        assertThat(preview.rows).hasSize(2)
        assertThat(preview.rows[0].amount?.minor).isEqualTo(1_250L)
        assertThat(preview.rows[1].amount?.minor).isEqualTo(123_456L)

        val imported = csv.importTransactions(file, mapping)
        assertThat(imported).isEqualTo(2)
        assertThat(transactions.observe().first()).hasSize(2)
    }

    @Test
    fun importsEnglishBankExport() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        categories.upsert(
            Category(name = "Groceries", kind = CategoryKind.EXPENSE, icon = "groceries", color = 1L),
        )
        val file = File(context.cacheDir, "en.csv")
        file.writeText(
            """
            Date,Amount,Currency,Merchant,Category,Note,Account
            2026-11-18,"1,234.56",EUR,Whole Foods,Groceries,weekly,Card
            11/17/2026,9.99,EUR,Netflix,Groceries,,Card
            """.trimIndent(),
        )
        val mapping = mapOf(
            CsvColumns.DATE to "Date",
            CsvColumns.AMOUNT to "Amount",
            CsvColumns.CURRENCY to "Currency",
            CsvColumns.MERCHANT to "Merchant",
            CsvColumns.CATEGORY to "Category",
            CsvColumns.NOTE to "Note",
            CsvColumns.ACCOUNT to "Account",
        )
        val preview = csv.previewImport(file, mapping)
        assertThat(preview.rows[0].amount?.minor).isEqualTo(123_456L)
        assertThat(preview.rows[1].amount?.minor).isEqualTo(999L)
        assertThat(csv.importTransactions(file, mapping)).isEqualTo(2)
    }
}
