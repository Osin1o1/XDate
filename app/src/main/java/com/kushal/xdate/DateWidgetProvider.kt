package com.kushal.xdate

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Classic 1x1 widget. The other styles subclass this so each gets its own size in the launcher's widget list.
 * Kept under its original name so widgets added with version 1.0 keep working.
 */
open class DateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val style = styleOf(javaClass)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, style)
        }
        scheduleMidnightUpdate(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetRenderer.delete(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        scheduleMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget of this style is removed; other styles may still be on screen.
        if (allWidgetIds(context).isEmpty()) cancelMidnightUpdate(context)
    }

    companion object {
        private val providers = mapOf(
            DateWidgetProvider::class.java to WidgetStyle.CLASSIC,
            TallWidgetProvider::class.java to WidgetStyle.TALL,
            WideWidgetProvider::class.java to WidgetStyle.WIDE,
            StripWidgetProvider::class.java to WidgetStyle.STRIP,
            MonthWidgetProvider::class.java to WidgetStyle.MONTH
        )

        private fun styleOf(provider: Class<*>) = providers[provider] ?: WidgetStyle.CLASSIC

        fun styleOf(context: Context, appWidgetId: Int): WidgetStyle {
            val className = AppWidgetManager.getInstance(context)
                .getAppWidgetInfo(appWidgetId)?.provider?.className
            return providers.entries.firstOrNull { it.key.name == className }?.value ?: WidgetStyle.CLASSIC
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            style: WidgetStyle
        ) {
            val config = WidgetRenderer.load(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, WidgetRenderer.render(context, style, config))
        }

        /** Redraws every XDate widget on the home screen and schedules the next midnight refresh. */
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            var any = false
            for ((provider, style) in providers) {
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, provider))
                for (id in ids) updateWidget(context, appWidgetManager, id, style)
                any = any || ids.isNotEmpty()
            }
            if (any) scheduleMidnightUpdate(context)
        }

        private fun allWidgetIds(context: Context): List<Int> {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            return providers.keys.flatMap {
                appWidgetManager.getAppWidgetIds(ComponentName(context, it)).toList()
            }
        }

        private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, MidnightUpdateReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun scheduleMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = midnightIntent(context)

            // Schedule for tomorrow at 00:00:01. MIUI and other battery savers can drop this alarm once the app
            // is killed; the 30-minute updatePeriodMillis in the widget info is the backup.
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 1)
                set(Calendar.MILLISECOND, 0)
            }

            val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
            try {
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight.timeInMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight.timeInMillis, pendingIntent)
            }
        }

        private fun cancelMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(midnightIntent(context))
        }
    }
}

class TallWidgetProvider : DateWidgetProvider()
class WideWidgetProvider : DateWidgetProvider()
class StripWidgetProvider : DateWidgetProvider()
class MonthWidgetProvider : DateWidgetProvider()
