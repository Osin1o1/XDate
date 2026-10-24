package com.kushal.xdate

import org.junit.Assert.assertEquals
import org.junit.Test

class NepaliDateConverterTest {

    private fun assertBs(ad: Triple<Int, Int, Int>, bs: Triple<Int, Int, Int>) {
        val result = NepaliDateConverter.convertToNepali(ad.first, ad.second, ad.third)
        assertEquals("AD $ad", bs, Triple(result.year, result.month, result.day))
    }

    @Test
    fun referenceDate() = assertBs(Triple(1943, 4, 14), Triple(2000, 1, 1))

    @Test
    fun newYear2082() = assertBs(Triple(2025, 4, 14), Triple(2082, 1, 1))

    @Test
    fun shrawan2082() = assertBs(Triple(2025, 7, 17), Triple(2082, 4, 1))

    @Test
    fun newYear2083() = assertBs(Triple(2026, 4, 14), Triple(2083, 1, 1))

    @Test
    fun kartik2083() = assertBs(Triple(2026, 10, 18), Triple(2083, 7, 1))

    @Test
    fun dayBeforeKartik2083() = assertBs(Triple(2026, 10, 17), Triple(2083, 6, 31))

    @Test
    fun newYear2084() = assertBs(Triple(2027, 4, 14), Triple(2084, 1, 1))

    @Test
    fun jestha2062() = assertBs(Triple(2005, 5, 15), Triple(2062, 2, 1))

    @Test
    fun lastDayOfTable() = assertBs(Triple(2044, 4, 12), Triple(2100, 12, 30))
}
