package dev.antomanc.tameki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.antomanc.tameki.core.model.TamekiIntents
import dev.antomanc.tameki.core.ui.navigation.ActivityRoute
import dev.antomanc.tameki.core.ui.navigation.AiBridgeRoute
import dev.antomanc.tameki.core.ui.navigation.EditRecurringRoute
import dev.antomanc.tameki.core.ui.navigation.HomeRoute
import dev.antomanc.tameki.core.ui.navigation.InsightsRoute
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
import dev.antomanc.tameki.feature.home.HomeRoute as HomeFeatureRoute
import dev.antomanc.tameki.feature.insights.InsightsRoute as InsightsFeatureRoute
import dev.antomanc.tameki.feature.settings.SettingsScreen
import dev.antomanc.tameki.feature.subscriptions.RecurringEditRoute
import dev.antomanc.tameki.feature.subscriptions.SubscriptionDetailRoute
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsReviewRoute as SubscriptionsReviewFeature
import dev.antomanc.tameki.feature.subscriptions.SubscriptionsRoute as SubscriptionsFeatureRoute
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TamekiTheme {
                TamekiRoot(launchIntent = intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

private data class Destination(
    val route: Any,
    @param:StringRes val label: Int,
    val icon: ImageVector,
)

@Composable
private fun TamekiRoot(launchIntent: Intent?) {
    val navController = rememberNavController()
    val quickAdd: QuickAddViewModel = koinViewModel()
    val quickAddState by quickAdd.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    var fabMenu by rememberSaveable { mutableStateOf(false) }
    var openInbox by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(launchIntent) {
        if (launchIntent?.getStringExtra(TamekiIntents.EXTRA_OPEN) == TamekiIntents.OPEN_INBOX) {
            openInbox = true
            navController.navigate(ActivityRoute(openInbox = true)) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val savedLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_saved)
    val undoLabel = stringResource(dev.antomanc.tameki.feature.activity.R.string.activity_undo)
    LaunchedEffect(quickAddState.snackbar) {
        if (quickAddState.snackbar == "saved") {
            val result = snackbarHost.showSnackbar(
                message = "$savedLabel · $undoLabel",
                actionLabel = undoLabel,
            )
            if (result == SnackbarResult.ActionPerformed) quickAdd.undoLastSave()
            quickAdd.clearSnackbar()
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val routeName = backStack?.destination?.route.orEmpty()
    val selectedIndex = when {
        routeName.contains("ActivityRoute") -> 1
        routeName.contains("SubscriptionsRoute") -> 2
        routeName.contains("InsightsRoute") -> 3
        else -> 0
    }

    val destinations = listOf(
        Destination(HomeRoute, R.string.nav_home, Icons.Default.Home),
        Destination(ActivityRoute(), R.string.nav_activity, Icons.Default.DateRange),
        Destination(SubscriptionsRoute, R.string.nav_subscriptions, Icons.Default.Star),
        Destination(InsightsRoute, R.string.nav_insights, Icons.Default.Info),
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            val target = when (index) {
                                1 -> ActivityRoute(openInbox = false)
                                else -> destination.route
                            }
                            navController.navigate(target) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            if (index != 1) openInbox = false
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
        floatingActionButton = {
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
                            // TODO: wire ScanReceiptRoute when capture UI lands
                            navController.navigate(ScanReceiptRoute)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.fab_paste_ai)) },
                        onClick = {
                            fabMenu = false
                            // TODO: wire PasteFromAiRoute
                            navController.navigate(PasteFromAiRoute)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.fab_new_recurring)) },
                        onClick = {
                            fabMenu = false
                            // TODO: wire NewRecurringRoute when subscriptions editor lands
                            navController.navigate(NewRecurringRoute)
                        },
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
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
                    onOpenUrl = { /* TODO: open cancel URL */ },
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
            composable<InsightsRoute> {
                InsightsFeatureRoute(
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
            composable<SettingsRoute> { SettingsScreen() }
            composable<AiBridgeRoute> { AibridgeScreen() }
            composable<ScanReceiptRoute> { Box(Modifier.fillMaxSize()) }
            composable<PasteFromAiRoute> { Box(Modifier.fillMaxSize()) }
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
    Surface(
        modifier = Modifier
            .size(96.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.nav_add),
            )
        }
    }
}
