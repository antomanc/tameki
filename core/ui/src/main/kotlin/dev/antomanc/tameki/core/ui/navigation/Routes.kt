package dev.antomanc.tameki.core.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. They live in core:ui so features never depend on each other:
 * a feature takes its arguments as a route and the app owns the NavHost.
 */
@Serializable
data object HomeRoute

@Serializable
data class ActivityRoute(val openInbox: Boolean = false)

@Serializable
data object SubscriptionsRoute

@Serializable
data object InsightsRoute

@Serializable
data object SettingsRoute

@Serializable
data object AiBridgeRoute

/** FAB long-press → scan a receipt. Wired as a TODO destination until the capture flow lands. */
@Serializable
data object ScanReceiptRoute

/** FAB long-press → paste AI JSON. Wired as a TODO destination until the AI bridge flow lands. */
@Serializable
data object PasteFromAiRoute

/** FAB long-press / Recurring tab → create a recurring payment. */
@Serializable
data object NewRecurringRoute

@Serializable
data class RecurringDetailRoute(val id: Long)

/** [id] null means create. */
@Serializable
data class EditRecurringRoute(val id: Long? = null)

@Serializable
data object SubscriptionsReviewRoute
