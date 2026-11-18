package dev.antomanc.tameki.core.data.di

import dev.antomanc.tameki.core.data.backup.AutoBackupWorker
import dev.antomanc.tameki.core.data.backup.BackupServiceImpl
import dev.antomanc.tameki.core.data.backup.KeystoreBackupPasswordStore
import dev.antomanc.tameki.core.data.csv.CsvServiceImpl
import dev.antomanc.tameki.core.data.db.TamekiDatabase
import dev.antomanc.tameki.core.data.exchange.FrankfurterExchangeRates
import dev.antomanc.tameki.core.data.reminder.AutoLogProcessor
import dev.antomanc.tameki.core.data.reminder.ReminderSchedulerImpl
import dev.antomanc.tameki.core.data.reminder.RenewalReminderWorker
import dev.antomanc.tameki.core.data.repository.AccountRepositoryImpl
import dev.antomanc.tameki.core.data.repository.BudgetRepositoryImpl
import dev.antomanc.tameki.core.data.repository.CategoryRepositoryImpl
import dev.antomanc.tameki.core.data.repository.GoalRepositoryImpl
import dev.antomanc.tameki.core.data.repository.InboxRepositoryImpl
import dev.antomanc.tameki.core.data.repository.ReceiptRepositoryImpl
import dev.antomanc.tameki.core.data.repository.RecurringRepositoryImpl
import dev.antomanc.tameki.core.data.repository.TransactionRepositoryImpl
import dev.antomanc.tameki.core.data.seed.DatabaseSeeder
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.data.snooze.InsightSnoozeRepositoryImpl
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.BackupPasswordStore
import dev.antomanc.tameki.core.model.repository.BackupService
import dev.antomanc.tameki.core.model.repository.BudgetRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.CsvService
import dev.antomanc.tameki.core.model.repository.GoalRepository
import dev.antomanc.tameki.core.model.repository.InsightSnoozeRepository
import dev.antomanc.tameki.core.model.repository.InboxRepository
import dev.antomanc.tameki.core.model.repository.ReceiptRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Room, DataStore, WorkManager workers and every repository/service behind a domain interface. */
val dataModule = module {
    single { DatabaseSeeder() }
    single {
        TamekiDatabase.build(androidContext(), seeder = get())
    }
    single { get<TamekiDatabase>().accountDao() }
    single { get<TamekiDatabase>().categoryDao() }
    single { get<TamekiDatabase>().transactionDao() }
    single { get<TamekiDatabase>().receiptDao() }
    single { get<TamekiDatabase>().recurringDao() }
    single { get<TamekiDatabase>().budgetDao() }
    single { get<TamekiDatabase>().goalDao() }
    single { get<TamekiDatabase>().inboxDao() }
    single { get<TamekiDatabase>().exchangeRateDao() }

    single { Clock.systemDefaultZone() }

    singleOf(::AccountRepositoryImpl) bind AccountRepository::class
    singleOf(::CategoryRepositoryImpl) bind CategoryRepository::class
    single {
        TransactionRepositoryImpl(get(), get())
    } bind TransactionRepository::class
    singleOf(::RecurringRepositoryImpl) bind RecurringRepository::class
    singleOf(::BudgetRepositoryImpl) bind BudgetRepository::class
    singleOf(::GoalRepositoryImpl) bind GoalRepository::class
    singleOf(::ReceiptRepositoryImpl) bind ReceiptRepository::class
    singleOf(::InboxRepositoryImpl) bind InboxRepository::class
    single { SettingsRepositoryImpl(androidContext()) } bind SettingsRepository::class
    single<InsightSnoozeRepository> { InsightSnoozeRepositoryImpl(androidContext()) }

    single { FrankfurterExchangeRates(get(), clock = get()) } bind ExchangeRates::class
    single<BackupPasswordStore> { KeystoreBackupPasswordStore(androidContext()) }
    single<BackupService> {
        BackupServiceImpl(androidContext(), get(), get(), get())
    }
    single<CsvService> { CsvServiceImpl(get(), get(), get(), get(), get()) }
    single { ReminderSchedulerImpl(androidContext()) } bind ReminderScheduler::class
    single { AutoLogProcessor(get()) }

    workerOf(::RenewalReminderWorker)
    workerOf(::AutoBackupWorker)
}
