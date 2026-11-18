package dev.antomanc.tameki.core.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. They live in core:ui so features never depend on each other:
 * a feature takes its arguments as a route and the app owns the NavHost.
 */
@Serializable
data object HomeRoute

@Serializable
data object ActivityRoute

@Serializable
data object SubscriptionsRoute

@Serializable
data object InsightsRoute

@Serializable
data object SettingsRoute

@Serializable
data object AiBridgeRoute
