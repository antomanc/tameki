package dev.antomanc.tameki.core.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.antomanc.tameki.core.data.R
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.recurring.RecurrenceCalculator
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderSchedulerImpl(
    private val context: Context,
    private val workManager: WorkManager = WorkManager.getInstance(context),
) : ReminderScheduler {
    override suspend fun ensureDailyWorker() {
        ensureChannel()
        val request = PeriodicWorkRequestBuilder<RenewalReminderWorker>(1, TimeUnit.DAYS)
            .addTag(RenewalReminderWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            RenewalReminderWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun scheduleRecurringReminder(recurringId: Long, on: LocalDate, daysBefore: Int) {
        ensureDailyWorker()
    }

    override suspend fun cancelRecurringReminder(recurringId: Long) {
        // Daily worker skips remindDaysBefore == -1; nothing per-id to cancel.
    }

    override suspend fun scheduleTrialEnding(recurringId: Long, endsOn: LocalDate) {
        ensureDailyWorker()
    }

    override suspend fun cancelTrialEnding(recurringId: Long) {
        // Handled by status / trialEndsOn on the recurring row.
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            RenewalReminderWorker.CHANNEL_ID,
            context.getString(R.string.data_renewal_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.data_renewal_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}

class RenewalReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
    private val recurringRepository: RecurringRepository by inject()
    private val transactionRepository: TransactionRepository by inject()
    private val clock: Clock by inject()

    override suspend fun doWork(): Result {
        val today = LocalDate.now(clock)
        val all = recurringRepository.observeAll().first()
        val reminders = RenewalReminderLogic.remindersDue(all, today)
        for (reminder in reminders) {
            postNotification(reminder)
        }
        for (action in RenewalReminderLogic.autoLogsDue(all, today)) {
            val accountId = action.recurring.accountId ?: continue
            val kind = when (action.recurring.type) {
                RecurringType.INCOME -> TransactionKind.INCOME
                else -> TransactionKind.EXPENSE
            }
            transactionRepository.upsert(
                Transaction(
                    kind = kind,
                    amount = action.recurring.amount,
                    accountId = accountId,
                    categoryId = action.recurring.categoryId,
                    merchant = action.recurring.name,
                    occurredAt = LocalDateTime.of(action.chargeDate, LocalTime.NOON),
                    recurringId = action.recurring.id,
                    source = TransactionSource.RECURRING,
                ),
            )
            val advanced = action.recurring.copy(
                nextDate = RecurrenceCalculator.nextAfter(action.recurring, action.chargeDate),
            )
            recurringRepository.upsert(advanced)
        }
        return Result.success()
    }

    private fun postNotification(reminder: ReminderNotification) {
        val title = when (reminder.kind) {
            ReminderKind.RENEWAL -> applicationContext.getString(R.string.data_renewal_title)
            ReminderKind.TRIAL_ENDING -> applicationContext.getString(R.string.data_trial_title)
        }
        val body = when (reminder.kind) {
            ReminderKind.RENEWAL -> applicationContext.getString(
                R.string.data_renewal_body,
                reminder.name,
                reminder.on.toString(),
            )
            ReminderKind.TRIAL_ENDING -> applicationContext.getString(
                R.string.data_trial_body,
                reminder.name,
                reminder.on.toString(),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext)
            .notify((reminder.recurringId * 10 + reminder.kind.ordinal).toInt(), notification)
    }

    companion object {
        const val UNIQUE_NAME = "tameki_renewal_reminders"
        const val TAG = "renewal_reminders"
        const val CHANNEL_ID = "Renewals"
    }
}
