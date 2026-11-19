package dev.antomanc.tameki

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.antomanc.tameki.automation.AutomationIntents
import dev.antomanc.tameki.automation.AutomationNotifier
import dev.antomanc.tameki.automation.IntentAction
import dev.antomanc.tameki.automation.IntentRouter
import dev.antomanc.tameki.automation.SilentSavePolicy
import dev.antomanc.tameki.core.model.CurrencyCode
import dev.antomanc.tameki.core.model.entity.CategoryMatcher
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.lock.AppLockEvent
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.RecurringRepository
import dev.antomanc.tameki.core.model.repository.SettingsRepository
import dev.antomanc.tameki.core.model.repository.TransactionRepository
import dev.antomanc.tameki.core.ui.navigation.ActivityRoute
import dev.antomanc.tameki.core.ui.navigation.AiBridgeRoute
import dev.antomanc.tameki.core.ui.navigation.EditRecurringRoute
import dev.antomanc.tameki.core.ui.navigation.HomeRoute
import dev.antomanc.tameki.core.ui.navigation.InsightsRoute
import dev.antomanc.tameki.core.ui.navigation.NewRecurringRoute
import dev.antomanc.tameki.core.ui.navigation.PasteFromAiRoute
import dev.antomanc.tameki.core.ui.navigation.RecurringDetailRoute
import dev.antomanc.tameki.core.ui.navigation.SettingsRoute
import dev.antomanc.tameki.core.ui.navigation.SubscriptionsReviewRoute
import dev.antomanc.tameki.core.ui.navigation.SubscriptionsRoute
import dev.antomanc.tameki.core.ui.components.OneShotSnackbar
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.rememberMotionEnabled
import dev.antomanc.tameki.feature.activity.ActivityRoute as ActivityFeatureRoute
import dev.antomanc.tameki.feature.activity.QuickAddSheet
import dev.antomanc.tameki.feature.activity.QuickAddViewModel
import dev.antomanc.tameki.feature.aibridge.AibridgeScreen
import dev.antomanc.tameki.feature.aibridge.PasteResultRoute
import dev.antomanc.tameki.feature.home.HomeRoute as HomeFeatureRoute
import dev.antomanc.tameki.feature.insights.InsightsRoute as InsightsFeatureRoute
import dev.antomanc.tameki.feature.settings.SettingsScreen
import dev.antomanc.tameki.feature.subscriptions.RecurringEditRoute
import dev.antomanc.tameki.feature.subscriptions.SubscriptionDetailRoute
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsReviewRoute as SubscriptionsReviewFeature
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsRoute as SubscriptionsFeatureRoute
import dev.antomanc.tameki.onboarding.OnboardingScreen
import dev.antomanc.tameki.onboarding.applyOnboardingAccounts
import dev.antomanc.tameki.scan.api.DocumentScanLauncher
import dev.antomanc.tameki.scan.api.DocumentScanOutcome
import dev.antomanc.tameki.scan.api.ReceiptScanNonImageEffect
import dev.antomanc.tameki.scan.api.toNonImageEffect
import java.io.File
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class MainActivity : FragmentActivity() {
    private val settings: SettingsRepository by inject()
    private val transactions: TransactionRepository by inject()
    private val accounts: AccountRepository by inject()
    private val categories: CategoryRepository by inject()
    private val documentScanLauncher: DocumentScanLauncher by inject()
    private val clock: Clock by inject()

    // Compose state: a new intent arriving via onNewIntent must recompose the router.
    private var pendingIntent by mutableStateOf<Intent?>(null)
    private var lockAvailable by mutableStateOf(true)

    // Read by the splash screen: it stays up until the theme, onboarding and lock settings are
    // known, so no frame of the wrong theme, of the journal before onboarding, or of the journal
    // before the lock cover ever shows.
    @Volatile private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !contentReady || BiometricLock.state.value.loading }
        // Only a fresh launch carries an entry request. A restore after process death or a relaunch
        // from Recents re-delivers the original intent, and replaying it would save a deep-linked
        // transaction twice or re-apply a shared image.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) pendingIntent = intent
        lifecycleScope.launch {
            settings.observeLockEnabled().collect { enabled ->
                BiometricLock.dispatch(AppLockEvent.SettingLoaded(enabled))
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    BiometricLock.promptIfNeeded(this@MainActivity)
                }
            }
        }
        setContent {
            val theme by settings.observeTheme().collectAsStateWithLifecycle(initialValue = null)
            val dynamic by settings.observeDynamicColor().collectAsStateWithLifecycle(initialValue = true)
            val lock by BiometricLock.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (theme) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            // Status and navigation bar icons follow the app's theme, not only the system's: a
            // Dark setting on a light phone must not leave dark icons on a dark bar.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }
            // The journal is financial data: keep it out of the recents thumbnail and screenshots
            // for as long as the app lock is on, not only while the cover is showing.
            DisposableEffect(lock.enabled) {
                if (lock.enabled == true) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose { }
            }
            TamekiTheme(darkTheme = dark, dynamicColor = dynamic) {
                Box(Modifier.fillMaxSize()) {
                    // The app stays composed under the cover: its back stack, sheets and pending
                    // activity results (a receipt scan that outlived the grace period) survive a
                    // lock, and the cover hides it from sight and from TalkBack.
                    if (!lock.loading && theme != null) {
                        TamekiRoot(
                            launchIntent = pendingIntent,
                            onConsumeIntent = { pendingIntent = null },
                            covered = lock.cover,
                            onReady = { contentReady = true },
                            settings = settings,
                            transactions = transactions,
                            accounts = accounts,
                            categories = categories,
                            documentScanLauncher = documentScanLauncher,
                            clock = clock,
                            activity = this@MainActivity,
                            modifier = if (lock.cover) Modifier.clearAndSetSemantics { } else Modifier,
                        )
                    }
                    AnimatedVisibility(
                        visible = lock.cover,
                        enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                        exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    ) {
                        LockScreen(
                            lockAvailable = lockAvailable,
                            onUnlock = {
                                BiometricLock.dispatch(AppLockEvent.UnlockRequested)
                                BiometricLock.promptIfNeeded(this@MainActivity)
                            },
                            onOpenSecuritySettings = {
                                runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }

    override fun onPause() {
        super.onPause()
        BiometricLock.dispatch(AppLockEvent.Paused(clock.instant()))
    }

    override fun onResume() {
        super.onResume()
        // Checked on every resume: the user may have removed or added a screen lock meanwhile.
        lockAvailable = BiometricLock.canAuthenticate(this)
        BiometricLock.dispatch(AppLockEvent.Resumed(clock.instant()))
        BiometricLock.promptIfNeeded(this)
    }
}

@Composable
private fun TamekiRoot(
    launchIntent: Intent?,
    onConsumeIntent: () -> Unit,
    covered: Boolean,
    onReady: () -> Unit,
    settings: SettingsRepository,
    transactions: TransactionRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    documentScanLauncher: DocumentScanLauncher,
    clock: Clock,
    activity: FragmentActivity,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val quickAdd: QuickAddViewModel = koinViewModel()
    val recurrings: RecurringRepository = koinInject()
    val quickAddState by quickAdd.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var fabMenu by rememberSaveable { mutableStateOf(false) }
    // The row removed on the detail screen, kept until its snackbar resolves so Undo can restore
    // it with its id and price history intact. The token makes two deletes two snackbars.
    var recurringUndo by remember { mutableStateOf<Pair<Long, Recurring>?>(null) }
    // When true, a successful scan fills empty fields only (in-sheet chip); FAB/deep-link prefills fully.
    var scanFillEmptyOnly by rememberSaveable { mutableStateOf(false) }
    // Null until read: neither onboarding nor the journal may flash before the answer is known.
    val onboardingDone by settings.observeOnboardingDone().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val linkFailed = stringResource(R.string.app_open_link_failed)

    if (onboardingDone != null) SideEffect { onReady() }

    /** Copies a picked or shared image into the cache off the main thread; null when unreadable. */
    suspend fun copyToCache(uri: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(context.cacheDir, "${prefix}_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            tmp.absolutePath
        }.getOrNull()
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = copyToCache(uri, "picked")
            if (path != null) {
                quickAdd.applyCapturedReceipt(path, fillEmptyOnly = scanFillEmptyOnly)
            } else {
                quickAdd.reportReceiptFailed()
            }
        }
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val outcome = documentScanLauncher.interpret(
            resultCode = result.resultCode,
            data = result.data,
            cacheDirPath = context.cacheDir.absolutePath,
        )
        when (outcome.toNonImageEffect()) {
            ReceiptScanNonImageEffect.Ignore -> Unit
            ReceiptScanNonImageEffect.FallBackToPicker -> photoPicker.launch("image/*")
            null -> {
                val success = outcome as DocumentScanOutcome.Success
                quickAdd.applyCapturedReceipt(success.imagePath, fillEmptyOnly = scanFillEmptyOnly)
            }
        }
    }

    fun launchReceiptScan(fillEmptyOnly: Boolean) {
        scanFillEmptyOnly = fillEmptyOnly
        documentScanLauncher.prepare(
            activity = activity,
            onReady = { sender ->
                documentScannerLauncher.launch(
                    IntentSenderRequest.Builder(sender as IntentSender).build(),
                )
            },
            onUnavailable = {
                photoPicker.launch("image/*")
            },
        )
    }

    fun openUrl(url: String) {
        // Cancel links are typed by the user: "netflix.com/cancel" has no scheme and no app
        // handles it. Add https, and say so instead of crashing when nothing can open it.
        val trimmed = url.trim()
        val withScheme = if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(withScheme)))
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHost.showSnackbar(linkFailed) }
        } catch (_: SecurityException) {
            scope.launch { snackbarHost.showSnackbar(linkFailed) }
        }
    }

    fun openPasteFromAi(text: String? = null) {
        navController.navigate(PasteFromAiRoute)
        // Shared text travels with the destination's own saved state, so it is gone as soon as
        // that screen is popped, however it is left (toolbar, system back, predictive back).
        if (text != null) navController.currentBackStackEntry?.savedStateHandle?.set(SHARE_TEXT_KEY, text)
    }

    LaunchedEffect(launchIntent, onboardingDone) {
        val intent = launchIntent ?: return@LaunchedEffect
        // Entry requests wait for onboarding: a deep link must not land behind the first-run flow.
        if (onboardingDone != true) return@LaunchedEffect
        onConsumeIntent()
        when (val action = IntentRouter.route(intent)) {
            IntentAction.None -> Unit
            IntentAction.OpenQuickAdd -> quickAdd.openNew()
            IntentAction.OpenPasteAi -> openPasteFromAi()
            IntentAction.OpenScan -> launchReceiptScan(fillEmptyOnly = false)
            IntentAction.OpenInbox -> navController.navigateToTab(ActivityRoute(openInbox = true), restore = false)
            is IntentAction.ShareText -> openPasteFromAi(action.text)
            is IntentAction.ShareImage -> {
                val uri = action.uri ?: return@LaunchedEffect
                val path = copyToCache(uri, "share")
                if (path != null) {
                    quickAdd.applyCapturedReceipt(path, fillEmptyOnly = false)
                } else {
                    quickAdd.reportReceiptFailed()
                }
            }
            IntentAction.AddTransaction -> {
                val base = settings.observeBaseCurrency().first()
                val parsed = AutomationIntents.parseAddIntent(intent, base)
                parsed.onSuccess { result ->
                    val silentAllowed = settings.observeSilentAutomationSaves().first()
                    val amount = result.draft.amount
                    if (SilentSavePolicy.shouldSilentSave(silentAllowed, result.save) && amount != null) {
                        val accs = accounts.observeAll().first()
                        val cats = categories.observeAll().first()
                        val memory = categories.observeMerchantCategoryMemory().first()
                        val hint = result.draft.accountHint
                        // A hint that names no account opens the sheet: a silent save must never
                        // guess which account the money came from.
                        val accountId = if (hint != null) {
                            accs.firstOrNull { it.name.equals(hint, ignoreCase = true) }?.id
                        } else {
                            accs.firstOrNull()?.id
                        }
                        if (accountId != null) {
                            val categoryId = CategoryMatcher.match(
                                result.draft.categoryHint,
                                cats,
                                memory,
                                result.draft.merchant,
                            )
                            val id = transactions.upsert(
                                Transaction(
                                    kind = result.draft.kind,
                                    amount = amount,
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    merchant = result.draft.merchant,
                                    note = result.draft.note,
                                    tags = result.tags,
                                    occurredAt = result.draft.occurredAt
                                        ?: LocalDateTime.now(clock),
                                    source = TransactionSource.AUTOMATION,
                                ),
                            )
                            AutomationNotifier.showAdded(context, id)
                        } else {
                            quickAdd.openFromDraft(result.draft)
                        }
                    } else {
                        quickAdd.openFromDraft(result.draft)
                    }
                }
            }
        }
    }

    when (onboardingDone) {
        null -> return
        false -> {
            val base by settings.observeBaseCurrency().collectAsStateWithLifecycle(CurrencyCode("EUR"))
            val openingBalanceNote = stringResource(R.string.app_opening_balance)
            // One finish only: a double tap on Finish must not book the opening balances twice.
            var finishing by remember { mutableStateOf(false) }
            OnboardingScreen(
                initialCurrency = base.value,
                onFinish = { currency, accountDrafts, importCsv ->
                    if (!finishing) {
                        finishing = true
                        scope.launch {
                            val code = CurrencyCode.parse(currency) ?: CurrencyCode("EUR")
                            settings.setBaseCurrency(code)
                            applyOnboardingAccounts(
                                accounts = accounts,
                                transactions = transactions,
                                drafts = accountDrafts,
                                currency = code,
                                clock = clock,
                                openingBalanceNote = openingBalanceNote,
                            )
                            settings.setOnboardingDone(true)
                            if (importCsv) navController.navigate(SettingsRoute)
                        }
                    }
                },
                onSkip = { currency ->
                    scope.launch {
                        // Skipping later steps keeps the currency the user already picked.
                        CurrencyCode.parse(currency)?.let { settings.setBaseCurrency(it) }
                        settings.setOnboardingDone(true)
                    }
                },
                onRestoreBackup = {
                    scope.launch {
                        settings.setOnboardingDone(true)
                        navController.navigate(SettingsRoute)
                    }
                },
            )
            return
        }
        true -> Unit
    }

    val savedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_saved)
    val splitsScaledLabel = stringResource(
        dev.antomanc.tameki.feature.activity.R.string.activity_saved_splits_scaled,
    )
    val splitsDroppedLabel = stringResource(
        dev.antomanc.tameki.feature.activity.R.string.activity_saved_splits_dropped,
    )
    val deletedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_deleted)
    val undoLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_undo)
    val receiptFailedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_receipt_read_failed)
    QuickAddNoticeEffect(
        notice = quickAddState.notice,
        host = snackbarHost,
        savedLabel = savedLabel,
        splitsScaledLabel = splitsScaledLabel,
        splitsDroppedLabel = splitsDroppedLabel,
        deletedLabel = deletedLabel,
        receiptFailedLabel = receiptFailedLabel,
        undoLabel = undoLabel,
        onUndo = quickAdd::undo,
        onResolved = quickAdd::acknowledgeNotice,
    )

    // Recurring delete is immediate too: the detail screen pops, the row comes back only if the
    // user taps Undo. Upsert keeps the row's id and price history, so reminders and reports follow.
    OneShotSnackbar(
        token = recurringUndo?.first,
        host = snackbarHost,
        message = deletedLabel,
        actionLabel = undoLabel,
        onAction = { token ->
            val deleted = recurringUndo?.takeIf { it.first == token }?.second
            if (deleted != null) scope.launch { recurrings.upsert(deleted) }
        },
        onResolved = { token -> if (recurringUndo?.first == token) recurringUndo = null },
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentTab = backStack?.destination?.appTab()
    val showChrome = currentTab != null
    // The tab the user is "in": a pushed screen keeps its tab, so opening and closing a detail
    // screen is not a tab change and does not dismiss the snackbar it just raised.
    var lastTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    if (currentTab != null && currentTab != lastTab) lastTab = currentTab
    // Saved · Undo must not linger on another tab after the user leaves the screen that triggered it.
    LaunchedEffect(lastTab) {
        snackbarHost.currentSnackbarData?.dismiss()
        fabMenu = false
    }

    val motionEnabled = rememberMotionEnabled()
    val navMotion = remember(motionEnabled) { NavMotion(motionEnabled) }
    // Unfolded (or any window 600 dp and wider): a side rail instead of the bottom bar, and the
    // content held to a readable column instead of lines stretched across the whole screen.
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    Row(modifier.fillMaxSize()) {
    AnimatedVisibility(
        visible = wide && showChrome,
        enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) +
            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()) +
            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        AppNavigationRail(
            selected = currentTab,
            onSelect = { tab -> navController.navigateToTab(tab.route) },
        )
    }
    Scaffold(
        modifier = Modifier.weight(1f),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            AnimatedVisibility(
                visible = showChrome && !wide,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                AppNavigationBar(
                    selected = currentTab,
                    onSelect = { tab -> navController.navigateToTab(tab.route) },
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showChrome,
                enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                AddFabMenu(
                    onRecurringTab = currentTab == AppTab.Recurring,
                    expanded = fabMenu,
                    onExpandedChange = { fabMenu = it },
                    onAction = { action ->
                        when (action) {
                            AddAction.QuickAdd -> quickAdd.openNew()
                            AddAction.NewRecurring -> navController.navigate(NewRecurringRoute)
                            AddAction.ScanReceipt -> launchReceiptScan(fillEmptyOnly = false)
                            AddAction.PasteAi -> openPasteFromAi()
                        }
                    },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp),
            enterTransition = { navMotion.enter(this) },
            exitTransition = { navMotion.exit(this) },
            popEnterTransition = { navMotion.popEnter(this) },
            popExitTransition = { navMotion.popExit(this) },
        ) {
            composable<HomeRoute> {
                HomeFeatureRoute(
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenActivity = { navController.navigateToTab(ActivityRoute()) },
                    onOpenInbox = { navController.navigateToTab(ActivityRoute(openInbox = true), restore = false) },
                    onOpenSubscriptions = { navController.navigateToTab(SubscriptionsRoute) },
                    onOpenBudget = { navController.navigateToTab(InsightsRoute(openBudget = true), restore = false) },
                    onOpenTransaction = { id -> quickAdd.openEdit(id) },
                    onOpenSubscription = { id -> navController.navigate(RecurringDetailRoute(id)) },
                    onAddTransaction = { quickAdd.openNew() },
                )
            }
            composable<ActivityRoute> { entry ->
                val route = entry.toRoute<ActivityRoute>()
                ActivityFeatureRoute(
                    snackbarHostState = snackbarHost,
                    openInbox = route.openInbox,
                    categoryId = route.categoryId,
                    onAddMovement = { quickAdd.openNew() },
                    onEditTransaction = { id -> quickAdd.openEdit(id) },
                    onAcceptInbox = { item -> quickAdd.openFromDraft(item.draft, item.id) },
                )
            }
            composable<SubscriptionsRoute> {
                SubscriptionsFeatureRoute(
                    onOpenSubscription = { id -> navController.navigate(RecurringDetailRoute(id)) },
                    onAddRecurring = { navController.navigate(EditRecurringRoute()) },
                    onReviewSubscriptions = { navController.navigate(SubscriptionsReviewRoute) },
                )
            }
            composable<RecurringDetailRoute> { entry ->
                val route = entry.toRoute<RecurringDetailRoute>()
                SubscriptionDetailRoute(
                    id = route.id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditRecurringRoute(route.id)) },
                    onOpenUrl = ::openUrl,
                    onDeleted = { deleted ->
                        recurringUndo = (recurringUndo?.first ?: 0L) + 1 to deleted
                    },
                )
            }
            composable<EditRecurringRoute> { entry ->
                val route = entry.toRoute<EditRecurringRoute>()
                RecurringEditRoute(
                    id = route.id,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<SubscriptionsReviewRoute> {
                SubscriptionsReviewFeature(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOpenUrl = ::openUrl,
                )
            }
            composable<InsightsRoute> { entry ->
                val route = entry.toRoute<InsightsRoute>()
                InsightsFeatureRoute(
                    snackbarHostState = snackbarHost,
                    openBudget = route.openBudget,
                    onOpenSubscription = { id -> navController.navigate(RecurringDetailRoute(id)) },
                    onReviewSubscriptions = { navController.navigate(SubscriptionsReviewRoute) },
                    onOpenCategory = { categoryId ->
                        navController.navigateToTab(ActivityRoute(categoryId = categoryId), restore = false)
                    },
                    onOpenActivity = { navController.navigateToTab(ActivityRoute()) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onOpenAutomationDocs = {
                        openUrl("https://github.com/antomanc/tameki/blob/main/docs/automation.md")
                    },
                    onOpenDebugGallery = {
                        if (BuildConfig.DEBUG) {
                            activity.startActivity(
                                Intent().setClassName(
                                    activity,
                                    "dev.antomanc.tameki.GalleryActivity",
                                ),
                            )
                        }
                    },
                    onOpenAi = { navController.navigate(AiBridgeRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<AiBridgeRoute> {
                AibridgeScreen(
                    onOpenPaste = { openPasteFromAi() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<PasteFromAiRoute> { entry ->
                val shared by entry.savedStateHandle.getStateFlow<String?>(SHARE_TEXT_KEY, null)
                    .collectAsStateWithLifecycle()
                PasteResultRoute(
                    initialText = shared,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NewRecurringRoute> {
                RecurringEditRoute(
                    id = null,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
      }
    }
    }

    // The sheet is its own window and would draw over the lock cover: keep its state in the view
    // model and only show it while the app is uncovered.
    if (!covered) {
        QuickAddSheet(
            state = quickAddState,
            onDismiss = quickAdd::dismiss,
            onSelectKind = quickAdd::selectKind,
            onDigit = quickAdd::digit,
            onDecimal = quickAdd::decimal,
            onBackspace = quickAdd::backspace,
            onClearAmount = quickAdd::clearAmount,
            onSelectCategory = quickAdd::selectCategory,
            onSelectAccount = quickAdd::setAccount,
            onSelectToAccount = quickAdd::setToAccount,
            onSetDate = quickAdd::setDate,
            onSetNote = quickAdd::setNote,
            onToggleNote = quickAdd::toggleNoteEditor,
            onScanReceipt = { launchReceiptScan(fillEmptyOnly = true) },
            onClearReceipt = quickAdd::clearReceipt,
            onSave = quickAdd::save,
            onDelete = quickAdd::deleteCurrent,
        )
    }
}

private const val SHARE_TEXT_KEY = "share_text"
