package il.hiya.shabbatwatch

import il.hiya.shabbatwatch.clock.ShabbatTimes
import il.hiya.shabbatwatch.mode.LocationStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LocationTest {
    private fun cityNamed(name: String): ShabbatLocation = Constants.CITIES.first { it.name == name }

    @Test
    fun nothingStored_fallsBackToDefaultLocation() {
        assertEquals(Constants.LOCATION, LocationStore.resolve(null, null))
    }

    @Test
    fun unknownCity_fallsBackToDefaultLocation() {
        assertEquals(Constants.LOCATION.name, LocationStore.resolve("עיר שאינה ברשימה", null).name)
    }

    @Test
    fun storedCityWithoutMinutes_usesCityDefaultMinutes() {
        assertEquals(40, LocationStore.resolve("ירושלים", null).candleLightingMinutes)
    }

    @Test
    fun storedMinutes_overrideCityDefault() {
        val resolved = LocationStore.resolve("ירושלים", 20)
        assertEquals("ירושלים", resolved.name)
        assertEquals(20, resolved.candleLightingMinutes)
    }

    @Test
    fun minutesOutsideOfferedOptions_areIgnored() {
        assertEquals(40, LocationStore.resolve("ירושלים", 7).candleLightingMinutes)
    }

    @Test
    fun cityNamesAreUnique_andDefaultLocationIsListed() {
        assertEquals(Constants.CITIES.size, Constants.CITIES.map { it.name }.toSet().size)
        assertEquals(Constants.LOCATION, cityNamed(Constants.LOCATION.name))
    }

    @Test
    fun everyCity_producesTimes() {
        val friday = LocalDate.of(2026, 10, 9)
        Constants.CITIES.forEach { city ->
            val times = ShabbatTimes(city)
            assertNotNull(city.name, times.candleLighting(friday))
            assertNotNull(city.name, times.tzais(friday.plusDays(1)))
        }
    }

    /**
     * Hebcal (sea-level sunset, its default 40 minutes for Jerusalem), checked 2026-09-30:
     * candles 9.10.2026 17:34, Shabbat ends 10.10.2026 18:49.
     */
    @Test
    fun jerusalem_matchesPublishedTimes() {
        val times = ShabbatTimes(cityNamed("ירושלים"))
        assertEquals(LocalTime.of(17, 34), times.candleLighting(LocalDate.of(2026, 10, 9))!!.toLocalTime())
        assertEquals(LocalTime.of(18, 49), times.tzais(LocalDate.of(2026, 10, 10))!!.toLocalTime())
    }

    /**
     * Hebcal (sea-level sunset, its default 30 minutes for Haifa): candles 17:44, Shabbat ends 18:50.
     * Hebcal truncates candle lighting to the minute while this app rounds to the nearest one,
     * so candle lighting may be one minute later here.
     */
    @Test
    fun haifa_matchesPublishedTimesWithinRounding() {
        val times = ShabbatTimes(cityNamed("חיפה"))
        val candles = times.candleLighting(LocalDate.of(2026, 10, 9))!!.toLocalTime()
        assertTrue(candles == LocalTime.of(17, 44) || candles == LocalTime.of(17, 45))
        assertEquals(LocalTime.of(18, 50), times.tzais(LocalDate.of(2026, 10, 10))!!.toLocalTime())
    }
}
