# Domain contracts (core:model)

Package `dev.antomanc.tameki.core.model`. Pure Kotlin, `java.time`, `@Serializable` where it crosses a boundary. Names below are the contract; internals are free. Every function marked (pure) gets JVM unit tests.

## Money
```kotlin
@JvmInline value class CurrencyCode(val value: String)          // ISO 4217, upper case
data class Money(val minor: Long, val currency: CurrencyCode)  // minor units per currency fraction digits (EUR 2, JPY 0)
object Currencies { fun fractionDigits(c: CurrencyCode): Int }  // from java.util.Currency, default 2
object MoneyParser { fun parse(text: String, currency: CurrencyCode): Money? } // (pure) "12,50" "12.50" "1.234,56" "1,234.56" "€ 12" "-3"
operator fun Money.plus/minus, times(Int), Money.negate(), isZero, Money.zero(currency)
interface ExchangeRates { suspend fun convert(m: Money, to: CurrencyCode, on: LocalDate): Money? }
```
Arithmetic across different currencies throws `IllegalArgumentException`. Aggregations convert to the base currency first via `ExchangeRates` (implementation: manual rates + optional ECB rates from frankfurter.dev, cached).

## Entities (ids are `Long`, 0 = not yet stored)
```kotlin
enum class AccountType { CASH, BANK, CARD, SAVINGS, OTHER }
data class Account(id, name, currency: CurrencyCode, type, archived: Boolean, sortOrder: Int)

enum class CategoryKind { EXPENSE, INCOME }
data class Category(id, name, kind, icon: String /* key into core:ui icon set */, color: Long /* ARGB */, parentId: Long?, archived)

enum class TransactionKind { EXPENSE, INCOME, TRANSFER }
enum class TransactionSource { MANUAL, RECEIPT, NOTIFICATION, AI_IMPORT, AUTOMATION, RECURRING, CSV_IMPORT }
data class Split(categoryId: Long, amount: Money, note: String?)
data class Transaction(
  id, kind, amount: Money /* always positive */, accountId: Long, toAccountId: Long? /* TRANSFER only */,
  categoryId: Long?, splits: List<Split> /* empty or sums to amount */, merchant: String?, note: String?,
  tags: Set<String>, occurredAt: LocalDateTime, receiptId: Long?, recurringId: Long?, source: TransactionSource,
)
data class ReceiptItem(name: String, amount: Money, quantity: Double?)
data class Receipt(id, imagePath: String, rawText: String?, items: List<ReceiptItem>, createdAt: Instant)

data class Cycle(val every: Int, val unit: CycleUnit)    // every 1 MONTH, every 3 MONTH, every 1 YEAR, every 2 WEEK
enum class CycleUnit { DAY, WEEK, MONTH, YEAR }
enum class RecurringType { SUBSCRIPTION, BILL, INCOME }
enum class RecurringStatus { ACTIVE, TRIAL, PAUSED, CANCELLED }
enum class Worth { LOVE, OK, RARELY, UNUSED }            // user's "is it worth it?" rating, nullable = not rated
data class PricePoint(val amount: Money, val since: LocalDate)
data class Recurring(
  id, type, name, amount: Money, cycle: Cycle, startDate: LocalDate, nextDate: LocalDate,
  trialEndsOn: LocalDate?, status, accountId: Long?, categoryId: Long?, cancelUrl: String?, note: String?,
  worth: Worth?, remindDaysBefore: Int /* default 2, -1 = off */, autoLog: Boolean,
  priceHistory: List<PricePoint>, annualAlternative: Money? /* price of the yearly plan, if known */,
  brandKey: String? /* key into bundled brand catalog for logo/colour/cancel link */, sharedWith: Int /* people splitting, 1 = alone */,
)

enum class BudgetPeriod { WEEK, MONTH }
data class Budget(id, name, categoryIds: Set<Long> /* empty = all expenses */, limit: Money, period, rollover: Boolean)
data class Goal(id, name, target: Money, saved: Money, deadline: LocalDate?, icon: String, archived)

enum class InboxStatus { PENDING, ACCEPTED, DISMISSED }
data class InboxItem(id, draft: TransactionDraft, origin: TransactionSource, rawText: String, receivedAt: Instant, status)
data class TransactionDraft(kind, amount: Money?, merchant: String?, categoryHint: String?, occurredAt: LocalDateTime?, note: String?, items: List<ReceiptItem>, accountHint: String?)
```

