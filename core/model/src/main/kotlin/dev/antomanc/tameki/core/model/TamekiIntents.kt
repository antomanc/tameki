package dev.antomanc.tameki.core.model

/**
 * Cross-module intent extras shared by capture notifications, deep links and MainActivity.
 */
object TamekiIntents {
    /** Extra key: which in-app surface to open when the launcher activity starts. */
    const val EXTRA_OPEN: String = "tameki.open"

    /** [EXTRA_OPEN] value: show the payment-capture inbox. */
    const val OPEN_INBOX: String = "inbox"
}
