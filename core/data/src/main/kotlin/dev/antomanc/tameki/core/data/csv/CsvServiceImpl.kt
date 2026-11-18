package dev.antomanc.tameki.core.data.csv

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.money.MoneyParser
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.model.repository.CsvImportPreview
import dev.antomanc.tameki.core.model.repository.CsvParsedRow
import dev.antomanc.tameki.core.model.repository.CsvProblem
import dev.antomanc.tameki.core.model.repository.CsvService
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CsvServiceImpl(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
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
    ): CsvImportPreview = withContext(Dispatchers.IO) {
        val baseCurrency = settings.observeBaseCurrency().first()
        parseFile(source, columnMapping, baseCurrency)
    }

    override suspend fun importTransactions(
        source: File,
        columnMapping: Map<String, String>,
    ): Int = withContext(Dispatchers.IO) {
        val preview = previewImport(source, columnMapping)
        val accountList = accounts.observeAll(includeArchived = true).first()
        val categoryList = categories.observeAll(includeArchived = true).first()
        val defaultAccount = accountList.firstOrNull()
            ?: error("No accounts available for CSV import")
        var imported = 0
        for (row in preview.rows) {
            if (row.problems.isNotEmpty()) continue
            val amount = row.amount ?: continue
            val date = row.date ?: continue
            val accountId = row.account?.let { name ->
                accountList.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
            } ?: defaultAccount.id
            val categoryId = row.category?.let { name ->
                categoryList.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
            }
            val currency = row.currency ?: amount.currency
            val money = if (amount.currency == currency) amount else Money(amount.minor, currency)
            val kind = if (money.minor < 0) TransactionKind.INCOME else TransactionKind.EXPENSE
            val absolute = if (money.minor < 0) Money(-money.minor, money.currency) else money
            transactions.upsert(
                Transaction(
                    kind = kind,
                    amount = absolute,
                    accountId = accountId,
                    categoryId = categoryId,
                    merchant = row.merchant,
                    note = row.note,
                    occurredAt = LocalDateTime.of(date, LocalTime.NOON),
                    source = TransactionSource.CSV_IMPORT,
                ),
            )
            imported++
        }
        imported
    }

    internal fun parseFile(
        source: File,
        columnMapping: Map<String, String>,
        baseCurrency: CurrencyCode,
    ): CsvImportPreview {
        val text = source.readText(Charsets.UTF_8)
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return CsvImportPreview(emptyList(), listOf(CsvProblem(0, "Empty file")))
        }
        val separator = detectSeparator(lines.first())
        val headers = splitCsvLine(lines.first(), separator).map { it.trim().removePrefix("\uFEFF") }
        val indexOf = columnMapping.mapValues { (_, header) ->
            headers.indexOfFirst { it.equals(header, ignoreCase = true) }
        }
        val problems = mutableListOf<CsvProblem>()
        val missing = listOf(CsvColumns.DATE, CsvColumns.AMOUNT).filter { key ->
            val idx = indexOf[key] ?: -1
            idx < 0
        }
        for (key in missing) {
            problems += CsvProblem(1, "Missing required column mapping: $key")
        }
        val rows = mutableListOf<CsvParsedRow>()
        for ((offset, line) in lines.drop(1).withIndex()) {
            val lineNumber = offset + 2
            val cells = splitCsvLine(line, separator)
            fun cell(key: String): String? {
                val idx = indexOf[key] ?: return null
                if (idx < 0 || idx >= cells.size) return null
                return cells[idx].trim().trim('"').takeIf { it.isNotEmpty() }
            }
            val rowProblems = mutableListOf<String>()
            val dateRaw = cell(CsvColumns.DATE)
            val date = dateRaw?.let { parseDate(it) }
            if (dateRaw != null && date == null) rowProblems += "Unparseable date: $dateRaw"
            if (dateRaw == null) rowProblems += "Missing date"

            val currencyRaw = cell(CsvColumns.CURRENCY)
            val currency = currencyRaw?.let { CurrencyCode.parse(it) } ?: baseCurrency
            if (currencyRaw != null && CurrencyCode.parse(currencyRaw) == null) {
                rowProblems += "Bad currency: $currencyRaw"
            }

            val amountRaw = cell(CsvColumns.AMOUNT)
            val amount = amountRaw?.let { MoneyParser.parse(it, currency) }
            if (amountRaw != null && amount == null) rowProblems += "Unparseable amount: $amountRaw"
            if (amountRaw == null) rowProblems += "Missing amount"

            val row = CsvParsedRow(
                lineNumber = lineNumber,
                date = date,
                amount = amount,
                currency = currency,
                merchant = cell(CsvColumns.MERCHANT),
                category = cell(CsvColumns.CATEGORY),
                note = cell(CsvColumns.NOTE),
                account = cell(CsvColumns.ACCOUNT),
                problems = rowProblems,
            )
            rows += row
            for (msg in rowProblems) {
                problems += CsvProblem(lineNumber, msg)
            }
        }
        return CsvImportPreview(rows, problems)
    }

    companion object {
        private val dateFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        )

        fun detectSeparator(headerLine: String): Char {
            val semis = headerLine.count { it == ';' }
            val commas = headerLine.count { it == ',' }
            return if (semis > commas) ';' else ','
        }

        fun splitCsvLine(line: String, separator: Char): List<String> {
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < line.length) {
                val ch = line[i]
                when {
                    ch == '"' -> {
                        if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                            current.append('"')
                            i++
                        } else {
                            inQuotes = !inQuotes
                        }
                    }
                    ch == separator && !inQuotes -> {
                        result += current.toString()
                        current.clear()
                    }
                    else -> current.append(ch)
                }
                i++
            }
            result += current.toString()
            return result
        }

        fun parseDate(raw: String): LocalDate? {
            val trimmed = raw.trim()
            for (fmt in dateFormats) {
                try {
                    return LocalDate.parse(trimmed, fmt)
                } catch (_: DateTimeParseException) {
                    // try next
                }
            }
            return null
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
