package dev.antomanc.tameki.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.repository.AccountRepositoryImpl
import dev.antomanc.tameki.core.data.repository.BudgetRepositoryImpl
import dev.antomanc.tameki.core.data.repository.CategoryRepositoryImpl
import dev.antomanc.tameki.core.data.repository.GoalRepositoryImpl
import dev.antomanc.tameki.core.data.repository.InboxRepositoryImpl
import dev.antomanc.tameki.core.data.repository.ReceiptRepositoryImpl
import dev.antomanc.tameki.core.data.repository.RecurringRepositoryImpl
import dev.antomanc.tameki.core.data.repository.TransactionRepositoryImpl
import dev.antomanc.tameki.core.data.seed.DatabaseSeeder
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
abstract class DbTestRule {
    protected lateinit var context: Context
    protected lateinit var db: TamekiDatabase
    protected lateinit var accounts: AccountRepositoryImpl
    protected lateinit var categories: CategoryRepositoryImpl
    protected lateinit var transactions: TransactionRepositoryImpl
    protected lateinit var recurring: RecurringRepositoryImpl
    protected lateinit var budgets: BudgetRepositoryImpl
    protected lateinit var goals: GoalRepositoryImpl
    protected lateinit var receipts: ReceiptRepositoryImpl
    protected lateinit var inbox: InboxRepositoryImpl

    @Before
    fun setUpDb() {
        context = ApplicationProvider.getApplicationContext()
        db = TamekiDatabase.inMemory(context)
        accounts = AccountRepositoryImpl(db.accountDao())
        categories = CategoryRepositoryImpl(db.categoryDao())
        transactions = TransactionRepositoryImpl(db, categories)
        recurring = RecurringRepositoryImpl(db)
        budgets = BudgetRepositoryImpl(db)
        goals = GoalRepositoryImpl(db.goalDao())
        receipts = ReceiptRepositoryImpl(db)
        inbox = InboxRepositoryImpl(db.inboxDao())
    }

    protected fun seed() {
        kotlinx.coroutines.runBlocking {
            DatabaseSeeder().seedIfEmpty(db, context)
        }
    }

    @After
    fun tearDownDb() {
        db.close()
    }
}
