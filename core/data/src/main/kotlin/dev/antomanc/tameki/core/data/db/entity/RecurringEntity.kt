package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.PricePoint
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringStatus
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Worth
import java.time.LocalDate

@Entity(
    tableName = "recurring",
    indices = [Index("nextDate"), Index("status"), Index("categoryId")],
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val cycleEvery: Int,
    val cycleUnit: String,
    val startDate: LocalDate,
    val nextDate: LocalDate,
    val trialEndsOn: LocalDate? = null,
    val status: String = RecurringStatus.ACTIVE.name,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val cancelUrl: String? = null,
    val note: String? = null,
    val worth: String? = null,
    val worthRatedOn: LocalDate? = null,
    val remindDaysBefore: Int = 2,
    val autoLog: Boolean = false,
    val annualAlternativeMinor: Long? = null,
    val annualAlternativeCurrency: String? = null,
    val brandKey: String? = null,
    val sharedWith: Int = 1,
)

@Entity(
    tableName = "recurring_price_points",
    primaryKeys = ["recurringId", "since"],
    indices = [Index("recurringId")],
)
data class PricePointEntity(
    val recurringId: Long,
    val since: LocalDate,
    val amountMinor: Long,
    val currency: String,
) {
    fun toDomain(): PricePoint = PricePoint(
        amount = Money(amountMinor, CurrencyCode(currency)),
        since = since,
    )
}

fun RecurringEntity.toDomain(priceHistory: List<PricePointEntity>): Recurring = Recurring(
    id = id,
    type = RecurringType.valueOf(type),
    name = name,
    amount = Money(amountMinor, CurrencyCode(currency)),
    cycle = Cycle(cycleEvery, CycleUnit.valueOf(cycleUnit)),
    startDate = startDate,
    nextDate = nextDate,
    trialEndsOn = trialEndsOn,
    status = RecurringStatus.valueOf(status),
    accountId = accountId,
    categoryId = categoryId,
    cancelUrl = cancelUrl,
    note = note,
    worth = worth?.let(Worth::valueOf),
    worthRatedOn = worthRatedOn,
    remindDaysBefore = remindDaysBefore,
    autoLog = autoLog,
    priceHistory = priceHistory.sortedBy { it.since }.map { it.toDomain() },
    annualAlternative = if (annualAlternativeMinor != null && annualAlternativeCurrency != null) {
        Money(annualAlternativeMinor, CurrencyCode(annualAlternativeCurrency))
    } else {
        null
    },
    brandKey = brandKey,
    sharedWith = sharedWith,
)

fun Recurring.toEntity(): RecurringEntity = RecurringEntity(
    id = id,
    type = type.name,
    name = name,
    amountMinor = amount.minor,
    currency = amount.currency.value,
    cycleEvery = cycle.every,
    cycleUnit = cycle.unit.name,
    startDate = startDate,
    nextDate = nextDate,
    trialEndsOn = trialEndsOn,
    status = status.name,
    accountId = accountId,
    categoryId = categoryId,
    cancelUrl = cancelUrl,
    note = note,
    worth = worth?.name,
    worthRatedOn = worthRatedOn,
    remindDaysBefore = remindDaysBefore,
    autoLog = autoLog,
    annualAlternativeMinor = annualAlternative?.minor,
    annualAlternativeCurrency = annualAlternative?.currency?.value,
    brandKey = brandKey,
    sharedWith = sharedWith,
)
