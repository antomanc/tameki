package dev.antomanc.tameki

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import dev.antomanc.tameki.core.model.entity.Receipt
import dev.antomanc.tameki.core.model.entity.Transaction
import dev.antomanc.tameki.core.model.entity.TransactionKind
import dev.antomanc.tameki.core.model.entity.TransactionSource
import dev.antomanc.tameki.core.model.repository.AccountRepository
import dev.antomanc.tameki.core.model.repository.CategoryRepository
import dev.antomanc.tameki.core.model.repository.ReceiptRepository
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
import dev.antomanc.tameki.scan.api.ReceiptReader
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

class MainActivity : FragmentActivity() {
    private val settings: SettingsRepository by inject()
    private val transactions: TransactionRepository by inject()
    private val accounts: AccountRepository by inject()
    private val categories: CategoryRepository by inject()
    private val receipts: ReceiptRepository by inject()
    private val receiptReader: ReceiptReader by inject()
    private val clock: Clock by inject()

    private var pendingIntent: Intent? = null
    private var locked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntent = intent
        setContent {
            val theme by settings.observeTheme().collectAsStateWithLifecycle(initialValue = "system")
            val dynamic by settings.observeDynamicColor().collectAsStateWithLifecycle(initialValue = false)
            val dark = when (theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            TamekiTheme(darkTheme = dark, dynamicColor = dynamic) {
                if (locked) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.app_unlock_title))
                    }
                } else {
                    TamekiRoot(
                        launchIntent = pendingIntent,
                        onConsumeIntent = { pendingIntent = null },
                        settings = settings,
                        transactions = transactions,
                        accounts = accounts,
                        categories = categories,
                        receipts = receipts,
                        receiptReader = receiptReader,
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
        val scope = kotlinx.coroutines.MainScope()
        scope.launch {
            val enabled = settings.observeLockEnabled().first()
            if (enabled && BiometricLock.shouldLock()) {
                locked = true
                BiometricLock.prompt(
                    activity = this@MainActivity,
                    onUnlocked = { locked = false },
                    onFailed = { /* stay locked */ },
                )
            }
        }
    }
}

private data class Destination(
    val route: Any,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
private fun TamekiRoot(
    launchIntent: Intent?,
    onConsumeIntent: () -> Unit,
    settings: SettingsRepository,
    transactions: TransactionRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    receipts: ReceiptRepository,
    receiptReader: ReceiptReader,
    clock: Clock,
    activity: FragmentActivity,
) {
    val navController = rememberNavController()
    val quickAdd: QuickAddViewModel = koinViewModel()
    val quickAddState by quickAdd.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var fabMenu by rememberSaveable { mutableStateOf(false) }
    var openInbox by rememberSaveable { mutableStateOf(false) }
    var sharePasteText by rememberSaveable { mutableStateOf<String?>(null) }
    val onboardingDone by settings.observeOnboardingDone().collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val dest = File(context.filesDir, "receipts/${UUID.randomUUID()}.jpg")
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            val receiptId = receipts.upsert(
                Receipt(imagePath = dest.absolutePath, createdAt = Instant.now(clock)),
            )
            val read = runCatching { receiptReader.read(dest.absolutePath) }.getOrNull()
            if (read != null) {
                quickAdd.openFromDraft(read.draft)
            } else {
                quickAdd.openNew()
            }
        }
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
            IntentAction.OpenScan -> photoPicker.launch("image/*")
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
                val dest = File(context.filesDir, "receipts/${UUID.randomUUID()}.jpg")
                dest.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                receipts.upsert(Receipt(imagePath = dest.absolutePath, createdAt = Instant.now(clock)))
                val read = runCatching { receiptReader.read(dest.absolutePath) }.getOrNull()
                if (read != null) quickAdd.openFromDraft(read.draft) else quickAdd.openNew()
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
        OnboardingScreen(
            initialCurrency = base.value,
            onFinish = { currency, accountDrafts, importCsv ->
                scope.launch {
                    val code = CurrencyCode.parse(currency) ?: CurrencyCode("EUR")
                    settings.setBaseCurrency(code)
                    applyOnboardingAccounts(accounts, transactions, accountDrafts, code, clock)
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
    val undoLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_undo)
    LaunchedEffect(quickAddState.snackbar) {
        if (quickAddState.snackbar == "saved") {
            val result = snackbarHost.showSnackbar(
                message = savedLabel,
                actionLabel = undoLabel,
            )
            if (result == SnackbarResult.ActionPerformed) quickAdd.undoLastSave()
            quickAdd.clearSnackbar()
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

    val destinations = listOf(
        Destination(HomeRoute, R.string.nav_home, NavIcons.Today, NavIcons.TodaySelected),
        Destination(ActivityRoute(), R.string.nav_activity, NavIcons.Activity, NavIcons.ActivitySelected),
        Destination(SubscriptionsRoute, R.string.nav_subscriptions, NavIcons.Recurring, NavIcons.RecurringSelected),
        Destination(InsightsRoute(), R.string.nav_insights, NavIcons.Save, NavIcons.SaveSelected),
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
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showChrome) {
                Box {
                    AddFab(
                        onClick = { quickAdd.openNew() },
                        onLongClick = { fabMenu = true },
                    )
                    DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fab_scan_receipt)) },
                            onClick = {
                                fabMenu = false
                                photoPicker.launch("image/*")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.fab_paste_ai)) },
                            onClick = {
                                fabMenu = false
                                navController.navigate(PasteFromAiRoute)
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
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
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
                )
            }
            composable<ActivityRoute> { entry ->
                val route = entry.toRoute<ActivityRoute>()
                ActivityFeatureRoute(
                    openInbox = route.openInbox || openInbox,
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
                )
            }
            composable<InsightsRoute> { entry ->
                val route = entry.toRoute<InsightsRoute>()
                InsightsFeatureRoute(
                    openBudget = route.openBudget,
                    onOpenSubscription = { id -> navController.navigate(RecurringDetailRoute(id)) },
                    onReviewSubscriptions = { navController.navigate(SubscriptionsReviewRoute) },
                    onOpenCategory = {
                        navController.navigate(ActivityRoute()) {
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
                    photoPicker.launch("image/*")
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
        onToggleSplit = quickAdd::toggleSplitEditor,
        onSetSplits = quickAdd::setSplits,
        onAttachPhoto = quickAdd::attachPhoto,
        onReadReceipt = quickAdd::readReceipt,
        onSave = quickAdd::save,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddFab(onClick: () -> Unit, onLongClick: () -> Unit) {
    FloatingActionButton(
        onClick = {},
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            role = Role.Button,
        ),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.nav_add),
        )
    }
}

private suspend fun applyOnboardingAccounts(
    accounts: AccountRepository,
    transactions: TransactionRepository,
    drafts: List<OnboardingAccountDraft>,
    currency: CurrencyCode,
    clock: Clock,
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
                    note = "Opening balance",
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
