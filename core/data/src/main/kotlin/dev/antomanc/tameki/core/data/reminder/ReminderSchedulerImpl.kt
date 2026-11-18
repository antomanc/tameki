package dev.antomanc.tameki.core.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.antomanc.tameki.core.data.R
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.ReminderScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    private val autoLogProcessor: AutoLogProcessor by inject()
    private val clock: Clock by inject()

    override suspend fun doWork(): Result {
        val today = LocalDate.now(clock)
        val all = recurringRepository.observeAll().first()
        val reminders = RenewalReminderLogic.remindersDue(all, today)
        for (reminder in reminders) {
            postNotification(reminder)
        }
        autoLogProcessor.processDue(today)
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
                reminder.on.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            )
            ReminderKind.TRIAL_ENDING -> applicationContext.getString(
                R.string.data_trial_body,
                reminder.name,
                reminder.on.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(applicationContext)
            .notify((reminder.recurringId * 10 + reminder.kind.ordinal).toInt(), notification)
    }

    companion object {
        const val UNIQUE_NAME = "tameki_renewal_reminders"
        const val TAG = "renewal_reminders"
        const val CHANNEL_ID = "Renewals"
    }
}
