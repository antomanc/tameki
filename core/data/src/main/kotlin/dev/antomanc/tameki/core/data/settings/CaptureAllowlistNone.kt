package dev.antomanc.tameki.core.data.settings

/**
 * Explicit "capture from none" allowlist marker.
 * A non-empty set containing only this value means no banking packages are allowed,
 * distinct from an empty set which means "use known defaults" in CaptureAllowlist.
 */
object CaptureAllowlistNone {
    const val SENTINEL: String = "__none__"

    fun isNone(allowlist: Set<String>): Boolean =
        allowlist.size == 1 && allowlist.contains(SENTINEL)

    fun none(): Set<String> = setOf(SENTINEL)
}
