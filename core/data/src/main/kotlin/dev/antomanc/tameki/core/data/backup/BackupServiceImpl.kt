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
import dev.antomanc.tameki.core.model.repository.BackupService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
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
    private val workManager: WorkManager = WorkManager.getInstance(context),
) : BackupService {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override suspend fun export(destination: File, password: CharArray) = withContext(Dispatchers.IO) {
        val envelope = collectEnvelope()
        val zipBytes = buildZip(envelope)
        val encrypted = BackupCrypto.encrypt(zipBytes, password)
        destination.outputStream().use { it.write(encrypted) }
    }

    override suspend fun import(source: File, password: CharArray) = withContext(Dispatchers.IO) {
        val payload = source.readBytes()
        val zipBytes = BackupCrypto.decrypt(payload, password)
        val (envelope, images) = readZip(zipBytes)
        if (envelope.version != BACKUP_DATA_VERSION) {
            throw BackupCrypto.InvalidBackupException("Unsupported data.json version: ${envelope.version}")
        }
        db.withTransaction {
            wipeAll()
            applyEnvelope(envelope, images)
        }
    }

    override suspend fun scheduleAutoBackup(folderTreeUri: String) {
        settings.setAutoBackupFolderUri(folderTreeUri)
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .addTag(AutoBackupWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            AutoBackupWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override suspend fun cancelAutoBackup() {
        settings.setAutoBackupFolderUri(null)
        workManager.cancelUniqueWork(AutoBackupWorker.UNIQUE_NAME)
    }

    suspend fun autoBackupFolderUri(): String? = settings.observeAutoBackupFolderUri().first()

    private suspend fun collectEnvelope(): BackupEnvelope {
        val accounts = db.accountDao().observeAll(includeArchived = true).first().map {
            AccountDto(it.id, it.name, it.currency, it.type, it.archived, it.sortOrder)
        }
        val categories = db.categoryDao().observeAll(includeArchived = true).first().map {
            CategoryDto(it.id, it.name, it.kind, it.icon, it.color, it.parentId, it.archived)
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

    private fun buildZip(envelope: BackupEnvelope): ByteArray {
        val out = ByteArrayOutputStream()
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
        return out.toByteArray()
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

    private suspend fun applyEnvelope(envelope: BackupEnvelope, images: Map<String, ByteArray>) {
        val receiptsDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        for (account in envelope.accounts) {
            db.accountDao().upsert(
                AccountEntity(account.id, account.name, account.currency, account.type, account.archived, account.sortOrder),
            )
        }
        for (category in envelope.categories) {
            db.categoryDao().upsert(
                CategoryEntity(
                    category.id, category.name, category.kind, category.icon,
                    category.color, category.parentId, category.archived,
                ),
            )
        }
        for (memory in envelope.merchantMemory) {
            db.categoryDao().upsertMerchantMemory(MerchantCategoryEntity(memory.merchant, memory.categoryId))
        }
        for (receipt in envelope.receipts) {
            val entry = receipt.imageEntry ?: "receipts/${receipt.id}"
            val imageBytes = images[entry]
            val target = File(receiptsDir, "${receipt.id}.jpg")
            if (imageBytes != null) {
                target.writeBytes(imageBytes)
            }
            val path = if (target.exists()) target.absolutePath else receipt.imagePath
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
