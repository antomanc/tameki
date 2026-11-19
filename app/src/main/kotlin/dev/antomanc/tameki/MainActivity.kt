package dev.antomanc.tameki

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import dev.antomanc.tameki.core.model.entity.Account
import dev.antomanc.tameki.core.model.entity.AccountType
import dev.antomanc.tameki.core.model.entity.CategoryMatcher
import dev.antomanc.tameki.core.model.entity.DefaultAccounts
import dev.antomanc.tameki.core.model.entity.Recurring
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
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
import dev.antomanc.tameki.core.ui.navigation.NavIcons
import dev.antomanc.tameki.core.ui.navigation.NewRecurringRoute
import dev.antomanc.tameki.core.ui.navigation.PasteFromAiRoute
import dev.antomanc.tameki.core.ui.navigation.RecurringDetailRoute
import dev.antomanc.tameki.core.ui.navigation.ScanReceiptRoute
import dev.antomanc.tameki.core.ui.navigation.SettingsRoute
import dev.antomanc.tameki.core.ui.navigation.SubscriptionsReviewRoute
import dev.antomanc.tameki.core.ui.navigation.SubscriptionsRoute
import dev.antomanc.tameki.core.ui.theme.TamekiTheme
import dev.antomanc.tameki.core.ui.theme.motion
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
import dev.antomanc.tameki.onboarding.OnboardingAccountDraft
import dev.antomanc.tameki.onboarding.OnboardingScreen
import dev.antomanc.tameki.scan.api.DocumentScanLauncher
import dev.antomanc.tameki.scan.api.DocumentScanOutcome
import dev.antomanc.tameki.scan.api.toNonImageEffect
import dev.antomanc.tameki.scan.api.ReceiptScanNonImageEffect
import java.io.File
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private var locked by mutableStateOf(BiometricLock.isLocked())
    // Nothing but the lock screen may draw until the setting has been read: a cold start with the
    // lock on must not show the journal for a frame before the prompt appears.
    private var lockReady by mutableStateOf(false)
    private var prompting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntent = intent
        setContent {
            val theme by settings.observeTheme().collectAsStateWithLifecycle(initialValue = "system")
            val dynamic by settings.observeDynamicColor().collectAsStateWithLifecycle(initialValue = true)
            val dark = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            TamekiTheme(darkTheme = dark, dynamicColor = dynamic) {
                if (!lockReady) {
                    Box(Modifier.fillMaxSize())
                } else if (locked) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.app_unlock_title))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { requestUnlock() }) {
                            Text(stringResource(R.string.app_unlock_action))
                        }
                    }
                } else {
                    TamekiRoot(
                        launchIntent = pendingIntent,
                        onConsumeIntent = { pendingIntent = null },
                        settings = settings,
                        transactions = transactions,
                        accounts = accounts,
                        categories = categories,
                        documentScanLauncher = documentScanLauncher,
                        clock = clock,
                        activity = this,
                    )
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
        BiometricLock.onPause()
    }

    override fun onResume() {
        super.onResume()
        // Cover the journal before the setting round-trip finishes when the grace period expired.
        if (BiometricLock.shouldLock()) {
            locked = true
            lockReady = true
        }
        lifecycleScope.launch {
            val enabled = settings.observeLockEnabled().first()
            if (!enabled) {
                locked = false
                BiometricLock.markUnlocked()
                lockReady = true
                return@launch
            }
            // Lock on a cold start (never unlocked in this process), after the 60 s grace period,
            // or while a cancelled prompt asks to stay covered. Rotation keeps the process-level
            // unlock, so the app is not locked again by turning the phone.
            val expired = BiometricLock.shouldLock()
            locked = expired || !BiometricLock.isUnlockedForProcess()
            // Publish the lock state before prompting: the prompt is an overlay, and the screen
            // under it must already be the lock screen rather than the journal.
            lockReady = true
            if (locked) requestUnlock()
        }
    }

    private fun requestUnlock() {
        if (prompting) return
        prompting = true
        BiometricLock.prompt(
            activity = this,
            onUnlocked = {
                prompting = false
                locked = false
            },
            onFailed = {
                prompting = false
                locked = true
            },
        )
    }
}

