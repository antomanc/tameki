package dev.antomanc.tameki.automation

import android.content.Intent
import android.net.Uri
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.capture.DeepLinkParseResult
import dev.antomanc.tameki.core.model.capture.DeepLinkRequest
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.money.MoneyParser
import java.time.LocalDate
import java.time.LocalTime

object AutomationIntents {
    const val ACTION_ADD_TRANSACTION: String = "dev.antomanc.tameki.action.ADD_TRANSACTION"

    const val EXTRA_AMOUNT: String = "amount"
    const val EXTRA_CURRENCY: String = "currency"
    const val EXTRA_KIND: String = "kind"
    const val EXTRA_MERCHANT: String = "merchant"
    const val EXTRA_CATEGORY: String = "category"
    const val EXTRA_ACCOUNT: String = "account"
    const val EXTRA_DATE: String = "date"
    const val EXTRA_NOTE: String = "note"
    const val EXTRA_TAGS: String = "tags"
    const val EXTRA_SAVE: String = "save"

    const val EXTRA_OPEN: String = "tameki.open"
    const val OPEN_QUICK_ADD: String = "quick_add"
    const val OPEN_PASTE_AI: String = "paste_ai"
    const val OPEN_SCAN: String = "scan"
    const val OPEN_IMPORT: String = "import"

    fun parseAddIntent(intent: Intent, defaultCurrency: CurrencyCode): Result<DeepLinkParseResult> {
        val data = intent.data
        if (data != null && data.scheme.equals("tameki", ignoreCase = true)) {
            when (data.host?.lowercase()) {
                "add" -> return DeepLinkRequest.parse(data.toString(), defaultCurrency)
                "import" -> return Result.failure(ImportDeepLink)
            }
        }
        if (intent.action == ACTION_ADD_TRANSACTION || intent.hasExtra(EXTRA_AMOUNT)) {
            return parseExtras(intent, defaultCurrency)
        }
        return Result.failure(IllegalArgumentException("Not an add intent"))
    }

    fun isImportDeepLink(uri: Uri?): Boolean =
        uri != null &&
            uri.scheme.equals("tameki", ignoreCase = true) &&
            uri.host.equals("import", ignoreCase = true)

    private fun parseExtras(intent: Intent, defaultCurrency: CurrencyCode): Result<DeepLinkParseResult> {
        val currency = intent.getStringExtra(EXTRA_CURRENCY)?.let { CurrencyCode.parse(it) } ?: defaultCurrency
        val amountRaw = intent.getStringExtra(EXTRA_AMOUNT)
        val amount = if (amountRaw != null) {
            MoneyParser.parse(amountRaw, currency)
                ?: return Result.failure(IllegalArgumentException("Invalid amount"))
        } else {
            null
        }
        val kind = when (intent.getStringExtra(EXTRA_KIND)?.lowercase()) {
            null, "expense" -> TransactionKind.EXPENSE
            "income" -> TransactionKind.INCOME
            "transfer" -> TransactionKind.TRANSFER
            else -> return Result.failure(IllegalArgumentException("Invalid kind"))
        }
        val date = intent.getStringExtra(EXTRA_DATE)?.let {
            runCatchingCancellable { LocalDate.parse(it) }.getOrElse {
                return Result.failure(IllegalArgumentException("Invalid date"))
            }
        }
        val save = when (intent.getStringExtra(EXTRA_SAVE)?.lowercase()) {
            null, "0", "false", "no" -> intent.getBooleanExtra(EXTRA_SAVE, false)
            "1", "true", "yes" -> true
            else -> return Result.failure(IllegalArgumentException("Invalid save flag"))
        }
        val tags = intent.getStringExtra(EXTRA_TAGS)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()
        val draft = TransactionDraft(
            kind = kind,
            amount = amount,
            merchant = intent.getStringExtra(EXTRA_MERCHANT),
            categoryHint = intent.getStringExtra(EXTRA_CATEGORY),
            occurredAt = date?.atTime(LocalTime.NOON),
            note = intent.getStringExtra(EXTRA_NOTE),
            accountHint = intent.getStringExtra(EXTRA_ACCOUNT),
        )
        return Result.success(DeepLinkParseResult(draft = draft, save = save, tags = tags))
    }

    object ImportDeepLink : Exception("import")
}
