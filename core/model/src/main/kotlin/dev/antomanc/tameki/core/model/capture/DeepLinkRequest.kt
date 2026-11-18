package dev.antomanc.tameki.core.model.capture

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.money.MoneyParser
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

data class DeepLinkParseResult(
    val draft: TransactionDraft,
    val save: Boolean,
    val tags: Set<String> = emptySet(),
)

/**
 * Parses `tameki://add?...` automation deep links into a draft.
 * Rejects malformed input with a reason so Tasker/shortcuts can surface a clear failure.
 */
object DeepLinkRequest {
    private const val SCHEME = "tameki"
    private const val HOST = "add"

    fun parse(uriString: String, defaultCurrency: CurrencyCode = CurrencyCode("EUR")): Result<DeepLinkParseResult> {
        val uri = try {
            URI(uriString)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Malformed URI: ${e.message}"))
        }
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Expected scheme $SCHEME"))
        }
        val host = uri.host ?: uri.schemeSpecificPart?.substringBefore('?')?.trimStart('/')
        if (!host.equals(HOST, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Expected host $HOST"))
        }
        val query = uri.rawQuery ?: ""
        val params = parseQuery(query)

        val currency = params["currency"]?.let { CurrencyCode.parse(it) } ?: defaultCurrency
        val amountRaw = params["amount"]
        val amount = if (amountRaw != null) {
            MoneyParser.parse(amountRaw, currency)
                ?: return Result.failure(IllegalArgumentException("Invalid amount"))
        } else {
            null
        }
        val kind = when (params["kind"]?.lowercase()) {
            null, "expense" -> TransactionKind.EXPENSE
            "income" -> TransactionKind.INCOME
            "transfer" -> TransactionKind.TRANSFER
            else -> return Result.failure(IllegalArgumentException("Invalid kind"))
        }
        val date = params["date"]?.let {
            try {
                LocalDate.parse(it)
            } catch (_: Exception) {
                return Result.failure(IllegalArgumentException("Invalid date"))
            }
        }
        val occurredAt = date?.atTime(LocalTime.NOON)
        val save = when (params["save"]?.lowercase()) {
            null, "0", "false", "no" -> false
            "1", "true", "yes" -> true
            else -> return Result.failure(IllegalArgumentException("Invalid save flag"))
        }
        val tags = params["tags"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()

        val draft = TransactionDraft(
            kind = kind,
            amount = amount,
            merchant = params["merchant"],
            categoryHint = params["category"],
            occurredAt = occurredAt,
            note = params["note"],
            accountHint = params["account"],
        )
        return Result.success(DeepLinkParseResult(draft = draft, save = save, tags = tags))
    }

    private fun parseQuery(raw: String): Map<String, String> {
        if (raw.isEmpty()) return emptyMap()
        return raw.split('&')
            .mapNotNull { part ->
                if (part.isEmpty()) return@mapNotNull null
                val eq = part.indexOf('=')
                if (eq < 0) {
                    decode(part) to ""
                } else {
                    decode(part.substring(0, eq)) to decode(part.substring(eq + 1))
                }
            }
            .toMap()
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8)
}
