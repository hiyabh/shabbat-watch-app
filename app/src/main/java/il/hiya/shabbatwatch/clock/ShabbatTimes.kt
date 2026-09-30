package il.hiya.shabbatwatch.clock

import com.kosherjava.zmanim.ZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import il.hiya.shabbatwatch.ShabbatLocation
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

/** Sunset, candle lighting and Shabbat-end (tzeit hakochavim 8.5°) for a fixed location. */
class ShabbatTimes(private val location: ShabbatLocation) {
    val zone: ZoneId = ZoneId.of(location.timeZoneId)
    private val timeZone: TimeZone = TimeZone.getTimeZone(location.timeZoneId)
    private val geoLocation = GeoLocation(
        location.name,
        location.latitude,
        location.longitude,
        location.elevationMeters,
        timeZone,
    )

    fun sunset(date: LocalDate): ZonedDateTime? = calendarFor(date).sunset.toZoned()

    fun candleLighting(date: LocalDate): ZonedDateTime? = calendarFor(date).candleLighting.toZoned()

    /** Tzeit hakochavim at 8.5 degrees - the common Israeli Shabbat-end time. */
    fun tzais(date: LocalDate): ZonedDateTime? = calendarFor(date).tzais.toZoned()

    fun isAfterSunset(now: ZonedDateTime): Boolean {
        val sunset = sunset(now.toLocalDate()) ?: return false
        return now.isAfter(sunset)
    }

    private fun calendarFor(date: LocalDate): ZmanimCalendar {
        val calendar = GregorianCalendar(timeZone).apply {
            clear()
            set(date.year, date.monthValue - 1, date.dayOfMonth)
        }
        return ZmanimCalendar(geoLocation).apply {
            this.calendar = calendar
            isUseElevation = true
            candleLightingOffset = location.candleLightingMinutes.toDouble()
        }
    }

    /** Published tables show whole minutes, rounded to the nearest one - match them. */
    private fun Date?.toZoned(): ZonedDateTime? =
        this?.toInstant()?.atZone(zone)?.plusSeconds(HALF_MINUTE_SECONDS)?.truncatedTo(ChronoUnit.MINUTES)

    private companion object {
        const val HALF_MINUTE_SECONDS = 30L
    }
}
