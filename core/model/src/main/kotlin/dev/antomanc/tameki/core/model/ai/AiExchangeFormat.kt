package dev.antomanc.tameki.core.model.ai

import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.ReceiptItem
import dev.antomanc.tameki.core.model.entity.TransactionDraft
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.money.MoneyParser
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses the documented tameki AI exchange JSON (v1).
 * Strips markdown fences and surrounding prose; amounts may be numbers or locale strings.
 */
object AiExchangeFormat {
    const val VERSION: Int = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowComments = true
    }

    fun parse(
        text: String,
        baseCurrency: CurrencyCode = CurrencyCode("EUR"),
        clock: Clock = Clock.systemDefaultZone(),
    ): AiImportResult {
        val problems = mutableListOf<Problem>()
        val extracted = extractJsonObject(text)
        if (extracted == null) {
            return AiImportResult(problems = listOf(Problem("$", "No JSON object found", ProblemCode.NO_JSON)))
        }
        val root = try {
            json.parseToJsonElement(extracted).jsonObject
        } catch (e: Exception) {
            return AiImportResult(problems = listOf(Problem("$", "Invalid JSON: ${e.message}", ProblemCode.INVALID_JSON, e.message)))
        }

        val versionEl = root["tameki"]
        if (versionEl == null) {
            problems += Problem("tameki", "Missing version field", ProblemCode.MISSING_VERSION)
        } else {
            val v = versionEl.asIntOrNull()
            if (v == null) {
                problems += Problem("tameki", "Version must be a number", ProblemCode.VERSION_NOT_NUMBER)
            } else if (v != VERSION) {
                problems += Problem("tameki", "Unsupported version $v (expected $VERSION)", ProblemCode.UNSUPPORTED_VERSION, v.toString())
            }
        }

        val today = LocalDate.now(clock)
        val transactions = mutableListOf<TransactionDraft>()
        val txArray = root["transactions"]?.asArrayOrNull()
        if (txArray != null) {
            txArray.forEachIndexed { index, el ->
                val obj = el.asObjectOrNull()
                if (obj == null) {
                    problems += Problem("transactions[$index]", "Expected object", ProblemCode.EXPECTED_OBJECT)
                } else {
                    val parsed = parseTransaction(obj, "transactions[$index]", baseCurrency, today, problems)
                    if (parsed != null) transactions += parsed
                }
            }
        }

        val subscriptions = mutableListOf<SubscriptionDraft>()
        val subArray = root["subscriptions"]?.asArrayOrNull()
        if (subArray != null) {
            subArray.forEachIndexed { index, el ->
                val obj = el.asObjectOrNull()
                if (obj == null) {
                    problems += Problem("subscriptions[$index]", "Expected object", ProblemCode.EXPECTED_OBJECT)
                } else {
                    val parsed = parseSubscription(obj, "subscriptions[$index]", baseCurrency, problems)
                    if (parsed != null) subscriptions += parsed
                }
            }
        }

        return AiImportResult(
            transactions = transactions,
            subscriptions = subscriptions,
            problems = problems,
        )
    }

    private fun parseTransaction(
        obj: JsonObject,
        path: String,
        baseCurrency: CurrencyCode,
        today: LocalDate,
        problems: MutableList<Problem>,
    ): TransactionDraft? {
        val kind = when (obj.string("kind")?.lowercase()) {
            null, "expense" -> TransactionKind.EXPENSE
            "income" -> TransactionKind.INCOME
            "transfer" -> TransactionKind.TRANSFER
            else -> {
                problems += Problem("$path.kind", "Unknown kind", ProblemCode.UNKNOWN_KIND)
                TransactionKind.EXPENSE
            }
        }
        val currency = obj.string("currency")?.let { CurrencyCode.parse(it) } ?: baseCurrency
        val amountRaw = obj["amount"]
        val amount = when {
            amountRaw == null || amountRaw is JsonNull -> {
                problems += Problem("$path.amount", "Missing amount", ProblemCode.MISSING_AMOUNT)
                null
            }
            else -> parseAmount(amountRaw, currency, "$path.amount", problems)
        }
        val date = obj.string("date")?.let { parseDate(it, "$path.date", problems) } ?: today
        val time = obj.string("time")?.let { parseTime(it, "$path.time", problems) } ?: LocalTime.NOON
        val items = mutableListOf<ReceiptItem>()
        obj["items"]?.asArrayOrNull()?.forEachIndexed { i, el ->
            val itemObj = el.asObjectOrNull() ?: run {
                problems += Problem("$path.items[$i]", "Expected object", ProblemCode.EXPECTED_OBJECT)
                return@forEachIndexed
            }
            val name = itemObj.string("name")
            if (name.isNullOrBlank()) {
                problems += Problem("$path.items[$i].name", "Missing name", ProblemCode.MISSING_NAME)
                return@forEachIndexed
            }
            val itemCurrency = itemObj.string("currency")?.let { CurrencyCode.parse(it) } ?: currency
            val itemAmount = itemObj["amount"]?.let { parseAmount(it, itemCurrency, "$path.items[$i].amount", problems) }
            if (itemAmount == null) return@forEachIndexed
            val quantity: Double? = itemObj["quantity"]?.jsonPrimitive?.doubleOrNull // money-ok: count/weight
            items += ReceiptItem(name = name, amount = itemAmount, quantity = quantity)
        }
        return TransactionDraft(
            kind = kind,
            amount = amount,
            merchant = obj.string("merchant"),
            categoryHint = obj.string("category"),
            occurredAt = LocalDateTime.of(date, time),
            note = obj.string("note"),
            items = items,
            accountHint = obj.string("account"),
        )
    }

    private fun parseSubscription(
        obj: JsonObject,
        path: String,
        baseCurrency: CurrencyCode,
        problems: MutableList<Problem>,
    ): SubscriptionDraft? {
        val name = obj.string("name")
        if (name.isNullOrBlank()) {
            problems += Problem("$path.name", "Missing name", ProblemCode.MISSING_NAME)
            return null
        }
        val currency = obj.string("currency")?.let { CurrencyCode.parse(it) } ?: baseCurrency
        val amountEl = obj["amount"]
        if (amountEl == null) {
            problems += Problem("$path.amount", "Missing amount", ProblemCode.MISSING_AMOUNT)
            return null
        }
        val amount = parseAmount(amountEl, currency, "$path.amount", problems) ?: return null
        val cycle = parseCycle(obj.string("cycle"), "$path.cycle", problems) ?: Cycle(1, CycleUnit.MONTH)
        val nextDate = obj.string("next_date")?.let { parseDate(it, "$path.next_date", problems) }
        val trial = obj.string("trial_ends_on")?.let { parseDate(it, "$path.trial_ends_on", problems) }
        return SubscriptionDraft(
            name = name,
            amount = amount,
            cycle = cycle,
            nextDate = nextDate,
            trialEndsOn = trial,
            categoryHint = obj.string("category"),
            cancelUrl = obj.string("cancel_url"),
        )
    }

    private fun parseAmount(
        el: JsonElement,
        currency: CurrencyCode,
        path: String,
        problems: MutableList<Problem>,
    ): Money? {
        val raw = when (el) {
            is JsonPrimitive -> el.contentOrNull ?: el.toString()
            else -> {
                problems += Problem(path, "Amount must be string or number", ProblemCode.AMOUNT_TYPE)
                return null
            }
        }
        val parsed = MoneyParser.parse(raw, currency)
        if (parsed == null) {
            problems += Problem(path, "Cannot parse amount '$raw'", ProblemCode.CANNOT_PARSE_AMOUNT, raw)
        }
        return parsed?.let { if (it.minor < 0) it.negate() else it }
    }

    private fun parseCycle(raw: String?, path: String, problems: MutableList<Problem>): Cycle? {
        if (raw.isNullOrBlank()) {
            problems += Problem(path, "Missing cycle; defaulting to 1 month", ProblemCode.MISSING_CYCLE_DEFAULT)
            return null
        }
        val m = Regex("""(?i)^\s*(\d+)\s*(day|days|week|weeks|month|months|year|years)\s*$""").matchEntire(raw.trim())
        if (m == null) {
            problems += Problem(path, "Unrecognized cycle '$raw'", ProblemCode.UNRECOGNIZED_CYCLE, raw)
            return null
        }
        val every = m.groupValues[1].toInt()
        val unit = when (m.groupValues[2].lowercase().removeSuffix("s")) {
            "day" -> CycleUnit.DAY
            "week" -> CycleUnit.WEEK
            "month" -> CycleUnit.MONTH
            "year" -> CycleUnit.YEAR
            else -> CycleUnit.MONTH
        }
        return Cycle(every, unit)
    }

    private fun parseDate(raw: String, path: String, problems: MutableList<Problem>): LocalDate? =
        try {
            LocalDate.parse(raw.trim())
        } catch (_: Exception) {
            problems += Problem(path, "Invalid date '$raw'", ProblemCode.INVALID_DATE, raw)
            null
        }

    private fun parseTime(raw: String, path: String, problems: MutableList<Problem>): LocalTime? =
        try {
            LocalTime.parse(raw.trim())
        } catch (_: Exception) {
            problems += Problem(path, "Invalid time '$raw'", ProblemCode.INVALID_TIME, raw)
            null
        }

    internal fun extractJsonObject(text: String): String? {
        var s = text.trim()
        val fence = Regex("""```(?:json)?\s*([\s\S]*?)```""", RegexOption.IGNORE_CASE)
        val fenced = fence.find(s)
        if (fenced != null) {
            s = fenced.groupValues[1].trim()
        }
        val start = s.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until s.length) {
            val ch = s[i]
            if (inString) {
                when {
                    escape -> escape = false
                    ch == '\\' -> escape = true
                    ch == '"' -> inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return s.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray
    private fun JsonElement.asIntOrNull(): Int? =
        (this as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
            ?: (this as? JsonPrimitive)?.takeIf { it.isString.not() }?.content?.toIntOrNull()

    private fun JsonObject.string(key: String): String? {
        val el = this[key] ?: return null
        if (el is JsonNull) return null
        return (el as? JsonPrimitive)?.contentOrNull
    }
}
