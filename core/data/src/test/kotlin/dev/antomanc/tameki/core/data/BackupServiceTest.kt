package dev.antomanc.tameki.core.data

import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.backup.BackupCrypto
import dev.antomanc.tameki.core.data.backup.BackupServiceImpl
import dev.antomanc.tameki.core.data.backup.InMemoryBackupPasswordStore
import dev.antomanc.tameki.core.data.db.entity.ReceiptEntity
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Cycle
import dev.antomanc.tameki.core.model.entity.CycleUnit
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.RecurringType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.Worth
import java.io.File
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BackupServiceTest : DbTestRule() {
    private lateinit var settings: SettingsRepositoryImpl
    private lateinit var passwordStore: InMemoryBackupPasswordStore
    private lateinit var backup: BackupServiceImpl
    private val eur = CurrencyCode("EUR")
    private val password = "correct horse battery".toCharArray()

    @Before
    fun initBackup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        settings = SettingsRepositoryImpl(context)
        passwordStore = InMemoryBackupPasswordStore()
        backup = BackupServiceImpl(context, db, settings, passwordStore)
    }

    @Test
    fun roundTripExportWipeImport() = runTest {
        val accountId = accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        transactions.upsert(
            Transaction(
                kind = TransactionKind.EXPENSE,
                amount = Money(1_299L, eur),
                accountId = accountId,
                merchant = "Esselunga",
                note = "latte",
                tags = setOf("food"),
                occurredAt = LocalDateTime.of(2026, 11, 18, 13, 5),
            ),
        )

        val file = File(context.cacheDir, "roundtrip.tameki")
        backup.export(file, password)

        db.transactionDao().deleteAllTags()
        db.transactionDao().deleteAllSplits()
        db.transactionDao().deleteAll()
        db.accountDao().deleteAll()
        assertThat(transactions.observe().first()).isEmpty()

        backup.import(file, password)

        val restoredAccounts = accounts.observeAll(includeArchived = true).first()
        assertThat(restoredAccounts.map { it.name }).contains("Card")
        val restoredTx = transactions.observe().first()
        assertThat(restoredTx).hasSize(1)
        assertThat(restoredTx.single().merchant).isEqualTo("Esselunga")
        assertThat(restoredTx.single().amount.minor).isEqualTo(1_299L)
        assertThat(restoredTx.single().tags).containsExactly("food")
    }

    @Test
    fun wrongPasswordFailsCleanly() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        val file = File(context.cacheDir, "badpass.tameki")
        backup.export(file, password)

        val before = accounts.observeAll(includeArchived = true).first()
        try {
            backup.import(file, "wrong-password".toCharArray())
            throw AssertionError("expected WrongPasswordException")
        } catch (_: BackupCrypto.WrongPasswordException) {
            // expected
        }
        val after = accounts.observeAll(includeArchived = true).first()
        assertThat(after).isEqualTo(before)
    }

    @Test
    fun truncatedBackupReportsCorruptedNotWrongPassword() {
        val encrypted = BackupCrypto.encrypt("hello".toByteArray(), password)
        val truncated = encrypted.copyOf(10)
        try {
            BackupCrypto.decrypt(truncated, password)
            throw AssertionError("expected InvalidBackupException")
        } catch (e: BackupCrypto.InvalidBackupException) {
            assertThat(e.message).isEqualTo("corrupted file")
        } catch (_: BackupCrypto.WrongPasswordException) {
            throw AssertionError("truncated file must not look like wrong password")
        }
    }

    @Test
    fun roundTripKeepsTheDayASubscriptionWasRated() = runTest {
        val ratedOn = LocalDate.of(2026, 9, 1)
        recurring.upsert(
            Recurring(
                type = RecurringType.SUBSCRIPTION,
                name = "Netflix",
                amount = Money(1_399L, eur),
                cycle = Cycle(1, CycleUnit.MONTH),
                startDate = LocalDate.of(2025, 1, 1),
                nextDate = LocalDate.of(2026, 12, 1),
                worth = Worth.LOVE,
                worthRatedOn = ratedOn,
            ),
        )
        val file = File(context.cacheDir, "rating.tameki")
        backup.export(file, password)

        db.recurringDao().deleteAllPricePoints()
        db.recurringDao().deleteAll()

        backup.import(file, password)

        val restored = recurring.observeAll().first().single()
        assertThat(restored.worth).isEqualTo(Worth.LOVE)
        assertThat(restored.worthRatedOn).isEqualTo(ratedOn)
    }

    @Test
    fun importWritesAnEncryptedPreRestoreSnapshotOfTheOldData() = runTest {
        val oldId = accounts.upsert(Account(name = "Old card", currency = eur, type = AccountType.CARD))
        val file = File(context.cacheDir, "snapshot-source.tameki")
        backup.export(file, password)

        // The account is about to be replaced; the automatic snapshot must still carry it.
        accounts.upsert(Account(id = oldId, name = "Renamed", currency = eur, type = AccountType.CARD))
        backup.import(file, password)

        val snapshot = File(context.filesDir, "pre-restore").listFiles()
            ?.filter { it.name.startsWith("pre-restore-") }
            ?.maxByOrNull { it.lastModified() }
        assertThat(snapshot).isNotNull()
        val zipBytes = BackupCrypto.decrypt(requireNotNull(snapshot).readBytes(), password)
        var dataJson: String? = null
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "data.json") dataJson = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertThat(dataJson).isNotNull()
        assertThat(dataJson!!).contains("Renamed")
    }

    @Test
    fun missingReceiptImageDoesNotReuseExistingFile() = runTest {
        accounts.upsert(Account(name = "Card", currency = eur, type = AccountType.CARD))
        // Image path that does not exist → export omits receipt bytes from the zip.
        db.receiptDao().upsert(
            ReceiptEntity(
                id = 42,
                imagePath = File(context.cacheDir, "missing-source.jpg").absolutePath,
                rawText = null,
                createdAt = Instant.parse("2026-11-18T12:00:00Z"),
            ),
        )
        val file = File(context.cacheDir, "no-image.tameki")
        backup.export(file, password)

        db.receiptDao().deleteAllItems()
        db.receiptDao().deleteAll()
        db.accountDao().deleteAll()

        val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        val stale = File(receiptsDir, "42.jpg")
        stale.writeText("STALE_IMAGE")

        backup.import(file, password)

        val restored = db.receiptDao().get(42)
        assertThat(restored).isNotNull()
        assertThat(restored!!.imagePath).isEmpty()
        assertThat(stale.readText()).isEqualTo("STALE_IMAGE")
    }
}
