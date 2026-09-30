package il.hiya.shabbatwatch

import il.hiya.shabbatwatch.clock.HebrewDate
import il.hiya.shabbatwatch.clock.ShabbatCalendar
import il.hiya.shabbatwatch.clock.ShabbatTimes
import il.hiya.shabbatwatch.clock.TitleKind
import il.hiya.shabbatwatch.clock.ZmanKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ShabbatCalendarTest {
    private val zone: ZoneId = ZoneId.of("Asia/Jerusalem")
    private val times = ShabbatTimes(Constants.LOCATION)
    private val calendar = ShabbatCalendar(times)

    private fun at(date: LocalDate, time: LocalTime): ZonedDateTime = ZonedDateTime.of(date, time, zone)

    @Test
    fun shabbatBereishit2026_showsParsha() {
        val title = calendar.titleFor(LocalDate.of(2026, 10, 10))
        assertNotNull(title)
        assertEquals(TitleKind.PARSHA, title!!.kind)
        assertEquals("בראשית", title.name)
        assertNull(title.special)
    }

    @Test
    fun shminiAtzeret2026_fallsOnShabbat_showsHolidayNotParsha() {
        val title = calendar.titleFor(LocalDate.of(2026, 10, 3))
        assertEquals(TitleKind.HOLIDAY, title!!.kind)
        assertTrue(title.name.contains("שמיני עצרת"))
    }

    @Test
    fun weekday_showsUpcomingShabbatParsha() {
        val title = calendar.titleFor(LocalDate.of(2026, 10, 6))
        assertEquals(TitleKind.PARSHA, title!!.kind)
        assertEquals("בראשית", title.name)
    }

    @Test
    fun cholHamoedSukkot_showsHoliday() {
        val title = calendar.titleFor(LocalDate.of(2026, 9, 29))
        assertEquals(TitleKind.HOLIDAY, title!!.kind)
        assertTrue(title.name.contains("חול המועד"))
    }

    @Test
    fun afterSunsetOnFriday_hebrewDateAdvancesToShabbat() {
        val fridayNight = at(LocalDate.of(2026, 10, 9), LocalTime.of(19, 0))
        val info = calendar.infoFor(fridayNight)
        assertEquals(HebrewDate.of(LocalDate.of(2026, 10, 10)), info.hebrewDate)
        assertEquals(ZmanKind.SHABBAT_ENDS, info.zman!!.kind)
        assertEquals(LocalDate.of(2026, 10, 10), info.zman.time.toLocalDate())
    }

    @Test
    fun fridayAfternoon_showsCandleLightingInPlausibleWindow() {
        val friday = at(LocalDate.of(2026, 10, 9), LocalTime.of(15, 0))
        val zman = calendar.infoFor(friday).zman!!
        assertEquals(ZmanKind.CANDLE_LIGHTING, zman.kind)
        assertEquals(LocalDate.of(2026, 10, 9), zman.time.toLocalDate())
        assertTrue(zman.time.toLocalTime().isAfter(LocalTime.of(17, 30)))
        assertTrue(zman.time.toLocalTime().isBefore(LocalTime.of(18, 30)))
    }

    @Test
    fun shabbatEnd_isAfterSunsetInPlausibleWindow() {
        val shabbat = at(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0))
        val zman = calendar.infoFor(shabbat).zman!!
        assertEquals(ZmanKind.SHABBAT_ENDS, zman.kind)
        assertTrue(zman.time.toLocalTime().isAfter(LocalTime.of(18, 30)))
        assertTrue(zman.time.toLocalTime().isBefore(LocalTime.of(19, 30)))
    }

    @Test
    fun holidayFollowedByShabbat_endsAfterTheWholeBlock() {
        // Rosh Hashana 5787: Sat 12.9 + Sun 13.9 2026 - block ends Sunday as a holiday.
        val roshHashanaDay1 = at(LocalDate.of(2026, 9, 12), LocalTime.of(10, 0))
        val zman = calendar.infoFor(roshHashanaDay1).zman!!
        assertEquals(ZmanKind.HOLIDAY_ENDS, zman.kind)
        assertEquals(LocalDate.of(2026, 9, 13), zman.time.toLocalDate())
    }

    @Test
    fun specialShabbat_isReported() {
        // Shabbat Zachor 5787 is the Shabbat before Purim (Purim 14 Adar 5787 = 2027-03-23).
        val title = calendar.titleFor(LocalDate.of(2027, 3, 20))
        assertEquals(TitleKind.PARSHA, title!!.kind)
        assertNotNull(title.special)
    }
}
