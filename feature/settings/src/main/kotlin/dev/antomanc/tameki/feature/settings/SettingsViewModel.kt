package dev.antomanc.tameki.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.antomanc.tameki.core.data.settings.CaptureAllowlistNone
import dev.antomanc.tameki.core.data.settings.SettingsRepositoryImpl
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.Category
import dev.antomanc.tameki.core.model.money.ExchangeRates
import dev.antomanc.tameki.core.model.money.MoneyParser
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.BackupPasswordStore
import dev.antomanc.tameki.core.model.repository.BackupService
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.CsvColumns
import dev.antomanc.tameki.core.model.repository.CsvImportOptions
import dev.antomanc.tameki.core.model.repository.CsvService
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val settingsImpl: SettingsRepositoryImpl,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val backup: BackupService,
    private val backupPasswords: BackupPasswordStore,
    private val csv: CsvService,
    private val exchangeRates: ExchangeRates,
    private val captureBridge: CaptureSettingsBridge,
    private val nanoBridge: GeminiNanoBridge,
    private val clock: Clock,
    private val versionName: String,
    private val isDebug: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(now = Instant.now(clock), versionName = versionName, isDebug = isDebug),
    )
    val uiState: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var csvSource: File? = null
    private val json = Json { prettyPrint = true }

    init {
        viewModelScope.launch {
            combine(
                settings.observeBaseCurrency(),
                settings.observeTheme(),
                settings.observeDynamicColor(),
                settings.observeLockEnabled(),
                settings.observeSilentAutomationSaves(),
            ) { base, theme, dynamic, lock, silent ->
                _state.update {
                    it.copy(
                        baseCurrency = base,
                        theme = theme,
                        dynamicColor = dynamic,
                        lockEnabled = lock,
                        silentAutomation = silent,
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            combine(
                settings.observeNotificationCaptureEnabled(),
                settings.observeNotificationAllowlist(),
                settingsImpl.observeRemindersEnabled(),
                settingsImpl.observeDefaultRemindDays(),
            ) { capture, allow, remindOn, remindDays ->
                _state.update {
                    it.copy(
                        captureEnabled = capture,
                        allowlist = allow,
                        allowlistNone = CaptureAllowlistNone.isNone(allow),
                        remindersEnabled = remindOn,
                        defaultRemindDays = remindDays,
                        captureGranted = captureBridge.isGranted(),
                        bankingApps = captureBridge.installedBankingApps(),
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            combine(
                settingsImpl.observeAutoBackupFolderUri(),
                settingsImpl.observeAutoBackupFrequencyDays(),
                settings.observeAutoBackupLastSuccessAt(),
                settings.observeAutoBackupLastFailureAt(),
                settings.observeAutoBackupLastFailureReason(),
            ) { uri, freq, ok, fail, reason ->
                _state.update {
                    it.copy(
                        autoBackupUri = uri,
                        autoBackupFreqDays = freq,
                        autoBackupLastSuccess = ok,
                        autoBackupLastFailure = fail,
                        autoBackupLastFailureReason = reason,
                    )
                }
            }.collect { }
        }
        viewModelScope.launch {
            combine(
                accounts.observeAll(includeArchived = true),
                categories.observeAll(includeArchived = true),
            ) { a, c ->
                _state.update { it.copy(accounts = a, categories = c) }
            }.collect { }
        }
        viewModelScope.launch { refreshNano() }
        viewModelScope.launch { refreshPasswordPresence() }
    }

    fun open(section: SettingsSection) = _state.update { it.copy(section = section) }
    /** Back from a sub-page goes to its parent: CSV import and exchange rates return to Data. */
    fun backToRoot() = _state.update {
        it.copy(section = it.section.parent ?: SettingsSection.ROOT, csvPreview = null)
    }

    fun setBaseCurrency(code: String) = viewModelScope.launch {
        val parsed = CurrencyCode.parse(code) ?: return@launch
        settings.setBaseCurrency(parsed)
    }

    fun setTheme(theme: String) = viewModelScope.launch { settings.setTheme(theme) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settings.setDynamicColor(enabled) }
    fun setLock(enabled: Boolean) = viewModelScope.launch { settings.setLockEnabled(enabled) }
    fun setReminders(enabled: Boolean) = viewModelScope.launch { settingsImpl.setRemindersEnabled(enabled) }
    fun setDefaultRemindDays(days: Int) = viewModelScope.launch { settingsImpl.setDefaultRemindDays(days) }
    fun setSilentAutomation(enabled: Boolean) = viewModelScope.launch {
        settings.setSilentAutomationSaves(enabled)
    }

    fun setCaptureEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setNotificationCaptureEnabled(enabled)
    }

    fun openCaptureGrant() = captureBridge.openGrantSettings()

    fun refreshCaptureGranted() {
        _state.update {
            it.copy(
                captureGranted = captureBridge.isGranted(),
                bankingApps = captureBridge.installedBankingApps(),
            )
        }
    }

    fun setAllowlistNone() = viewModelScope.launch {
        settings.setNotificationAllowlist(CaptureAllowlistNone.none())
    }

    fun setAllowlistDefault() = viewModelScope.launch {
        settings.setNotificationAllowlist(emptySet())
    }

    fun toggleBankingApp(packageName: String, enabled: Boolean) = viewModelScope.launch {
        val raw = settings.observeNotificationAllowlist().first()
        val current = raw.filterNot { it == CaptureAllowlistNone.SENTINEL }.toMutableSet()
        if (raw.isEmpty()) {
            current.addAll(captureBridge.installedBankingApps().map { it.packageName })
        }
        if (enabled) current.add(packageName) else current.remove(packageName)
        settings.setNotificationAllowlist(
            if (current.isEmpty()) CaptureAllowlistNone.none() else current,
        )
    }

    fun refreshNano() = viewModelScope.launch {
        _state.update { it.copy(nanoStatus = nanoBridge.status()) }
    }

    fun downloadNano() = viewModelScope.launch {
        _state.update { it.copy(nanoStatus = NanoStatusUi.DOWNLOADING) }
        runCatchingCancellable { nanoBridge.download() }
        refreshNano()
    }

    /**
     * Suspends until the encrypted file is on disk. The screen may copy it to the user's Uri only
     * after this returns true; returning at launch time handed it a half-written file.
     */
    suspend fun exportBackup(
        destination: File,
        password: CharArray,
        deliver: suspend (File) -> Boolean = { true },
    ): Boolean {
        // "Exported" only once the file is in the place the user picked: a failed copy to the
        // chosen document must not be reported as a success over a 0-byte file.
        val result = runCatchingCancellable { backup.export(destination, password) }
            .mapCatching { check(deliver(destination)) { "Could not write the export" } }
        _state.update {
            it.copy(
                snackbar = if (result.isSuccess) {
                    SettingsSnackbarMessage.BACKUP_EXPORTED
                } else {
                    SettingsSnackbarMessage.BACKUP_FAILED
                },
            )
        }
        return result.isSuccess
    }

    fun requestRestoreConfirm() = _state.update { it.copy(confirmRestore = true) }
    fun dismissRestoreConfirm() = _state.update { it.copy(confirmRestore = false) }

    fun restoreBackup(source: File, password: CharArray) = viewModelScope.launch {
        _state.update { it.copy(confirmRestore = false) }
        runCatchingCancellable { backup.import(source, password) }
            .onSuccess { _state.update { it.copy(snackbar = SettingsSnackbarMessage.BACKUP_RESTORED) } }
            .onFailure { _state.update { it.copy(snackbar = SettingsSnackbarMessage.BACKUP_FAILED) } }
    }

    fun scheduleAutoBackup(folderUri: String, password: CharArray, freqDays: Int) = viewModelScope.launch {
        settingsImpl.setAutoBackupFrequencyDays(freqDays)
        backup.scheduleAutoBackup(folderUri, password)
        refreshPasswordPresence()
        _state.update { it.copy(snackbar = SettingsSnackbarMessage.AUTO_BACKUP_ON) }
    }

    /**
     * Changes the frequency of an already configured backup. The password lives in the encrypted
     * store, not in this screen: a rotation or a fresh Settings session must not look like "no
     * password" and silently skip the reschedule.
     */
    fun rescheduleAutoBackup(freqDays: Int) = viewModelScope.launch {
        val folder = _state.value.autoBackupUri ?: return@launch
        val stored = backupPasswords.load() ?: return@launch
        settingsImpl.setAutoBackupFrequencyDays(freqDays)
        try {
            backup.scheduleAutoBackup(folder, stored)
            _state.update { it.copy(snackbar = SettingsSnackbarMessage.AUTO_BACKUP_ON) }
        } finally {
            stored.fill('\u0000')
        }
    }

    private suspend fun refreshPasswordPresence() {
        val stored = backupPasswords.load()
        stored?.fill('\u0000')
        _state.update { it.copy(autoBackupPasswordStored = stored != null) }
    }

    fun cancelAutoBackup() = viewModelScope.launch {
        backup.cancelAutoBackup()
        refreshPasswordPresence()
    }

    suspend fun exportCsv(destination: File, deliver: suspend (File) -> Boolean = { true }): Boolean {
        val result = runCatchingCancellable { csv.exportTransactions(destination) }
            .mapCatching { check(deliver(destination)) { "Could not write the export" } }
        _state.update {
            it.copy(
                snackbar = if (result.isSuccess) {
                    SettingsSnackbarMessage.CSV_EXPORTED
                } else {
                    SettingsSnackbarMessage.EXPORT_FAILED
                },
            )
        }
        return result.isSuccess
    }

    suspend fun exportJson(destination: File, deliver: suspend (File) -> Boolean = { true }): Boolean {
        val result = runCatchingCancellable {
            val txs = transactions.observe().first()
            val accountNames = accounts.observeAll(true).first().associate { it.id to it.name }
            val categoryNames = categories.observeAll(true).first().associate { it.id to it.name }
            val array = buildJsonArray {
                for (tx in txs) {
                    add(
                        buildJsonObject {
                            put("date", tx.occurredAt.toLocalDate().toString())
                            put("amount", tx.amount.minor)
                            put("currency", tx.amount.currency.value)
                            put("kind", tx.kind.name.lowercase())
                            put("merchant", tx.merchant.orEmpty())
                            put("category", tx.categoryId?.let { categoryNames[it] }.orEmpty())
                            put("account", accountNames[tx.accountId].orEmpty())
                            put("note", tx.note.orEmpty())
                        },
                    )
                }
            }
            destination.writeText(
                json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), array),
            )
        }.mapCatching { check(deliver(destination)) { "Could not write the export" } }
        _state.update {
            it.copy(
                snackbar = if (result.isSuccess) {
                    SettingsSnackbarMessage.JSON_EXPORTED
                } else {
                    SettingsSnackbarMessage.EXPORT_FAILED
                },
            )
        }
        return result.isSuccess
    }

    fun loadCsvForMapping(source: File, headers: List<String>) {
        csvSource = source
        val defaultMapping = buildMap {
            for (col in listOf(
                CsvColumns.DATE, CsvColumns.AMOUNT, CsvColumns.CURRENCY, CsvColumns.MERCHANT,
                CsvColumns.CATEGORY, CsvColumns.NOTE, CsvColumns.ACCOUNT, CsvColumns.KIND, CsvColumns.TAGS,
            )) {
                val match = headers.firstOrNull { it.equals(col, ignoreCase = true) }
                if (match != null) put(col, match)
            }
        }
        _state.update {
            it.copy(
                section = SettingsSection.CSV_IMPORT,
                csvHeaders = headers,
                csvMapping = defaultMapping,
                csvPreview = null,
            )
        }
    }

    fun setCsvMapping(column: String, header: String?) = _state.update { state ->
        val map = state.csvMapping.toMutableMap()
        if (header.isNullOrBlank()) map.remove(column) else map[column] = header
        state.copy(csvMapping = map)
    }

    fun previewCsv() = viewModelScope.launch {
        val source = csvSource ?: return@launch
        // A file the importer cannot read (no account yet, broken rows) says so instead of
        // crashing the app.
        runCatchingCancellable { csv.previewImport(source, _state.value.csvMapping, CsvImportOptions()) }
            .onSuccess { preview -> _state.update { it.copy(csvPreview = preview) } }
            .onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(snackbar = SettingsSnackbarMessage.CSV_FAILED) }
            }
    }

    fun importCsv() = viewModelScope.launch {
        val source = csvSource ?: return@launch
        val result = runCatchingCancellable { csv.importTransactions(source, _state.value.csvMapping, CsvImportOptions()) }
        result.exceptionOrNull()?.let { e ->
            if (e is kotlinx.coroutines.CancellationException) throw e
            _state.update { it.copy(snackbar = SettingsSnackbarMessage.CSV_FAILED) }
            return@launch
        }
        csvSource = null
        _state.update {
            it.copy(
                snackbar = SettingsSnackbarMessage.CSV_IMPORTED,
                csvPreview = null,
                section = SettingsSection.DATA,
            )
        }
    }

    fun setManualRate(from: String, to: String, rate: String) = viewModelScope.launch {
        val fromCode = CurrencyCode.parse(from)
        val toCode = CurrencyCode.parse(to)
        // The keyboard follows the locale: "1,08" on an Italian phone is 1.08, not a parse error.
        // Normalise to a plain dot-decimal before the repository reads it.
        val value = MoneyParser.parseDecimal(rate)
        if (fromCode == null || toCode == null || value == null || value.signum() <= 0) {
            _state.update { it.copy(snackbar = SettingsSnackbarMessage.RATE_INVALID) }
            return@launch
        }
        val normalized = value.stripTrailingZeros().toPlainString()
        // Defense in depth: the repository also rejects empty/zero, but a failed write must not crash.
        runCatchingCancellable { exchangeRates.setManualRate(fromCode, toCode, normalized, LocalDate.now(clock)) }
            .onSuccess { _state.update { it.copy(snackbar = SettingsSnackbarMessage.RATE_SAVED) } }
            .onFailure { _state.update { it.copy(snackbar = SettingsSnackbarMessage.RATE_INVALID) } }
    }

    fun upsertAccount(account: Account) = viewModelScope.launch { accounts.upsert(account) }
    fun archiveAccount(id: Long, archived: Boolean) = viewModelScope.launch {
        accounts.archive(id, archived)
    }

    fun upsertCategory(category: Category) = viewModelScope.launch { categories.upsert(category) }
    fun archiveCategory(id: Long, archived: Boolean) = viewModelScope.launch {
        categories.archive(id, archived)
    }

    fun reorderAccounts(ordered: List<Account>) = viewModelScope.launch {
        ordered.forEachIndexed { index, account ->
            accounts.upsert(account.copy(sortOrder = index))
        }
    }

    fun clearSnackbar() = _state.update { it.copy(snackbar = null) }
}
