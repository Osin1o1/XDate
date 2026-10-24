package com.kushal.xdate

import android.app.PendingIntent
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

/** Widget shapes. Each one has its own provider so the launcher places it at the right size. */
enum class WidgetStyle(val title: String, val cols: Int, val rows: Int, val layout: Int) {
    CLASSIC("Classic", 1, 1, R.layout.widget_classic),
    TALL("Tall", 1, 2, R.layout.widget_tall),
    WIDE("Wide", 2, 1, R.layout.widget_wide),
    STRIP("Strip", 4, 1, R.layout.widget_strip),
    MONTH("Month", 4, 2, R.layout.widget_month)
}

enum class DateLang { NEPALI, ENGLISH, BOTH }
enum class Ink { AUTO, LIGHT, DARK }
enum class WidgetBg { NONE, SOFT }

data class WidgetConfig(
    val lang: DateLang = DateLang.NEPALI,
    val ink: Ink = Ink.AUTO,
    val bg: WidgetBg = WidgetBg.NONE
)

/**
 * Builds the RemoteViews for every widget style. Also used by the setup screen for its live preview.
 */
object WidgetRenderer {

    private const val PREFS_NAME = "xdate_widget_prefs"

    private val enMonths = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private val enDays = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val enDayHeads = arrayOf("S", "M", "T", "W", "T", "F", "S")
    private val npDayHeads = arrayOf("आ", "सो", "मं", "बु", "बि", "शु", "श")

    private class Palette(
        val main: Int,
        val label: Int,
        val saturday: Int,
        val todayText: Int,
        val todayBg: Int,
        val softBg: Int
    )

    private val lightInk = Palette(
        Color.WHITE, 0xFFA8A8A8.toInt(), 0xFFFF8A8A.toInt(),
        0xFF111111.toInt(), R.drawable.widget_today_light, R.drawable.widget_soft_dark
    )
    private val darkInk = Palette(
        0xFF111111.toInt(), 0xFF555555.toInt(), 0xFFC8102E.toInt(),
        Color.WHITE, R.drawable.widget_today_dark, R.drawable.widget_soft_light
    )

    // ---- Saved settings, one set per widget id ----

