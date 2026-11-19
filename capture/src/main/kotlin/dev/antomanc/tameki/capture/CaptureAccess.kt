package dev.antomanc.tameki.capture

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import dev.antomanc.tameki.core.model.capture.KnownBankingPackages

/** Helpers for the settings UI around notification-listener access and banking apps. */
object CaptureAccess {
    fun isGranted(context: Context): Boolean {
        val expected = ComponentName(context, PaymentNotificationListener::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        if (TextUtils.isEmpty(flat)) return false
        return flat.split(':').any { ComponentName.unflattenFromString(it)?.equals(expected) == true }
    }

    fun intentToGrant(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /**
     * Installed packages that appear in [KnownBankingPackages], for the allowlist picker.
     */
    fun installedKnownBankingApps(context: Context): List<KnownBankingPackages.Entry> {
        val pm = context.packageManager
        return KnownBankingPackages.all.filter { entry ->
            try {
                pm.getApplicationInfo(entry.packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Resolves a human-readable app label if visible to this app, or null when package visibility
     * hides it. Never requires `QUERY_ALL_PACKAGES`.
     */
    fun resolveAppLabel(context: Context, packageName: String): String? {
        KnownBankingPackages.labelFor(packageName)?.let { return it }
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}

/** Empty user allowlist means "use the known banking defaults". */
object CaptureAllowlist {
    fun effective(userAllowlist: Set<String>): Set<String> =
        userAllowlist.ifEmpty { KnownBankingPackages.packageNames }
}
