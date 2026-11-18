package dev.antomanc.tameki.core.data

import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dev.antomanc.tameki.core.data.backup.BackupCrypto
import dev.antomanc.tameki.core.data.backup.BackupServiceImpl
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.Money
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class BackupServiceTest : DbTestRule() {
    private lateinit var settings: SettingsRepositoryImpl
    private lateinit var backup: BackupServiceImpl
    private val eur = CurrencyCode("EUR")
    private val password = "correct horse battery".toCharArray()

    @Before
    fun initBackup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        settings = SettingsRepositoryImpl(context)
        backup = BackupServiceImpl(context, db, settings)
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
}