    fun load(context: Context, appWidgetId: Int): WidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Widgets from version 1.0 only stored "english" or "nepali".
        val legacy = prefs.getString("widget_type_$appWidgetId", null)
        val legacyLang = if (legacy == "english") DateLang.ENGLISH else DateLang.NEPALI
        return WidgetConfig(
            lang = enumOr(prefs.getString("lang_$appWidgetId", null), legacyLang),
            ink = enumOr(prefs.getString("ink_$appWidgetId", null), Ink.AUTO),
            bg = enumOr(prefs.getString("bg_$appWidgetId", null), WidgetBg.NONE)
        )
    }

    fun save(context: Context, appWidgetId: Int, config: WidgetConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("lang_$appWidgetId", config.lang.name)
            .putString("ink_$appWidgetId", config.ink.name)
            .putString("bg_$appWidgetId", config.bg.name)
            .remove("widget_type_$appWidgetId")
            .apply()
    }

    fun delete(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (id in appWidgetIds) {
            listOf("lang_", "ink_", "bg_", "widget_type_").forEach { editor.remove("$it$id") }
        }
        editor.apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    // ---- Rendering ----

    /** White text on dark wallpapers, black on light ones. Uses the launcher's wallpaper hint when available. */
    private fun resolveInk(context: Context, ink: Ink): Ink {
        if (ink != Ink.AUTO) return ink
        if (Build.VERSION.SDK_INT < 27) return Ink.LIGHT
        val colors = try {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        } catch (e: Exception) {
            null
        } ?: return Ink.LIGHT
        val lightWallpaper = if (Build.VERSION.SDK_INT >= 31) {
            colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        } else {
            Color.luminance(colors.primaryColor.toArgb()) > 0.6f
        }
        return if (lightWallpaper) Ink.DARK else Ink.LIGHT
    }

    fun render(
        context: Context,
        style: WidgetStyle,
        config: WidgetConfig,
        clickable: Boolean = true
    ): RemoteViews {
        val pal = if (resolveInk(context, config.ink) == Ink.DARK) darkInk else lightInk
        val views = RemoteViews(context.packageName, style.layout)
        val today = Today(Calendar.getInstance())

        when (style) {
            WidgetStyle.CLASSIC -> renderClassic(views, config.lang, today, pal)
            WidgetStyle.TALL -> renderTall(views, config.lang, today, pal)
            WidgetStyle.WIDE -> renderWide(views, config.lang, today, pal)
            WidgetStyle.STRIP -> renderStrip(views, config.lang, today, pal)
            WidgetStyle.MONTH -> renderMonth(context, views, config.lang, today, pal)
        }

        views.setInt(
            R.id.widgetRoot, "setBackgroundResource",
            if (config.bg == WidgetBg.SOFT) pal.softBg else 0
        )

        if (clickable) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
        }
        return views
    }

    private class Today(val cal: Calendar) {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = cal.get(Calendar.DAY_OF_WEEK) - 1
        val bs = NepaliDateConverter.convertToNepali(year, month, day)
    }

    private fun np(number: Int) = NepaliDateConverter.toNepaliDigits(number)
    private fun shortMonth(month: Int) = enMonths[month - 1].take(3)

    private fun RemoteViews.text(id: Int, value: CharSequence, color: Int) {
        setTextViewText(id, value)
        setTextColor(id, color)
        setViewVisibility(id, View.VISIBLE)
    }

    private fun RemoteViews.hide(vararg ids: Int) = ids.forEach { setViewVisibility(it, View.GONE) }

    private fun RemoteViews.rule(id: Int, color: Int) {
        setInt(id, "setBackgroundColor", color)
        setViewVisibility(id, View.VISIBLE)
    }

    private fun renderClassic(v: RemoteViews, lang: DateLang, t: Today, pal: Palette) {
        val nepali = lang != DateLang.ENGLISH
        v.text(R.id.day, if (nepali) np(t.bs.day) else "${t.day}", pal.main)
        v.text(R.id.label, if (nepali) t.bs.monthName else shortMonth(t.month).uppercase(), pal.label)
        // Always set the size: launchers reapply updates onto the existing view, so it would otherwise stick.
        v.setTextViewTextSize(R.id.day, TypedValue.COMPLEX_UNIT_SP, if (lang == DateLang.BOTH) 36f else 44f)
        if (lang == DateLang.BOTH) {
            v.rule(R.id.rule, pal.label)
            v.text(R.id.sub, "${t.day} ${shortMonth(t.month).uppercase()}", pal.label)
        } else {
            v.hide(R.id.rule, R.id.sub)
        }
    }

    private fun renderTall(v: RemoteViews, lang: DateLang, t: Today, pal: Palette) {
        val nepali = lang != DateLang.ENGLISH
        v.text(R.id.top, if (nepali) NepaliDateConverter.nepaliDayNames[t.weekday] else enDays[t.weekday].uppercase(), pal.label)
        v.text(R.id.day, if (nepali) np(t.bs.day) else "${t.day}", pal.main)
        v.text(R.id.label, if (nepali) t.bs.monthName else shortMonth(t.month).uppercase(), pal.label)
        v.text(R.id.year, if (nepali) np(t.bs.year) else "${t.year}", pal.label)
        if (lang == DateLang.BOTH) {
            v.rule(R.id.rule, pal.label)
            v.text(R.id.sub, "${enDays[t.weekday].take(3)} ${t.day} ${shortMonth(t.month)}".uppercase(), pal.label)
        } else {
            v.hide(R.id.rule, R.id.sub)
        }
    }

    private fun renderWide(v: RemoteViews, lang: DateLang, t: Today, pal: Palette) {
        if (lang == DateLang.BOTH) {
            // Nepali | English, side by side
            v.text(R.id.dayA, np(t.bs.day), pal.main)
            v.text(R.id.labelA, t.bs.monthName, pal.label)
            v.rule(R.id.divider, pal.label)
            v.text(R.id.dayB, "${t.day}", pal.main)
            v.text(R.id.labelB, shortMonth(t.month).uppercase(), pal.label)
            v.hide(R.id.subB)
            v.setInt(R.id.colB, "setGravity", Gravity.CENTER)
        } else {
            // Big day on the left, month, year and weekday on the right
            val nepali = lang == DateLang.NEPALI
            v.text(R.id.dayA, if (nepali) np(t.bs.day) else "${t.day}", pal.main)
            v.hide(R.id.labelA, R.id.dayB)
            v.setInt(R.id.divider, "setBackgroundColor", Color.TRANSPARENT)
            v.text(
                R.id.labelB,
                if (nepali) "${t.bs.monthName} ${np(t.bs.year)}" else "${enMonths[t.month - 1]} ${t.year}".uppercase(),
                pal.main
            )
            v.text(
                R.id.subB,
                if (nepali) NepaliDateConverter.nepaliDayNames[t.weekday] else enDays[t.weekday].uppercase(),
                pal.label
            )
            v.setInt(R.id.colB, "setGravity", Gravity.START or Gravity.CENTER_VERTICAL)
        }
    }

    private fun renderStrip(v: RemoteViews, lang: DateLang, t: Today, pal: Palette) {
        val text = SpannableStringBuilder()
        fun big(s: String) {
            val start = text.length
            text.append(s)
            text.setSpan(RelativeSizeSpan(1.7f), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(StyleSpan(Typeface.BOLD), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(ForegroundColorSpan(pal.main), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val dot = "  ·  "
        val npDate = "${np(t.bs.day)} ${t.bs.monthName} ${np(t.bs.year)}"
        when (lang) {
            DateLang.NEPALI -> {
                big(npDate)
                text.append(dot).append(NepaliDateConverter.nepaliDayNames[t.weekday])
            }
            DateLang.ENGLISH -> {
                big("${t.day} ${enMonths[t.month - 1]} ${t.year}")
                text.append(dot).append(enDays[t.weekday])
            }
            DateLang.BOTH -> {
                big(npDate)
                text.append(dot).append(NepaliDateConverter.nepaliDayNames[t.weekday])
                text.append(dot).append("${t.day} ${shortMonth(t.month)} ${t.year}")
            }
        }
        v.text(R.id.strip, text, pal.label)
    }

    private fun renderMonth(context: Context, v: RemoteViews, lang: DateLang, t: Today, pal: Palette) {
        val first = t.cal.clone() as Calendar
        val monthLength: Int
        val todayNumber: Int
        val title: String
        val subtitle: String

        if (lang == DateLang.ENGLISH) {
            first.set(Calendar.DAY_OF_MONTH, 1)
            monthLength = first.getActualMaximum(Calendar.DAY_OF_MONTH)
            todayNumber = t.day
            title = "${enMonths[t.month - 1]} ${t.year}".uppercase()
            val bsFirst = NepaliDateConverter.convertToNepali(t.year, t.month, 1)
            val bsLast = NepaliDateConverter.convertToNepali(t.year, t.month, monthLength)
            subtitle = "${bsFirst.monthNameEn} – ${bsLast.monthNameEn}"
        } else {
            first.add(Calendar.DAY_OF_MONTH, -(t.bs.day - 1))
            monthLength = NepaliDateConverter.daysInMonth(t.bs.year, t.bs.month)
            todayNumber = t.bs.day
            title = "${t.bs.monthName} ${np(t.bs.year)}"
            subtitle = if (lang == DateLang.BOTH) {
                val last = first.clone() as Calendar
                last.add(Calendar.DAY_OF_MONTH, monthLength - 1)
                val m1 = first.get(Calendar.MONTH) + 1
                val m2 = last.get(Calendar.MONTH) + 1
                "${shortMonth(m1)} – ${shortMonth(m2)} ${last.get(Calendar.YEAR)}"
            } else ""
        }
        v.text(R.id.title, title, pal.main)
        v.text(R.id.monthSub, subtitle, pal.label)

        val pkg = context.packageName
        v.removeAllViews(R.id.rows)

        val heads = if (lang == DateLang.ENGLISH) enDayHeads else npDayHeads
        val headRow = RemoteViews(pkg, R.layout.widget_month_row)
        heads.forEachIndexed { i, h ->
            headRow.addView(R.id.row, cell(pkg, h, if (i == 6) pal.saturday else pal.label, null))
        }
        v.addView(R.id.rows, headRow)

        val offset = first.get(Calendar.DAY_OF_WEEK) - 1
        val adDay = first.clone() as Calendar
        var row = RemoteViews(pkg, R.layout.widget_month_row)
        var col = 0
        repeat(offset) { row.addView(R.id.row, cell(pkg, "", pal.label, null)); col++ }

        for (d in 1..monthLength) {
            val text: CharSequence = when (lang) {
                DateLang.ENGLISH -> "$d"
                DateLang.NEPALI -> np(d)
                DateLang.BOTH -> SpannableStringBuilder(np(d)).apply {
                    val start = length
                    append("\n").append("${adDay.get(Calendar.DAY_OF_MONTH)}")
                    setSpan(RelativeSizeSpan(0.62f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (d != todayNumber) {
                        setSpan(ForegroundColorSpan(pal.label), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            val isToday = d == todayNumber
            val color = when {
                isToday -> pal.todayText
                col == 6 -> pal.saturday
                else -> pal.main
            }
            row.addView(R.id.row, cell(pkg, text, color, if (isToday) pal.todayBg else null))
            adDay.add(Calendar.DAY_OF_MONTH, 1)

            if (++col == 7) {
                v.addView(R.id.rows, row)
                row = RemoteViews(pkg, R.layout.widget_month_row)
                col = 0
            }
        }
        if (col > 0) {
            while (col++ < 7) row.addView(R.id.row, cell(pkg, "", pal.label, null))
            v.addView(R.id.rows, row)
        }
    }

    private fun cell(pkg: String, text: CharSequence, color: Int, background: Int?): RemoteViews {
        val cell = RemoteViews(pkg, R.layout.widget_month_cell)
        cell.setTextViewText(R.id.cell, text)
        cell.setTextColor(R.id.cell, color)
        if (background != null) cell.setInt(R.id.cell, "setBackgroundResource", background)
        return cell
    }
}
