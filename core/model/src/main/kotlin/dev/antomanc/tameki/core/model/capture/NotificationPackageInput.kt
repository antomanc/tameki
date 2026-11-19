package dev.antomanc.tameki.core.model.capture

import java.util.Locale

/**
 * Parses user input for custom notification-capture apps.
 *
 * Accepts either a raw Android package ID (`com.latuabancaperandroid`) or a shared Google Play
 * Store URL (`https://play.google.com/store/apps/details?id=com.latuabancaperandroid&hl=it` or
 * `market://details?id=...`), without requiring `QUERY_ALL_PACKAGES`.
 */
object NotificationPackageInput {
    private val validPackage = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")
    private val playIdQuery = Regex("""[?&]id=([a-zA-Z][a-zA-Z0-9_.]*)""")

    fun parsePackageInput(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val candidate = playIdQuery.find(trimmed)?.groupValues?.getOrNull(1)
            ?: trimmed.substringBefore('?').substringBefore('#').trim()
        return candidate.takeIf { validPackage.matches(it) }
    }

    /** Human-readable fallback label when Android package visibility hides the app's real name. */
    fun fallbackLabel(packageName: String): String {
        KnownBankingPackages.labelFor(packageName)?.let { return it }
        val genericSegments = setOf("android", "app", "mobile", "customer", "prd", "prod", "it", "en", "nl")
        val segments = packageName.split('.').filter { it.isNotBlank() }
        val picked = segments.asReversed().firstOrNull { it.lowercase(Locale.ROOT) !in genericSegments }
            ?: segments.lastOrNull()
            ?: packageName
        return picked.replace('_', ' ').replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
        }
    }
}
