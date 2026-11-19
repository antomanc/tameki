package dev.antomanc.tameki.core.data.backup

import android.content.Context
import androidx.room.withTransaction
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.antomanc.tameki.core.data.db.TamekiDatabase
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
import dev.antomanc.tameki.core.data.db.entity.TransactionSplitEntity
import dev.antomanc.tameki.core.data.db.entity.TransactionTagEntity
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.repository.BackupPasswordStore
import dev.antomanc.tameki.core.model.repository.BackupService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupServiceImpl(
    private val context: Context,
    private val db: TamekiDatabase,
    private val settings: SettingsRepositoryImpl,
    private val passwordStore: BackupPasswordStore,
    private val workManager: WorkManager = WorkManager.getInstance(context),
) : BackupService {
    private companion object {
        const val PRE_RESTORE_DIR = "pre-restore"
        const val PRE_RESTORE_KEEP = 3
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override suspend fun export(destination: File, password: CharArray) = withContext(Dispatchers.IO) {
        val envelope = collectEnvelope()
        // Zip to a temporary file and encrypt it as a stream: receipt photos never sit in memory
        // twice (zip + ciphertext), which ran low-RAM phones out of memory.
        writeEncrypted(envelope, destination, password)
    }

    override suspend fun import(source: File, password: CharArray) = withContext(Dispatchers.IO) {
        val payload = source.readBytes()
        val zipBytes = BackupCrypto.decrypt(payload, password)
        val (envelope, images) = readZip(zipBytes)
        if (envelope.version != BACKUP_DATA_VERSION) {
            throw BackupCrypto.InvalidBackupException("Unsupported data.json version: ${envelope.version}")
        }
        val stageDir = File(context.cacheDir, "backup_import_stage_${System.nanoTime()}").also {
            it.mkdirs()
        }
        var snapshot: File? = null
        try {
            val stagedPaths = stageImages(envelope, images, stageDir)
            // The restore replaces everything. Keep an encrypted copy of what is on the device
            // first, in the same format the user exports; if the new data cannot be written or
            // does not verify, roll the old database back from it.
            snapshot = writePreRestoreSnapshot(password)
            db.withTransaction {
                wipeAll()
                applyEnvelope(envelope, stagedPaths)
            }
            commitStagedImages(stagedPaths)
            verifyRestored(envelope)
        } catch (t: Throwable) {
            snapshot?.let { rollbackFrom(it, password) }
            throw t
        } finally {
            stageDir.deleteRecursively()
        }
    }

    /**
     * Writes the current database to `files/pre-restore/`, encrypted with the same password the
     * user restored with, and keeps the three newest copies. Throws before anything is wiped when
     * the copy cannot be written, so a restore never replaces data it cannot roll back.
     */
    private suspend fun writePreRestoreSnapshot(password: CharArray): File {
        val envelope = collectEnvelope()
        val dir = File(context.filesDir, PRE_RESTORE_DIR)
        if (!dir.isDirectory && !dir.mkdirs()) {
            throw IOException("Could not create the pre-restore folder")
        }
        val file = File(dir, "pre-restore-${System.nanoTime()}.tameki")
        writeEncrypted(envelope, file, password)
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("pre-restore-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(PRE_RESTORE_KEEP)
            ?.forEach { it.delete() }
        return file
    }

    /** Reinstalls [snapshot] after a failed restore. Never masks the failure that triggered it. */
    private suspend fun rollbackFrom(snapshot: File, password: CharArray) {
        runCatchingCancellable {
            val (envelope, images) = readZip(BackupCrypto.decrypt(snapshot.readBytes(), password))
            val stageDir = File(context.cacheDir, "pre_restore_rollback_${System.nanoTime()}").also {
                it.mkdirs()
            }
            try {
                val staged = stageImages(envelope, images, stageDir)
                db.withTransaction {
                    wipeAll()
                    applyEnvelope(envelope, staged)
                }
                commitStagedImages(staged)
            } finally {
                stageDir.deleteRecursively()
            }
        }
    }

    /** Cheap end-to-end check that the transaction wrote what the backup said it would. */
    private suspend fun verifyRestored(envelope: BackupEnvelope) {
        val transactions = db.transactionDao().getAll().size
        val accounts = db.accountDao().observeAll(includeArchived = true).first().size
        val recurring = db.recurringDao().getAll().size
        if (transactions != envelope.transactions.size ||
            accounts != envelope.accounts.size ||
            recurring != envelope.recurring.size
        ) {
            throw IOException(
                "Restore verification failed: " +
                    "transactions $transactions/${envelope.transactions.size}, " +
                    "accounts $accounts/${envelope.accounts.size}, " +
                    "recurring $recurring/${envelope.recurring.size}",
            )
        }
    }

    override suspend fun scheduleAutoBackup(folderTreeUri: String, password: CharArray) {
        passwordStore.save(password)
        settings.setAutoBackupFolderUri(folderTreeUri)
        val days = settings.observeAutoBackupFrequencyDays().first().toLong().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(days, TimeUnit.DAYS)
            .addTag(AutoBackupWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override suspend fun cancelAutoBackup() {
        passwordStore.clear()
        settings.setAutoBackupFolderUri(null)
        settings.setAutoBackupLastFailureAt(null)
        settings.setAutoBackupLastFailureReason(null)
        workManager.cancelUniqueWork(AutoBackupWorker.UNIQUE_NAME)
    }

    suspend fun autoBackupFolderUri(): String? = settings.observeAutoBackupFolderUri().first()

    fun settingsRepository(): SettingsRepositoryImpl = settings

    fun passwordStore(): BackupPasswordStore = passwordStore

    private suspend fun collectEnvelope(): BackupEnvelope {
        val accounts = db.accountDao().observeAll(includeArchived = true).first().map {
            AccountDto(
                it.id, it.name, it.currency, it.type, it.archived, it.sortOrder,
                it.seedKey, it.customName,
            )
        }
        val categories = db.categoryDao().observeAll(includeArchived = true).first().map {
            CategoryDto(
                it.id, it.name, it.kind, it.icon, Category.normalizeArgb(it.color), it.parentId, it.archived,
                it.seedKey, it.customName,
            )
        }
        val merchantMemory = db.categoryDao().observeMerchantMemory().first().map {
            MerchantMemoryDto(it.merchant, it.categoryId)
        }
        val txEntities = db.transactionDao().getAll()
        val transactions = txEntities.map { tx ->
            val splits = db.transactionDao().splitsFor(tx.id)
            val tags = db.transactionDao().tagsFor(tx.id)
            TransactionDto(
                id = tx.id,
                kind = tx.kind,
                amountMinor = tx.amountMinor,
                currency = tx.currency,
                accountId = tx.accountId,
                toAccountId = tx.toAccountId,
                categoryId = tx.categoryId,
                merchant = tx.merchant,
                note = tx.note,
                tags = tags.map { it.tag },
                occurredAt = tx.occurredAt.toString(),
                receiptId = tx.receiptId,
                recurringId = tx.recurringId,
                source = tx.source,
                splits = splits.map {
                    SplitDto(it.categoryId, it.amountMinor, it.currency, it.note)
                },
            )
        }
        val receipts = db.receiptDao().getAll().map { receipt ->
            val items = db.receiptDao().itemsFor(receipt.id)
            ReceiptDto(
                id = receipt.id,
                imagePath = receipt.imagePath,
                imageEntry = "receipts/${receipt.id}",
                rawText = receipt.rawText,
                createdAt = receipt.createdAt.toString(),
                items = items.map {
                    ReceiptItemDto(it.name, it.amountMinor, it.currency, it.quantity)
                },
            )
        }
        val recurring = db.recurringDao().getAll().map { r ->
            val prices = db.recurringDao().pricePointsFor(r.id)
            RecurringDto(
                id = r.id,
                type = r.type,
                name = r.name,
                amountMinor = r.amountMinor,
                currency = r.currency,
                cycleEvery = r.cycleEvery,
                cycleUnit = r.cycleUnit,
                startDate = r.startDate.toString(),
                nextDate = r.nextDate.toString(),
                trialEndsOn = r.trialEndsOn?.toString(),
                status = r.status,
                accountId = r.accountId,
                categoryId = r.categoryId,
                cancelUrl = r.cancelUrl,
                note = r.note,
                worth = r.worth,
                worthRatedOn = r.worthRatedOn?.toString(),
                remindDaysBefore = r.remindDaysBefore,
                autoLog = r.autoLog,
                annualAlternativeMinor = r.annualAlternativeMinor,
                annualAlternativeCurrency = r.annualAlternativeCurrency,
                brandKey = r.brandKey,
                sharedWith = r.sharedWith,
                priceHistory = prices.map {
                    PricePointDto(it.amountMinor, it.currency, it.since.toString())
                },
            )
        }
        val budgets = db.budgetDao().getAll().map { b ->
            BudgetDto(
                id = b.id,
                name = b.name,
                limitMinor = b.limitMinor,
                currency = b.currency,
                period = b.period,
                rollover = b.rollover,
                categoryIds = db.budgetDao().categoriesFor(b.id).map { it.categoryId },
            )
        }
        val goals = db.goalDao().getAll().map {
            GoalDto(
                it.id, it.name, it.targetMinor, it.targetCurrency,
                it.savedMinor, it.savedCurrency, it.deadline?.toString(), it.icon, it.archived,
            )
        }
        val inbox = db.inboxDao().getAll().map {
            InboxDto(it.id, it.draftJson, it.origin, it.rawText, it.receivedAt.toString(), it.status)
        }
        val rates = db.exchangeRateDao().getAll().map {
            ExchangeRateDto(it.baseCurrency, it.quoteCurrency, it.rateDate.toString(), it.rate, it.source)
        }
        return BackupEnvelope(
            version = BACKUP_DATA_VERSION,
            accounts = accounts,
            categories = categories,
            merchantMemory = merchantMemory,
            transactions = transactions,
            receipts = receipts,
            recurring = recurring,
            budgets = budgets,
            goals = goals,
            inbox = inbox,
            exchangeRates = rates,
        )
    }

    private fun writeEncrypted(envelope: BackupEnvelope, destination: File, password: CharArray) {
        val zipFile = File.createTempFile("tameki-backup", ".zip", destination.absoluteFile.parentFile)
        try {
            zipFile.outputStream().buffered().use { writeZip(envelope, it) }
            zipFile.inputStream().buffered().use { input ->
                destination.outputStream().buffered().use { output -> BackupCrypto.encrypt(input, output, password) }
            }
        } finally {
            zipFile.delete()
        }
    }

    private fun writeZip(envelope: BackupEnvelope, out: java.io.OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.encodeToString(envelope).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (receipt in envelope.receipts) {
                val file = File(receipt.imagePath)
                if (!file.isFile) continue
                val entryName = receipt.imageEntry ?: "receipts/${receipt.id}"
                zip.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun readZip(bytes: ByteArray): Pair<BackupEnvelope, Map<String, ByteArray>> {
        var envelope: BackupEnvelope? = null
        val images = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val content = zip.readBytes()
                if (name == "data.json") {
                    envelope = json.decodeFromString<BackupEnvelope>(content.toString(Charsets.UTF_8))
                } else if (name.startsWith("receipts/")) {
                    images[name] = content
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val data = envelope ?: throw BackupCrypto.InvalidBackupException("data.json missing")
        return data to images
    }

    private suspend fun wipeAll() {
        db.transactionDao().deleteAllTags()
        db.transactionDao().deleteAllSplits()
        db.transactionDao().deleteAll()
        db.receiptDao().deleteAllItems()
        db.receiptDao().deleteAll()
        db.recurringDao().deleteAllPricePoints()
        db.recurringDao().deleteAll()
        db.budgetDao().deleteAllCategories()
        db.budgetDao().deleteAll()
        db.goalDao().deleteAll()
        db.inboxDao().deleteAll()
        db.categoryDao().deleteAllMerchantMemory()
        db.categoryDao().deleteAll()
        db.accountDao().deleteAll()
        db.exchangeRateDao().deleteAll()
    }

    /**
     * Writes receipt images into [stageDir] only. Missing zip entries do not reuse an existing file.
     * Returns map of receipt id → staged file (absent when the backup had no image bytes).
     */
    private fun stageImages(
        envelope: BackupEnvelope,
        images: Map<String, ByteArray>,
        stageDir: File,
    ): Map<Long, File> {
        val staged = mutableMapOf<Long, File>()
        for (receipt in envelope.receipts) {
            val entry = receipt.imageEntry ?: "receipts/${receipt.id}"
            val imageBytes = images[entry] ?: continue
            val file = File(stageDir, "${receipt.id}.jpg")
            file.writeBytes(imageBytes)
            staged[receipt.id] = file
        }
        return staged
    }

    private fun commitStagedImages(staged: Map<Long, File>) {
        val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        for ((id, stagedFile) in staged) {
            val target = File(receiptsDir, "$id.jpg")
            stagedFile.copyTo(target, overwrite = true)
        }
    }

    private suspend fun applyEnvelope(envelope: BackupEnvelope, stagedImages: Map<Long, File>) {
        val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        for (account in envelope.accounts) {
            db.accountDao().upsert(
                AccountEntity(
                    account.id, account.name, account.currency, account.type,
                    account.archived, account.sortOrder, account.seedKey, account.customName,
                ),
            )
        }
        for (category in envelope.categories) {
            db.categoryDao().upsert(
                CategoryEntity(
                    category.id, category.name, category.kind, category.icon,
                    Category.normalizeArgb(category.color), category.parentId, category.archived,
                    category.seedKey, category.customName,
                ),
            )
        }
        for (memory in envelope.merchantMemory) {
            db.categoryDao().upsertMerchantMemory(MerchantCategoryEntity(memory.merchant, memory.categoryId))
        }
        for (receipt in envelope.receipts) {
            val staged = stagedImages[receipt.id]
            val path = if (staged != null) {
                File(receiptsDir, "${receipt.id}.jpg").absolutePath
            } else {
                // Never reuse a pre-existing on-disk file for a missing backup entry.
                ""
            }
            db.receiptDao().upsert(
                ReceiptEntity(receipt.id, path, receipt.rawText, Instant.parse(receipt.createdAt)),
            )
            db.receiptDao().upsertItems(
                receipt.items.mapIndexed { index, item ->
                    ReceiptItemEntity(receipt.id, index, item.name, item.amountMinor, item.currency, item.quantity)
                },
            )
        }
        for (tx in envelope.transactions) {
            db.transactionDao().upsert(
                TransactionEntity(
                    id = tx.id,
                    kind = tx.kind,
                    amountMinor = tx.amountMinor,
                    currency = tx.currency,
                    accountId = tx.accountId,
                    toAccountId = tx.toAccountId,
                    categoryId = tx.categoryId,
                    merchant = tx.merchant,
                    note = tx.note,
                    occurredAt = LocalDateTime.parse(tx.occurredAt),
                    receiptId = tx.receiptId,
                    recurringId = tx.recurringId,
                    source = tx.source,
                ),
            )
            db.transactionDao().upsertSplits(
                tx.splits.mapIndexed { index, split ->
                    TransactionSplitEntity(tx.id, index, split.categoryId, split.amountMinor, split.currency, split.note)
                },
            )
            db.transactionDao().upsertTags(tx.tags.map { TransactionTagEntity(tx.id, it) })
        }
        for (r in envelope.recurring) {
            db.recurringDao().upsert(
                RecurringEntity(
                    id = r.id,
                    type = r.type,
                    name = r.name,
                    amountMinor = r.amountMinor,
                    currency = r.currency,
                    cycleEvery = r.cycleEvery,
                    cycleUnit = r.cycleUnit,
                    startDate = LocalDate.parse(r.startDate),
                    nextDate = LocalDate.parse(r.nextDate),
                    trialEndsOn = r.trialEndsOn?.let(LocalDate::parse),
                    status = r.status,
                    accountId = r.accountId,
                    categoryId = r.categoryId,
                    cancelUrl = r.cancelUrl,
                    note = r.note,
                    worth = r.worth,
                    worthRatedOn = r.worthRatedOn?.let(LocalDate::parse),
                    remindDaysBefore = r.remindDaysBefore,
                    autoLog = r.autoLog,
                    annualAlternativeMinor = r.annualAlternativeMinor,
                    annualAlternativeCurrency = r.annualAlternativeCurrency,
                    brandKey = r.brandKey,
                    sharedWith = r.sharedWith,
                ),
            )
            db.recurringDao().upsertPricePoints(
                r.priceHistory.map {
                    PricePointEntity(r.id, LocalDate.parse(it.since), it.amountMinor, it.currency)
                },
            )
        }
        for (b in envelope.budgets) {
            db.budgetDao().upsert(
                BudgetEntity(b.id, b.name, b.limitMinor, b.currency, b.period, b.rollover),
            )
            db.budgetDao().upsertCategories(
                b.categoryIds.map { BudgetCategoryEntity(b.id, it) },
            )
        }
        for (g in envelope.goals) {
            db.goalDao().upsert(
                GoalEntity(
                    g.id, g.name, g.targetMinor, g.targetCurrency, g.savedMinor, g.savedCurrency,
                    g.deadline?.let(LocalDate::parse), g.icon, g.archived,
                ),
            )
        }
        for (item in envelope.inbox) {
            db.inboxDao().upsert(
                InboxEntity(
                    item.id, item.draftJson, item.origin, item.rawText,
                    Instant.parse(item.receivedAt), item.status,
                ),
            )
        }
        for (rate in envelope.exchangeRates) {
            db.exchangeRateDao().upsert(
                ExchangeRateEntity(
                    rate.baseCurrency, rate.quoteCurrency,
                    LocalDate.parse(rate.rateDate), rate.rate, rate.source,
                ),
            )
        }
    }
}
