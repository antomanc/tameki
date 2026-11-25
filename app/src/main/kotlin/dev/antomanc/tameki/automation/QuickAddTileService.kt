package dev.antomanc.tameki.automation

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import dev.antomanc.tameki.MainActivity

class QuickAddTileService : TileService() {
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(AutomationIntents.EXTRA_OPEN, AutomationIntents.OPEN_QUICK_ADD)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION") // Intent overload is the only API below 34
            @SuppressLint("StartActivityAndCollapseDeprecated") // Intent overload is the only API below 34
            startActivityAndCollapse(intent)
        }
    }
}
