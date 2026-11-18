package dev.antomanc.tameki

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Requests [Manifest.permission.POST_NOTIFICATIONS] the first time a reminder or notification
 * capture is enabled — never on cold launch.
 */
object NotificationPermission {
    private const val PREFS = "tameki.permissions"
    private const val KEY_ASKED = "post_notifications_asked"

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Call when the user enables a reminder or payment-capture. Returns true if a system dialog
     * was shown (or should be shown via the activity result launcher).
     */
    fun requestIfNeeded(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (hasPermission(activity)) return false
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false)) {
            // Already asked once; don't nag on every toggle. Caller may deep-link to settings.
            return false
        }
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE,
        )
        return true
    }

    /** For Compose Activity Result contracts — whether we should still prompt. */
    fun shouldPrompt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (hasPermission(context)) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_ASKED, false)
    }

    fun markAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED, true)
            .apply()
    }

    const val REQUEST_CODE: Int = 4401
}
