package dev.antomanc.tameki.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.antomanc.tameki.core.data.db.dao.AccountDao
import dev.antomanc.tameki.core.data.db.dao.BudgetDao
import dev.antomanc.tameki.core.data.db.dao.CategoryDao
import dev.antomanc.tameki.core.data.db.dao.ExchangeRateDao
import dev.antomanc.tameki.core.data.db.dao.GoalDao
import dev.antomanc.tameki.core.data.db.dao.InboxDao
import dev.antomanc.tameki.core.data.db.dao.ReceiptDao
import dev.antomanc.tameki.core.data.db.dao.RecurringDao
import dev.antomanc.tameki.core.data.db.dao.TransactionDao
import dev.antomanc.tameki.core.data.db.entity.AccountEntity
import dev.antomanc.tameki.core.data.db.entity.BudgetCategoryEntity
import dev.antomanc.tameki.core.data.db.entity.BudgetEntity
import dev.antomanc.tameki.core.data.db.entity.CategoryEntity
import dev.antomanc.tameki.core.data.db.entity.ExchangeRateEntity
import dev.antomanc.tameki.core.data.db.entity.GoalEntity
import dev.antomanc.tameki.core.data.db.entity.InboxEntity
import dev.antomanc.tameki.core.data.db.entity.MerchantCategoryEntity
import dev.antomanc.tameki.core.data.db.entity.PricePointEntity
import dev.antomanc.tameki.core.data.db.entity.ReceiptEntity
import dev.antomanc.tameki.core.data.db.entity.ReceiptItemEntity
import dev.antomanc.tameki.core.data.db.entity.RecurringEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionFtsEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionSplitEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionTagEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        MerchantCategoryEntity::class,
        TransactionEntity::class,
        TransactionFtsEntity::class,
        TransactionSplitEntity::class,
        TransactionTagEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        RecurringEntity::class,
        PricePointEntity::class,
        BudgetEntity::class,
        BudgetCategoryEntity::class,
        GoalEntity::class,
        InboxEntity::class,
        ExchangeRateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TamekiDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun recurringDao(): RecurringDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun inboxDao(): InboxDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        const val NAME = "tameki.db"

        fun build(context: Context, name: String = NAME): TamekiDatabase =
            Room.databaseBuilder(context.applicationContext, TamekiDatabase::class.java, name)
                .build()

        fun inMemory(context: Context): TamekiDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, TamekiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
