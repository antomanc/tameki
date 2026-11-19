package dev.antomanc.tameki

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.antomanc.tameki.core.ui.navigation.ActivityRoute
import dev.antomanc.tameki.core.ui.navigation.HomeRoute
import dev.antomanc.tameki.core.ui.navigation.InsightsRoute
import dev.antomanc.tameki.core.ui.navigation.NavIcons
import dev.antomanc.tameki.core.ui.navigation.SubscriptionsRoute

/** The four bottom-bar destinations. Every other destination is a pushed screen without chrome. */
internal enum class AppTab(
    val route: Any,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Today(HomeRoute, R.string.nav_home, NavIcons.Today, NavIcons.TodaySelected),
    Activity(ActivityRoute(), R.string.nav_activity, NavIcons.Activity, NavIcons.ActivitySelected),
    Recurring(SubscriptionsRoute, R.string.nav_subscriptions, NavIcons.Recurring, NavIcons.RecurringSelected),
    Save(InsightsRoute(), R.string.nav_insights, NavIcons.Save, NavIcons.SaveSelected),
}

/** The tab this destination is, or null for a pushed screen. Type-safe: no route-name matching. */
internal fun NavDestination.appTab(): AppTab? = when {
    hasRoute<HomeRoute>() -> AppTab.Today
    hasRoute<ActivityRoute>() -> AppTab.Activity
    hasRoute<SubscriptionsRoute>() -> AppTab.Recurring
    hasRoute<InsightsRoute>() -> AppTab.Save
    else -> null
}

/**
 * Switches tab the way a bottom bar does: one entry per tab, each tab keeps its own back stack and
 * scroll. [restore] is false when the route carries a one-shot argument (a category filter, open
 * the inbox): restoring would bring back the tab's old entry and drop the new argument.
 */
internal fun NavController.navigateToTab(route: Any, restore: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = restore
    }
}

/**
 * Navigation motion, following Material 3 transition patterns:
 * - between tabs, **fade through**: the old tab fades out quickly, the new one fades and scales in;
 * - into a pushed screen and back, **shared axis X**: a short slide with a fade, reversed on pop,
 *   which the predictive back gesture scrubs.
 * With "remove animations" on, everything snaps.
 */
internal class NavMotion(private val enabled: Boolean) {
    private val slide: FiniteAnimationSpec<IntOffset> = spring(dampingRatio = 1f, stiffness = 500f)
    private val fadeInSpec = tween<Float>(durationMillis = 210, delayMillis = 60)
    private val fadeOutSpec = tween<Float>(durationMillis = 90)
    private val scaleSpec = spring<Float>(dampingRatio = 0.9f, stiffness = 500f)

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
        initialState.destination.appTab() != null && targetState.destination.appTab() != null

    fun enter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition = with(scope) {
        when {
            !enabled -> EnterTransition.None
            isTabSwitch() -> fadeIn(fadeInSpec) + scaleIn(scaleSpec, initialScale = 0.94f)
            else -> slideInHorizontally(slide) { it / 6 } + fadeIn(fadeInSpec)
        }
    }

    fun exit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition = with(scope) {
        when {
            !enabled -> ExitTransition.None
            isTabSwitch() -> fadeOut(fadeOutSpec)
            else -> slideOutHorizontally(slide) { -it / 10 } + fadeOut(fadeOutSpec)
        }
    }

    fun popEnter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition = with(scope) {
        when {
            !enabled -> EnterTransition.None
            isTabSwitch() -> fadeIn(fadeInSpec) + scaleIn(scaleSpec, initialScale = 0.94f)
            else -> slideInHorizontally(slide) { -it / 10 } + fadeIn(fadeInSpec)
        }
    }

    fun popExit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition = with(scope) {
        when {
            !enabled -> ExitTransition.None
            isTabSwitch() -> fadeOut(fadeOutSpec)
            else -> slideOutHorizontally(slide) { it / 6 } + fadeOut(fadeOutSpec)
        }
    }
}
