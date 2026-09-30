package il.hiya.shabbatwatch

import il.hiya.shabbatwatch.clock.HebrewDate
import il.hiya.shabbatwatch.clock.HebrewNumerals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HebrewDateTest {

    @Test
    fun roshHashana5787_isSeptember12_2026() {
        assertEquals(HebrewDate(5787, 7, 1), HebrewDate.of(LocalDate.of(2026, 9, 12)))
    }

    @Test
    fun roshHashana5785_isOctober3_2024() {
        assertEquals(HebrewDate(5785, 7, 1), HebrewDate.of(LocalDate.of(2024, 10, 3)))
    }

    @Test
    fun pesach5786_isApril2_2026() {
        assertEquals(HebrewDate(5786, 1, 15), HebrewDate.of(LocalDate.of(2026, 4, 2)))
    }

    @Test
    fun leapYearsFollowTheNineteenYearCycle() {
        assertTrue(HebrewDate.isLeapYear(5784))
        assertFalse(HebrewDate.isLeapYear(5785))
        assertTrue(HebrewDate.isLeapYear(5787))
    }

    @Test
    fun formatUsesGematriaAndMonthName() {
        assertEquals("ט״ז תשרי תשפ״ז", HebrewDate(5787, 7, 16).format())
        assertEquals("א׳ ניסן תשפ״ו", HebrewDate(5786, 1, 1).format())
    }

    @Test
    fun numeralsUseCustomaryFifteenAndSixteen() {
        assertEquals("ט״ו", HebrewNumerals.of(15))
        assertEquals("ט״ז", HebrewNumerals.of(16))
        assertEquals("כ״ט", HebrewNumerals.of(29))
        assertEquals("ל׳", HebrewNumerals.of(30))
    }
}
