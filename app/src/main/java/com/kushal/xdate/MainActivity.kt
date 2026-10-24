package com.kushal.xdate

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import java.util.Calendar

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Immersive full screen. Light/dark bar icons come from the theme (windowLightStatusBar):
        // window.insetsController crashes on MIUI when called this early.
        val isDark = isDarkTheme()
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            if (!isDark) {
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= 26) {
                    @Suppress("DEPRECATION")
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = flags
        }

        findViewById<TextView>(R.id.footer).text =
            "made with ♥ by Osin1o1 · accurate till ${NepaliDateConverter.VERIFIED_UNTIL_YEAR} BS"
        displayDates()
    }

    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
               android.content.res.Configuration.UI_MODE_NIGHT_YES
    }


    override fun onResume() {
        super.onResume()
        displayDates()
        // Also refresh widgets, so AUTO text color catches a wallpaper change right away.
        DateWidgetProvider.updateAll(this)
    }

    private fun displayDates() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // English date
        val engMonthNames = arrayOf(
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
        )
        val engDayNames = arrayOf(
            "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY",
            "THURSDAY", "FRIDAY", "SATURDAY"
        )

        findViewById<TextView>(R.id.engDay).text = day.toString()
        findViewById<TextView>(R.id.engMonthYear).text = "${engMonthNames[month - 1]} $year"
        findViewById<TextView>(R.id.engDayName).text = engDayNames[dayOfWeek - 1]

        // Nepali date
        val nepDate = NepaliDateConverter.convertToNepali(year, month, day)
        
        findViewById<TextView>(R.id.nepDay).text = NepaliDateConverter.toNepaliDigits(nepDate.day)
        findViewById<TextView>(R.id.nepMonthYear).text = "${nepDate.monthName} ${NepaliDateConverter.toNepaliDigits(nepDate.year)}"
        // nepLabel is static in XML
    }
}