## Pure logic (each its own file + test)
- `RecurrenceCalculator` (pure): `next(date, cycle)`, `occurrencesBetween(r, from, to): List<LocalDate>`; month-end clamping (Jan 31 + 1 month = Feb 28/29, then stays anchored to 31 when possible via `startDate`).
- `CostNormalizer` (pure): `monthly(r): Money`, `yearly(r): Money` (divided by `sharedWith`, rounded half-even); totals per type/status.
- `BudgetPace` (pure): `evaluate(budget, spentSoFar, periodStart, periodEnd, today): Pace(spent, limit, projected, status: ON_TRACK|AT_RISK|OVER)`; linear projection, rollover adds the unspent part of the previous period.
- `SavingsInsights` (pure): `compute(input: InsightInput, today): List<Insight>` sorted by `yearlySaving` desc then severity.
  ```kotlin
  sealed interface Insight { val id: String; val yearlySaving: Money?; val severity: Severity }
  UnusedSubscription(recurringId, worth)              // worth UNUSED/RARELY -> saving = yearly cost
  OverlappingSubscriptions(recurringIds, categoryId)  // ≥2 active subs in same category -> saving = cheapest yearly
  AnnualPlanSaving(recurringId)                       // annualAlternative < yearly cost -> difference
  PriceIncrease(recurringId, old: Money, new: Money)  // last price point within 90 days higher than previous -> yearly delta
  TrialEnding(recurringId, endsOn)                    // within 7 days; saving = yearly cost if not kept
  CategorySpike(categoryId, thisMonth, average)       // > 25% and > 20 EUR-equivalent over trailing 3-month average
  SmallFrequentSpend(merchantOrCategory, count, monthly) // ≥8 purchases/month each < 10 EUR-equiv -> projected yearly; saving = 50% of it, labelled as estimate
  BudgetAtRisk(budgetId, pace)
  NoSpendStreak(days)                                  // positive reinforcement, no saving
  ```
  Thresholds live in one `InsightRules` data class with defaults so they can be tuned and tested.
- `PaymentNotificationParser` (pure): `parse(packageName, title, text, postedAt): TransactionDraft?`. Rule table of regexes (IT + EN): "Pagamento di 12,50 EUR presso X", "Hai pagato 12,50 € a X", "Addebito ... carta ... importo", "You paid €12.50 to X", "Payment of $3.20 at X", Google Wallet style "€4.50 at X". Ignores OTP/security codes, "accredito"/incoming recognised as INCOME. Known banking/wallet package list is data, user allowlist overrides it.
- `ReceiptTextParser` (pure): OCR text → `TransactionDraft` (total from TOTALE/TOTAL/IMPORTO PAGATO lines, date in common IT/EN formats, merchant = first meaningful line, items as `name  amount` lines). Fallback when no LLM is available.
- `AiExchangeFormat` (pure): see below.
- `DeepLinkRequest` (pure): parses `tameki://add?amount=&currency=&kind=&merchant=&category=&account=&date=&note=&tags=&save=` into a `TransactionDraft` + `save: Boolean`; rejects malformed input with a reason.
- `CategoryMatcher` (pure): maps a free-text hint ("Groceries", "spesa", "supermarket") to a category id via names, synonyms (IT/EN) and history of merchant→category.

## AI exchange format v1
The single contract between the app and any chatbot/agent/automation. Documented in `docs/ai-format.md` with a JSON Schema at `docs/ai-format.schema.json` (generated from tests to stay in sync).
```json
{ "tameki": 1,
  "transactions": [{ "date": "2026-11-18", "time": "13:05", "kind": "expense", "amount": "12.50", "currency": "EUR",
                     "merchant": "Esselunga", "category": "Groceries", "account": "Card", "note": "", "tags": [],
                     "items": [{ "name": "Latte", "amount": "1.29", "quantity": 1 }] }],
  "subscriptions": [{ "name": "Netflix", "amount": "13.99", "currency": "EUR", "cycle": "1 month", "next_date": "2026-12-01",
                      "trial_ends_on": null, "category": "Entertainment", "cancel_url": null }] }
```
- `AiExchangeFormat.parse(text): AiImportResult` is lenient: strips Markdown code fences and prose around the first JSON object, accepts numbers or strings for amounts, comma decimals, missing currency (= base), missing date (= today). Returns parsed drafts plus a list of per-item `Problem(path, message)`; never throws.
- `AiPrompts` builds prompts (IT/EN by app locale) embedding the user's category and account names and the schema: `receiptExtraction()`, `naturalLanguageEntry()`, `subscriptionDiscovery()` (paste bank statement → subscriptions), `monthReview(summary: MonthSummary)` (Markdown export of the month with totals, categories, subscriptions, insights, asking the chatbot for advice).

## Repositories (interfaces in core:model, Room implementations in core:data)
`AccountRepository`, `CategoryRepository`, `TransactionRepository` (paged/filtered flows: period, account, category, text, tag, source), `RecurringRepository`, `BudgetRepository`, `GoalRepository`, `ReceiptRepository`, `InboxRepository`, `SettingsRepository` (base currency, lock, theme, notification capture allowlist, silent automation saves, insight rules). All reads are `Flow`, writes `suspend`.

## Services (interfaces in core:model or scan:api)
`ReceiptReader { suspend fun read(imagePath: String): ReceiptReadResult }` chained: Gemini Nano Prompt API (image+text, JSON output) → ML Kit OCR + `ReceiptTextParser`. `ReminderScheduler`, `BackupService` (export/import encrypted archive: JSON + receipts, AES-GCM, key from password via PBKDF2-HMAC-SHA256 310k iterations), `CsvService` (import with column mapping + export).
