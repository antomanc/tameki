package dev.antomanc.tameki.core.data.csv

import androidx.room.withTransaction
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.money.MoneyParser
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.model.repository.CsvDateFormat
import dev.antomanc.tameki.core.model.repository.CsvImportOptions
import dev.antomanc.tameki.core.model.repository.CsvImportPreview
import dev.antomanc.tameki.core.model.repository.CsvImportResult
import dev.antomanc.tameki.core.model.repository.CsvParsedRow
import dev.antomanc.tameki.core.model.repository.CsvProblem
import dev.antomanc.tameki.core.model.repository.CsvProblemKind
import dev.antomanc.tameki.core.model.repository.CsvService
import dev.antomanc.tameki.core.model.repository.CsvSignConvention
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CsvServiceImpl(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val db: TamekiDatabase? = null,
    private val locale: Locale = Locale.getDefault(),
) : CsvService {
    override suspend fun exportTransactions(destination: File) = withContext(Dispatchers.IO) {
        val rows = transactions.observe().first()
        val accountNames = accounts.observeAll(includeArchived = true).first().associate { it.id to it.name }
        val categoryNames = categories.observeAll(includeArchived = true).first().associate { it.id to it.name }
        destination.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("date,amount,currency,merchant,category,note,account,kind,tags")
            for (tx in rows) {
                val date = tx.occurredAt.toLocalDate().toString()
                val amount = formatAmount(tx.amount)
                val merchant = escape(tx.merchant.orEmpty())
                val category = escape(tx.categoryId?.let { categoryNames[it] }.orEmpty())
                val note = escape(tx.note.orEmpty())
                val account = escape(accountNames[tx.accountId].orEmpty())
                val tags = escape(tx.tags.joinToString("|"))
                writer.appendLine(
                    listOf(
                        date,
                        amount,
                        tx.amount.currency.value,
                        merchant,
                        category,
                        note,
                        account,
                        tx.kind.name.lowercase(),
                        tags,
                    ).joinToString(","),
                )
            }
        }
    }

    override suspend fun previewImport(
        source: File,
        columnMapping: Map<String, String>,
        options: CsvImportOptions,
    ): CsvImportPreview = withContext(Dispatchers.IO) {
        val baseCurrency = settings.observeBaseCurrency().first()
        val accountNames = accounts.observeAll(includeArchived = true).first().map { it.name }.toSet()
        parseFile(source, columnMapping, baseCurrency, options, accountNames)
    }

    override suspend fun importTransactions(
        source: File,
        columnMapping: Map<String, String>,
        options: CsvImportOptions,
    ): CsvImportResult = withContext(Dispatchers.IO) {
        val preview = previewImport(source, columnMapping, options)
        val accountList = accounts.observeAll(includeArchived = true).first()
        val categoryList = categories.observeAll(includeArchived = true).first()
        val defaultAccount = accountList.firstOrNull()
            ?: error("No accounts available for CSV import")
        val database = db
        if (database != null) {
            database.withTransaction {
                doImport(preview, accountList, categoryList, defaultAccount.id, options)
            }
        } else {
            doImport(preview, accountList, categoryList, defaultAccount.id, options)
        }
    }

    private suspend fun doImport(
        preview: CsvImportPreview,
        accountList: List<dev.antomanc.tameki.core.model.entity.Account>,
        categoryList: List<dev.antomanc.tameki.core.model.entity.Category>,
        defaultAccountId: Long,
        options: CsvImportOptions,
    ): CsvImportResult {
        var imported = 0
        var skipped = 0
        for (row in preview.rows) {
            if (row.problems.isNotEmpty()) continue
            val amount = row.amount ?: continue
            val date = row.date ?: continue
            val accountName = row.account
            val accountId = if (accountName != null) {
                accountList.firstOrNull { it.name.equals(accountName, ignoreCase = true) }?.id
                    ?: continue // unknown account already reported in preview
            } else {
                defaultAccountId
            }
            val categoryId = row.category?.let { name ->
                categoryList.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
            }
            val currency = row.currency ?: amount.currency
            val money = if (amount.currency == currency) amount else Money(amount.minor, currency)
            val kind = resolveKind(row.kind, money, options.signConvention) ?: continue
            val absolute = if (money.minor < 0) Money(-money.minor, money.currency) else money
            val occurredAt = LocalDateTime.of(date, LocalTime.NOON)
            val merchant = row.merchant
            val existing = if (db != null) {
                db.transactionDao().countExactDuplicate(
                    occurredAt = occurredAt,
                    amountMinor = absolute.minor,
                    currency = absolute.currency.value,
                    merchant = merchant,
                    accountId = accountId,
                )
            } else {
                0
            }
            if (existing > 0) {
                skipped++
                continue
            }
            val tags = row.tags
                ?.split('|', ',', ';')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                .orEmpty()
            transactions.upsert(
                Transaction(
                    kind = kind,
                    amount = absolute,
                    accountId = accountId,
                    categoryId = categoryId,
                    merchant = merchant,
                    note = row.note,
                    tags = tags,
                    occurredAt = occurredAt,
                    source = TransactionSource.CSV_IMPORT,
                ),
            )
            imported++
        }
        return CsvImportResult(imported = imported, skippedDuplicates = skipped)
    }

    internal fun parseFile(
        source: File,
        columnMapping: Map<String, String>,
        baseCurrency: CurrencyCode,
        options: CsvImportOptions = CsvImportOptions(),
        knownAccounts: Set<String> = emptySet(),
    ): CsvImportPreview {
        val text = source.readText(Charsets.UTF_8)
        val parsed = CsvReader.parse(text)
        if (parsed.rows.isEmpty()) {
            return CsvImportPreview(
                emptyList(),
                listOf(CsvProblem(0, CsvProblemKind.EMPTY_FILE)),
            )
        }
        val headers = parsed.rows.first().map { it.trim() }
        val indexOf = columnMapping.mapValues { (_, header) ->
            headers.indexOfFirst { it.equals(header, ignoreCase = true) }
        }
        val problems = mutableListOf<CsvProblem>()
        val missing = listOf(CsvColumns.DATE, CsvColumns.AMOUNT).filter { key ->
            val idx = indexOf[key] ?: -1
            idx < 0
        }
        for (key in missing) {
            problems += CsvProblem(1, CsvProblemKind.MISSING_MAPPING, key)
        }

        fun cellAt(cells: List<String>, key: String): String? {
            val idx = indexOf[key] ?: return null
            if (idx < 0 || idx >= cells.size) return null
            return cells[idx].trim().trim('"').takeIf { it.isNotEmpty() }
        }

        val dateRaws = parsed.rows.drop(1).mapNotNull { cells -> cellAt(cells, CsvColumns.DATE) }
        val resolvedFormat = options.dateFormat
            ?: detectDateFormat(dateRaws, locale)

        val rows = mutableListOf<CsvParsedRow>()
        for ((offset, cells) in parsed.rows.drop(1).withIndex()) {
            val lineNumber = offset + 2
            fun cell(key: String): String? = cellAt(cells, key)
            val rowProblems = mutableListOf<CsvProblem>()
            val dateRaw = cell(CsvColumns.DATE)
            val date = dateRaw?.let { parseDate(it, resolvedFormat) }
            if (dateRaw != null && date == null) {
                rowProblems += CsvProblem(lineNumber, CsvProblemKind.UNPARSEABLE_DATE, dateRaw)
            }
            if (dateRaw == null) rowProblems += CsvProblem(lineNumber, CsvProblemKind.MISSING_DATE)

            val currencyRaw = cell(CsvColumns.CURRENCY)
            val currency = currencyRaw?.let { CurrencyCode.parse(it) } ?: baseCurrency
            if (currencyRaw != null && CurrencyCode.parse(currencyRaw) == null) {
                rowProblems += CsvProblem(lineNumber, CsvProblemKind.UNPARSEABLE_CURRENCY, currencyRaw)
            }

            val amountRaw = cell(CsvColumns.AMOUNT)
            val amount = amountRaw?.let { MoneyParser.parse(it, currency) }
            if (amountRaw != null && amount == null) {
                rowProblems += CsvProblem(lineNumber, CsvProblemKind.UNPARSEABLE_AMOUNT, amountRaw)
            }
            if (amountRaw == null) rowProblems += CsvProblem(lineNumber, CsvProblemKind.MISSING_AMOUNT)

            val account = cell(CsvColumns.ACCOUNT)
            if (account != null && knownAccounts.isNotEmpty() &&
                knownAccounts.none { it.equals(account, ignoreCase = true) }
            ) {
                rowProblems += CsvProblem(lineNumber, CsvProblemKind.UNKNOWN_ACCOUNT, account)
            }

            val kindRaw = cell(CsvColumns.KIND)
            if (kindRaw != null && parseKindToken(kindRaw) == null) {
                rowProblems += CsvProblem(lineNumber, CsvProblemKind.UNKNOWN_KIND, kindRaw)
            }

            val row = CsvParsedRow(
                lineNumber = lineNumber,
                date = date,
                amount = amount,
                currency = currency,
                merchant = cell(CsvColumns.MERCHANT),
                category = cell(CsvColumns.CATEGORY),
                note = cell(CsvColumns.NOTE),
                account = account,
                kind = kindRaw,
                tags = cell(CsvColumns.TAGS),
                problems = rowProblems,
            )
            rows += row
            problems += rowProblems
        }
        return CsvImportPreview(rows, problems, resolvedFormat)
    }

    companion object {
        private val iso = DateTimeFormatter.ISO_LOCAL_DATE
        private val dmy = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        )
        private val mdy = listOf(
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
        )

        fun detectDateFormat(dateRaws: List<String>, locale: Locale): CsvDateFormat {
            val hasIso = dateRaws.any { it.trim().matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            // Only inspect ambiguous numeric dates (not ISO yyyy-MM-dd) for day>12 clues.
            val numeric = dateRaws.mapNotNull { raw ->
                val trimmed = raw.trim()
                if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return@mapNotNull null
                val parts = trimmed.split('/', '-', '.')
                if (parts.size == 3 && parts.all { it.all(Char::isDigit) }) parts else null
            }
            for (parts in numeric) {
                val first = parts[0].toIntOrNull() ?: continue
                val second = parts[1].toIntOrNull() ?: continue
                if (first > 12) return CsvDateFormat.DMY
                if (second > 12) return CsvDateFormat.MDY
            }
            if (hasIso && numeric.isEmpty()) return CsvDateFormat.ISO
            if (numeric.isNotEmpty()) return defaultFormatForLocale(locale)
            return if (hasIso) CsvDateFormat.ISO else defaultFormatForLocale(locale)
        }

        fun defaultFormatForLocale(locale: Locale): CsvDateFormat {
            val country = locale.country.uppercase(Locale.ROOT)
            return if (country in setOf("US", "PH", "CA")) CsvDateFormat.MDY else CsvDateFormat.DMY
        }

        fun parseDate(raw: String, format: CsvDateFormat): LocalDate? {
            val trimmed = raw.trim()
            if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                return try {
                    LocalDate.parse(trimmed, iso)
                } catch (_: DateTimeParseException) {
                    null
                }
            }
            val formats = when (format) {
                CsvDateFormat.ISO -> listOf(iso)
                CsvDateFormat.DMY -> dmy
                CsvDateFormat.MDY -> mdy
            }
            for (fmt in formats) {
                try {
                    return LocalDate.parse(trimmed, fmt)
                } catch (_: DateTimeParseException) {
                    // try next
                }
            }
            return null
        }

        fun parseKindToken(raw: String): TransactionKind? {
            return when (raw.trim().lowercase(Locale.ROOT)) {
                "expense", "uscita", "debit", "addebito" -> TransactionKind.EXPENSE
                "income", "entrata", "credit", "accredito" -> TransactionKind.INCOME
                "transfer", "giroconto" -> TransactionKind.TRANSFER
                else -> null
            }
        }

        fun resolveKind(
            kindRaw: String?,
            money: Money,
            convention: CsvSignConvention,
        ): TransactionKind? {
            if (kindRaw != null) return parseKindToken(kindRaw)
            val negative = money.minor < 0
            return when (convention) {
                CsvSignConvention.NegativeIsExpense ->
                    if (negative) TransactionKind.EXPENSE else TransactionKind.INCOME
                CsvSignConvention.NegativeIsIncome ->
                    if (negative) TransactionKind.INCOME else TransactionKind.EXPENSE
            }
        }

        private fun formatAmount(money: Money): String {
            val digits = dev.antomanc.tameki.core.model.money.Currencies.fractionDigits(money.currency)
            if (digits == 0) return money.minor.toString()
            val negative = money.minor < 0
            val abs = kotlin.math.abs(money.minor)
            val whole = abs / pow10(digits)
            val frac = (abs % pow10(digits)).toString().padStart(digits, '0')
            val body = "$whole.$frac"
            return if (negative) "-$body" else body
        }

        private fun pow10(n: Int): Long {
            var v = 1L
            repeat(n) { v *= 10 }
            return v
        }

        private fun escape(value: String): String {
            if (value.none { it == ',' || it == '"' || it == '\n' || it == ';' }) return value
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
    }
}
