package dev.antomanc.tameki.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["baseCurrency", "quoteCurrency", "rateDate", "source"],
    indices = [Index("rateDate"), Index("baseCurrency", "quoteCurrency")],
)
data class ExchangeRateEntity(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rateDate: LocalDate,
    /** Decimal string: units of quote per 1 unit of base. */
    val rate: String,
    val source: String,
) {
    companion object {
        const val SOURCE_MANUAL = "MANUAL"
        const val SOURCE_ECB = "ECB"
    }
}