private data class Destination(
    val route: Any,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/** 0–3 for the four bottom-bar destinations; null for every pushed screen. */
private fun NavDestination.topLevelTabIndex(): Int? = when {
    hasRoute<HomeRoute>() -> 0
    hasRoute<ActivityRoute>() -> 1
    hasRoute<SubscriptionsRoute>() -> 2
    hasRoute<InsightsRoute>() -> 3
    else -> null
}

@Composable
private fun TamekiRoot(
    launchIntent: Intent?,
    onConsumeIntent: () -> Unit,
    settings: SettingsRepository,
    transactions: TransactionRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    documentScanLauncher: DocumentScanLauncher,
    clock: Clock,
    activity: FragmentActivity,
) {
    val navController = rememberNavController()
    val quickAdd: QuickAddViewModel = koinViewModel()
    val recurrings: RecurringRepository = koinInject()
    val quickAddState by quickAdd.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var fabMenu by rememberSaveable { mutableStateOf(false) }
    var openInbox by rememberSaveable { mutableStateOf(false) }
    var sharePasteText by rememberSaveable { mutableStateOf<String?>(null) }
    // The row removed on the detail screen, kept here until the snackbar resolves so Undo can
    // restore it with its id and price history intact.
    var deletedRecurring by remember { mutableStateOf<Recurring?>(null) }
    var recurringDeleteGeneration by remember { mutableStateOf(0L) }
    // When true, a successful scan fills empty fields only (in-sheet chip); FAB/deep-link prefills fully.
    var scanFillEmptyOnly by rememberSaveable { mutableStateOf(false) }
    val onboardingDone by settings.observeOnboardingDone().collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "picked_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: return@rememberLauncherForActivityResult
        quickAdd.applyCapturedReceipt(tmp.absolutePath, fillEmptyOnly = scanFillEmptyOnly)
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
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    LaunchedEffect(launchIntent) {
        val intent = launchIntent ?: return@LaunchedEffect
        onConsumeIntent()
        when (val action = IntentRouter.route(intent)) {
            IntentAction.None -> Unit
            IntentAction.OpenQuickAdd -> quickAdd.openNew()
            IntentAction.OpenPasteAi -> navController.navigate(PasteFromAiRoute)
            IntentAction.OpenScan -> launchReceiptScan(fillEmptyOnly = false)
            IntentAction.OpenInbox -> {
                openInbox = true
                navController.navigate(ActivityRoute(openInbox = true)) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            is IntentAction.ShareText -> {
                sharePasteText = action.text
                navController.navigate(PasteFromAiRoute)
            }
            is IntentAction.ShareImage -> {
                val uri = action.uri ?: return@LaunchedEffect
                val tmp = File(context.cacheDir, "share_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                } ?: return@LaunchedEffect
                quickAdd.applyCapturedReceipt(tmp.absolutePath, fillEmptyOnly = false)
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
                        val accountId = result.draft.accountHint?.let { hint ->
                            accs.firstOrNull { it.name.equals(hint, ignoreCase = true) }?.id
                        } ?: accs.firstOrNull()?.id
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

    if (!onboardingDone) {
        val base by settings.observeBaseCurrency().collectAsStateWithLifecycle(CurrencyCode("EUR"))
        val openingBalanceNote = stringResource(R.string.app_opening_balance)
        OnboardingScreen(
            initialCurrency = base.value,
            onFinish = { currency, accountDrafts, importCsv ->
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
                    if (importCsv) {
                        navController.navigate(SettingsRoute)
                    }
                }
            },
            onSkip = {
                scope.launch { settings.setOnboardingDone(true) }
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

    val savedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_saved)
    val deletedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_deleted)
    val undoLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_undo)
    val receiptFailedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_receipt_read_failed)
    LaunchedEffect(quickAddState.snackbar) {
        when (quickAddState.snackbar) {
            "saved" -> {
                // One snackbar at a time on the shared host: a new save replaces whatever is
                // still up, instead of queueing behind it and losing its own Undo.
                snackbarHost.currentSnackbarData?.dismiss()
                val result = snackbarHost.showSnackbar(
                    message = savedLabel,
                    actionLabel = undoLabel,
                )
                if (result == SnackbarResult.ActionPerformed) quickAdd.undoLastSave()
                quickAdd.clearSnackbar()
            }
            "receipt_read_failed" -> {
                snackbarHost.currentSnackbarData?.dismiss()
                snackbarHost.showSnackbar(message = receiptFailedLabel)
                quickAdd.clearSnackbar()
            }
        }
    }
    // Keyed on a counter, not a sticky "deleted" string: deleting twice must show Undo twice.
    // Acknowledged when the snackbar resolves (or the screen goes away mid-snackbar), so a tab
    // return or rotation cannot replay an Undo for a row that is already gone.
    val deleteGeneration = quickAddState.deleteGeneration
    LaunchedEffect(deleteGeneration) {
        if (deleteGeneration > 0L) {
            // A delete supersedes a "Saved · Undo" still on the shared host: drop its snapshot
            // (clearSnackbar) and its snackbar, so a late Undo tap cannot resurrect a row the
            // user has just removed.
            quickAdd.clearSnackbar()
            snackbarHost.currentSnackbarData?.dismiss()
            try {
                val result = snackbarHost.showSnackbar(
                    message = deletedLabel,
                    actionLabel = undoLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) quickAdd.undoDelete()
            } finally {
                quickAdd.acknowledgeDelete(deleteGeneration)
            }
        }
    }

    // Recurring delete is immediate too: the detail screen pops, the row comes back only if the
    // user taps Undo. Upsert keeps the row's id and price history, so reminders and reports follow.
    LaunchedEffect(recurringDeleteGeneration) {
        val deleted = deletedRecurring ?: return@LaunchedEffect
        if (recurringDeleteGeneration <= 0L) return@LaunchedEffect
        snackbarHost.currentSnackbarData?.dismiss()
        try {
            val result = snackbarHost.showSnackbar(
                message = deletedLabel,
                actionLabel = undoLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) recurrings.upsert(deleted)
        } finally {
            if (deletedRecurring === deleted) deletedRecurring = null
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val routeName = backStack?.destination?.route.orEmpty()
    val isMainTab = routeName.contains("HomeRoute") ||
        routeName.contains("ActivityRoute") ||
        (routeName.contains("SubscriptionsRoute") && !routeName.contains("Review") && !routeName.contains("Detail") && !routeName.contains("Edit")) ||
        (routeName.contains("InsightsRoute"))
    val showChrome = isMainTab &&
        !routeName.contains("SettingsRoute") &&
        !routeName.contains("AiBridgeRoute") &&
        !routeName.contains("PasteFromAiRoute") &&
        !routeName.contains("RecurringDetailRoute") &&
        !routeName.contains("EditRecurringRoute") &&
        !routeName.contains("NewRecurringRoute") &&
        !routeName.contains("SubscriptionsReviewRoute")
    val selectedIndex = when {
        routeName.contains("ActivityRoute") -> 1
        routeName.contains("SubscriptionsRoute") -> 2
        routeName.contains("InsightsRoute") -> 3
        else -> 0
    }
    // Saved · Undo must not linger on another tab after the user leaves the screen that triggered it.
    LaunchedEffect(selectedIndex) {
        snackbarHost.currentSnackbarData?.dismiss()
    }

    val destinations = listOf(
        Destination(HomeRoute, R.string.nav_home, NavIcons.Today, NavIcons.TodaySelected),
        Destination(ActivityRoute(), R.string.nav_activity, NavIcons.Activity, NavIcons.ActivitySelected),
        Destination(SubscriptionsRoute, R.string.nav_subscriptions, NavIcons.Recurring, NavIcons.RecurringSelected),
        Destination(InsightsRoute(), R.string.nav_insights, NavIcons.Save, NavIcons.SaveSelected),
    )
    val navMotion = MaterialTheme.motion.effects
    // Same spring as every other entrance, retargeted to IntOffset for horizontal slides.
    val slideSpec = spring<IntOffset>(
        dampingRatio = MaterialTheme.motion.spatial.dampingRatio,
        stiffness = MaterialTheme.motion.spatial.stiffness,
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    destinations.forEachIndexed { index, destination ->
                        val selected = selectedIndex == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val target = when (index) {
                                    1 -> ActivityRoute(openInbox = false)
                                    3 -> InsightsRoute()
                                    else -> destination.route
                                }
                                navController.navigate(target) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                if (index != 1) openInbox = false
                            },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showChrome) {
                Box {
                    // Click opens the menu: the long-press actions were invisible, so Scan,
                    // Paste from AI and New recurring now have a visible, labelled entry. Quick
                    // add (or New recurring on that tab) is first, so the primary action is still
                    // one tap away. DESIGN.md's "FAB opens quick-add" becomes two taps; the
                    // discoverability win is the deviation, noted in the commit.
                    val onRecurring = selectedIndex == 2
                    AddFab(
                        onClick = { fabMenu = true },
                        onLongClick = { fabMenu = true },
                        label = stringResource(R.string.fab_menu),
                    )
                    DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                        if (onRecurring) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.fab_new_recurring)) },
                                onClick = {
                                    fabMenu = false
                                    navController.navigate(NewRecurringRoute)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_add)) },
                                onClick = {
                                    fabMenu = false
                                    quickAdd.openNew()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_add)) },
                                onClick = {
                                    fabMenu = false
                                    quickAdd.openNew()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.fab_new_recurring)) },
                                onClick = {
                                    fabMenu = false
                                    navController.navigate(NewRecurringRoute)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fab_scan_receipt)) },
                            onClick = {
                                fabMenu = false
                                launchReceiptScan(fillEmptyOnly = false)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fab_paste_ai)) },
                            onClick = {
                                fabMenu = false
                                navController.navigate(PasteFromAiRoute)
                            },
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
            // Tab switches carry direction: going to a lower tab enters from the left. The settle
            // uses the same spatial token as the rest of the app, and every fade is the effects
            // token, so "remove animations" scales all of it together.
            enterTransition = {
                val from = initialState.destination.topLevelTabIndex()
                val to = targetState.destination.topLevelTabIndex()
                val direction = if (from != null && to != null && to < from) -1 else 1
                fadeIn(animationSpec = navMotion) +
                    slideInHorizontally(animationSpec = slideSpec) { width -> direction * width / 20 }
            },
            exitTransition = { fadeOut(animationSpec = navMotion) },
            popEnterTransition = { fadeIn(animationSpec = navMotion) },
            popExitTransition = {
                fadeOut(animationSpec = navMotion) +
                    slideOutHorizontally(animationSpec = slideSpec) { it / 20 }
            },
        ) {
            composable<HomeRoute> {
                HomeFeatureRoute(
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenActivity = {
                        navController.navigate(ActivityRoute()) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenInbox = {
                        openInbox = true
                        navController.navigate(ActivityRoute(openInbox = true)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSubscriptions = {
                        navController.navigate(SubscriptionsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenBudget = {
                        navController.navigate(InsightsRoute(openBudget = true)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenTransaction = { id -> quickAdd.openEdit(id) },
                    onOpenSubscription = { id -> navController.navigate(RecurringDetailRoute(id)) },
                )
            }
            composable<ActivityRoute> { entry ->
                val route = entry.toRoute<ActivityRoute>()
                ActivityFeatureRoute(
                    snackbarHostState = snackbarHost,
                    openInbox = route.openInbox || openInbox,
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
                        deletedRecurring = deleted
                        recurringDeleteGeneration += 1
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
                        navController.navigate(ActivityRoute(categoryId = categoryId)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenActivity = {
                        navController.navigate(ActivityRoute()) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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
                    onOpenPaste = { navController.navigate(PasteFromAiRoute) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ScanReceiptRoute> {
                LaunchedEffect(Unit) {
                    launchReceiptScan(fillEmptyOnly = false)
                    navController.popBackStack()
                }
                Box(Modifier.fillMaxSize())
            }
            composable<PasteFromAiRoute> {
                PasteResultRoute(
                    initialText = sharePasteText,
                    onBack = {
                        sharePasteText = null
                        navController.popBackStack()
                    },
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

    QuickAddSheet(
        state = quickAddState,
        onDismiss = quickAdd::dismiss,
        onSelectKind = quickAdd::selectKind,
        onDigit = quickAdd::digit,
        onDecimal = quickAdd::decimal,
        onBackspace = quickAdd::backspace,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AddFab(onClick: () -> Unit, onLongClick: () -> Unit, label: String) {
    // FloatingActionButton owns its click handler and would swallow taps before a
    // combinedClickable modifier sees them, so the FAB is a Surface with both gestures.
    // primary (not primaryContainer): dark-theme container was ~1.7:1 on background and
    // hid the one primary action. No decorative shadow — DESIGN.md depth is tonal only.
    Surface(
        shape = FloatingActionButtonDefaults.shape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .size(56.dp)
            .clip(FloatingActionButtonDefaults.shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Button),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = label,
            )
        }
    }
}

private suspend fun applyOnboardingAccounts(
    accounts: AccountRepository,
    transactions: TransactionRepository,
    drafts: List<OnboardingAccountDraft>,
    currency: CurrencyCode,
    clock: Clock,
    openingBalanceNote: String,
) {
    val existing = accounts.observeAll(includeArchived = true).first()
    val touched = mutableSetOf<Long>()
    drafts.forEachIndexed { index, draft ->
        val name = draft.name.trim().ifBlank { return@forEachIndexed }
        val seedKey = when (index) {
            0 -> DefaultAccounts.CASH
            1 -> DefaultAccounts.CARD
            else -> null
        }
        val match = when {
            seedKey != null -> existing.firstOrNull { it.seedKey == seedKey }
                ?: existing.getOrNull(index)
            else -> null
        }
        val type = when (seedKey) {
            DefaultAccounts.CASH -> AccountType.CASH
            DefaultAccounts.CARD -> AccountType.CARD
            else -> AccountType.BANK
        }
        val isDefaultLabel = seedKey != null && (
            name.equals("Cash", true) || name.equals("Contanti", true) ||
                name.equals("Card", true) || name.equals("Carta", true)
            )
        val id = accounts.upsert(
            Account(
                id = match?.id ?: 0L,
                name = name,
                currency = currency,
                type = match?.type ?: type,
                sortOrder = match?.sortOrder ?: index,
                seedKey = seedKey ?: match?.seedKey,
                customName = if (isDefaultLabel) null else name,
            ),
        )
        touched += id
        val balance = draft.balance
        if (balance != null && balance.minor > 0L) {
            transactions.upsert(
                Transaction(
                    kind = TransactionKind.INCOME,
                    amount = balance.copy(currency = currency),
                    accountId = id,
                    note = openingBalanceNote,
                    occurredAt = LocalDateTime.now(clock),
                    source = TransactionSource.MANUAL,
                ),
            )
        }
    }
    existing.filter { it.seedKey != null && it.id !in touched }.forEach { account ->
        if (account.currency != currency) {
            accounts.upsert(account.copy(currency = currency))
        }
    }
}
