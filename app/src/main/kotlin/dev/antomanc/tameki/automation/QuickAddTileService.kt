package dev.antomanc.tameki.automation

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import dev.antomanc.tameki.MainActivity

class QuickAddTileService : TileService() {
    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(AutomationIntents.EXTRA_OPEN, AutomationIntents.OPEN_QUICK_ADD)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
