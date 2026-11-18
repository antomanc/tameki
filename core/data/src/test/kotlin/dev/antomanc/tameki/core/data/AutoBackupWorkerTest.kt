package dev.antomanc.tameki.core.data

import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.backup.AutoBackupWorker
import dev.antomanc.tameki.core.data.backup.BackupServiceImpl
import dev.antomanc.tameki.core.data.backup.InMemoryBackupPasswordStore
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.repository.BackupPasswordStore
import dev.antomanc.tameki.core.model.repository.BackupService
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

@Config(sdk = [34])
class AutoBackupWorkerTest : DbTestRule() {
    private lateinit var settings: SettingsRepositoryImpl
    private lateinit var passwordStore: InMemoryBackupPasswordStore
    private lateinit var backup: BackupServiceImpl
    private val eur = CurrencyCode("EUR")
    private val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun init() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        settings = SettingsRepositoryImpl(context)
        passwordStore = InMemoryBackupPasswordStore()
        backup = BackupServiceImpl(context, db, settings, passwordStore)
        runCatching { stopKoin() }
        startKoin {
            modules(
                module {
                    single<BackupService> { backup }
                    single<BackupPasswordStore> { passwordStore }
                    single<Clock> { clock }
                },
            )
        }
    }

    @After
    fun tearDownKoin() {
        runCatching { stopKoin() }
    }

    @Test
    fun writesEncryptedBackupAndRecordsSuccess() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val folder = File(context.cacheDir, "auto_backups").also { it.mkdirs() }
        backup.scheduleAutoBackup(folder.toURI().toString(), "secret".toCharArray())

        val worker = TestListenableWorkerBuilder<AutoBackupWorker>(context).build()
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        val files = folder.listFiles { f -> f.name.endsWith(".tameki") }?.toList().orEmpty()
        assertThat(files).hasSize(1)
        assertThat(files.single().name).contains("tameki-autobackup-")
        assertThat(settings.observeAutoBackupLastSuccessAt().first()).isEqualTo(Instant.parse("2026-11-18T12:00:00Z"))
        assertThat(settings.observeAutoBackupLastFailureAt().first()).isNull()
    }

    @Test
    fun lostFolderPermissionRecordsFailure() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        passwordStore.save("secret".toCharArray())
        settings.setAutoBackupFolderUri("content://com.android.externalstorage.documents/tree/primary%3ALost")

        val worker = TestListenableWorkerBuilder<AutoBackupWorker>(context).build()
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        assertThat(settings.observeAutoBackupLastFailureAt().first()).isNotNull()
        assertThat(settings.observeAutoBackupLastFailureReason().first()).contains("permission")
    }
}
