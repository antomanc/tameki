package dev.antomanc.tameki.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.antomanc.tameki.core.data.db.entity.ExchangeRateEntity
import java.time.LocalDate

@Dao
interface ExchangeRateDao {
    @Query(
        """
        SELECT * FROM exchange_rates
        WHERE baseCurrency = :base AND quoteCurrency = :quote
          AND rateDate <= :on
        ORDER BY
          rateDate DESC,
          CASE source WHEN 'MANUAL' THEN 0 ELSE 1 END
        LIMIT 1
        """,
    )
    suspend fun findRate(base: String, quote: String, on: LocalDate): ExchangeRateEntity?

    @Query(
        """
        SELECT * FROM exchange_rates
        WHERE source = :source
        ORDER BY rateDate DESC
        LIMIT 1
        """,
    )
    suspend fun latestBySource(source: String): ExchangeRateEntity?

    @Upsert
    suspend fun upsert(entity: ExchangeRateEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExchangeRateEntity>)

    @Query("SELECT * FROM exchange_rates WHERE source = :source")
    suspend fun allBySource(source: String): List<ExchangeRateEntity>

    @Query("DELETE FROM exchange_rates WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRateEntity>
}
