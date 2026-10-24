package com.kushal.xdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Refreshes all widgets at midnight, after a reboot, an app update, or a time/timezone change.
 * Each refresh schedules the next midnight alarm.
 */
class MidnightUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DateWidgetProvider.updateAll(context)
    }
}
