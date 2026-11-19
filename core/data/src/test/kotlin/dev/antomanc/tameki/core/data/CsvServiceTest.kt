package dev.antomanc.tameki.core.data

import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.csv.CsvReader
import dev.antomanc.tameki.core.data.csv.CsvServiceImpl
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.entity.CategoryKind
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.model.repository.CsvDateFormat
import dev.antomanc.tameki.core.model.repository.CsvImportOptions
import dev.antomanc.tameki.core.model.repository.CsvProblemKind
import dev.antomanc.tameki.core.model.repository.CsvSignConvention
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
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
        csv = CsvServiceImpl(transactions, accounts, categories, settings, db, Locale.ITALY)
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
            Data;Importo;Valuta;Descrizione;Categoria;Note;Conto;Tipo
            18/11/2026;12,50;EUR;Esselunga;Spesa;latte;Carta;uscita
            17/11/2026;1.234,56;EUR;Ikea;Spesa;;Carta;expense
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
            CsvColumns.KIND to "Tipo",
        )
        val preview = csv.previewImport(file, mapping)
        assertThat(preview.rows).hasSize(2)
        assertThat(preview.rows[0].amount?.minor).isEqualTo(1_250L)
        assertThat(preview.rows[1].amount?.minor).isEqualTo(123_456L)
        assertThat(preview.resolvedDateFormat).isEqualTo(CsvDateFormat.DMY)

        val imported = csv.importTransactions(file, mapping)
        assertThat(imported.imported).isEqualTo(2)
        assertThat(transactions.observe().first()).hasSize(2)
        assertThat(transactions.observe().first().map { it.kind }).containsExactly(
            TransactionKind.EXPENSE,
            TransactionKind.EXPENSE,
        )
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
            11/17/2026,-9.99,EUR,Netflix,Groceries,,Card
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
        assertThat(preview.rows[1].amount?.minor).isEqualTo(-999L)
        val result = csv.importTransactions(file, mapping)
        assertThat(result.imported).isEqualTo(2)
        val kinds = transactions.observe().first().associateBy { it.merchant }
        // Default NegativeIsExpense: negative → expense, positive → income
        assertThat(kinds["Whole Foods"]!!.kind).isEqualTo(TransactionKind.INCOME)
        assertThat(kinds["Netflix"]!!.kind).isEqualTo(TransactionKind.EXPENSE)
    }

    @Test
    fun ambiguousDateFollowsExplicitFormatNotSilentDdMm() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val file = File(context.cacheDir, "ambig.csv")
        file.writeText(
            """
            Date,Amount
            04/05/2026,10.00
            """.trimIndent(),
        )
        val mapping = mapOf(CsvColumns.DATE to "Date", CsvColumns.AMOUNT to "Amount")
        val mdy = csv.previewImport(file, mapping, CsvImportOptions(dateFormat = CsvDateFormat.MDY))
        assertThat(mdy.rows.single().date).isEqualTo(LocalDate.of(2026, 4, 5))
        val dmy = csv.previewImport(file, mapping, CsvImportOptions(dateFormat = CsvDateFormat.DMY))
        assertThat(dmy.rows.single().date).isEqualTo(LocalDate.of(2026, 5, 4))
    }

    @Test
    fun unknownAccountIsPreviewProblemNotSilentFallback() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val file = File(context.cacheDir, "unknown-acct.csv")
        file.writeText(
            """
            Date,Amount,Account
            2026-11-18,10.00,Ghost
            """.trimIndent(),
        )
        val mapping = mapOf(
            CsvColumns.DATE to "Date",
            CsvColumns.AMOUNT to "Amount",
            CsvColumns.ACCOUNT to "Account",
        )
        val preview = csv.previewImport(file, mapping)
        assertThat(
            preview.rows.single().problems.any { it.kind == CsvProblemKind.UNKNOWN_ACCOUNT },
        ).isTrue()
        val result = csv.importTransactions(file, mapping)
        assertThat(result.imported).isEqualTo(0)
    }

    @Test
    fun quoteAwareReaderSpansNewlinesAndEscapes() {
        val parsed = CsvReader.parse(
            "date,note\n2026-01-01,\"line1\nline2 \"\"quoted\"\"\"\n",
        )
        assertThat(parsed.rows).hasSize(2)
        assertThat(parsed.rows[1][1]).isEqualTo("line1\nline2 \"quoted\"")
    }

    @Test
    fun skipsExactDuplicatesAndReportsCount() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(1_000L, eur),
                accountId = accountId,
                merchant = "Esselunga",
                occurredAt = LocalDateTime.of(2026, 11, 18, 12, 0),
            ),
        )
        val file = File(context.cacheDir, "dup.csv")
        file.writeText(
            """
            Date,Amount,Currency,Merchant,Account,Kind
            2026-11-18,10.00,EUR,Esselunga,Card,expense
            2026-11-18,20.00,EUR,New Shop,Card,expense
            """.trimIndent(),
        )
        val mapping = mapOf(
            CsvColumns.DATE to "Date",
            CsvColumns.AMOUNT to "Amount",
            CsvColumns.CURRENCY to "Currency",
            CsvColumns.MERCHANT to "Merchant",
            CsvColumns.ACCOUNT to "Account",
            CsvColumns.KIND to "Kind",
        )
        val result = csv.importTransactions(file, mapping)
        assertThat(result.imported).isEqualTo(1)
        assertThat(result.skippedDuplicates).isEqualTo(1)
        assertThat(transactions.observe().first()).hasSize(2)
    }

    @Test
    fun negativeIsIncomeOption() {
        val kind = CsvServiceImpl.resolveKind(
            kindRaw = null,
            money = Money(-500L, eur),
            convention = CsvSignConvention.NegativeIsIncome,
        )
        assertThat(kind).isEqualTo(TransactionKind.INCOME)
    }
}
