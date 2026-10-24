package com.kushal.xdate

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Setup screen shown when a widget is added (or reconfigured on Android 12+).
 * The preview is the real widget drawn over the user's own wallpaper.
 */
class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var style: WidgetStyle
    private lateinit var config: WidgetConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        // Set result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        style = DateWidgetProvider.styleOf(this, appWidgetId)
        config = WidgetRenderer.load(this, appWidgetId)

        findViewById<TextView>(R.id.styleName).text = "${style.title}  ${style.cols}×${style.rows}"

        option(R.id.langNp) { config.copy(lang = DateLang.NEPALI) }
        option(R.id.langEn) { config.copy(lang = DateLang.ENGLISH) }
        option(R.id.langBoth) { config.copy(lang = DateLang.BOTH) }
        option(R.id.inkAuto) { config.copy(ink = Ink.AUTO) }
        option(R.id.inkLight) { config.copy(ink = Ink.LIGHT) }
        option(R.id.inkDark) { config.copy(ink = Ink.DARK) }
        option(R.id.bgNone) { config.copy(bg = WidgetBg.NONE) }
        option(R.id.bgSoft) { config.copy(bg = WidgetBg.SOFT) }

        findViewById<TextView>(R.id.btnAdd).setOnClickListener { configureWidget() }

        render()
    }

    private fun option(id: Int, change: () -> WidgetConfig) {
        findViewById<TextView>(id).setOnClickListener {
            config = change()
            render()
        }
    }

    private fun render() {
        select(R.id.langNp, config.lang == DateLang.NEPALI)
        select(R.id.langEn, config.lang == DateLang.ENGLISH)
        select(R.id.langBoth, config.lang == DateLang.BOTH)
        select(R.id.inkAuto, config.ink == Ink.AUTO)
        select(R.id.inkLight, config.ink == Ink.LIGHT)
        select(R.id.inkDark, config.ink == Ink.DARK)
        select(R.id.bgNone, config.bg == WidgetBg.NONE)
        select(R.id.bgSoft, config.bg == WidgetBg.SOFT)

        val host = findViewById<FrameLayout>(R.id.previewHost)
        host.removeAllViews()
        val preview = WidgetRenderer.render(this, style, config, clickable = false).apply(this, host)
        val density = resources.displayMetrics.density
        val maxWidth = resources.displayMetrics.widthPixels - (32 * density).toInt()
        host.addView(
            preview,
            FrameLayout.LayoutParams(
                minOf((style.cols * 80 * density).toInt(), maxWidth),
                (style.rows * 90 * density).toInt()
            )
        )
    }

    private fun select(id: Int, selected: Boolean) {
        findViewById<TextView>(id).alpha = if (selected) 1f else 0.3f
    }

    private fun configureWidget() {
        WidgetRenderer.save(this, appWidgetId, config)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        DateWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, style)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
