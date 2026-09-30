package il.hiya.shabbatwatch.clock

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import il.hiya.shabbatwatch.Constants
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

enum class TitleKind { PARSHA, HOLIDAY }

/** What the day is called: a parsha (with an optional special Shabbat) or a holiday. */
data class DayTitle(val kind: TitleKind, val name: String, val special: String? = null)

enum class ZmanKind { CANDLE_LIGHTING, SHABBAT_ENDS, HOLIDAY_ENDS }

data class ZmanLine(val kind: ZmanKind, val time: ZonedDateTime)

/** Everything the clock shows besides the time. */
data class ShabbatInfo(val hebrewDate: HebrewDate, val title: DayTitle?, val zman: ZmanLine?)

/**
 * Israel-mode Jewish calendar logic on top of KosherJava: the Hebrew date (advancing after
 * sunset), the parsha or holiday name, and the next relevant candle-lighting / end time.
 */
class ShabbatCalendar(private val times: ShabbatTimes) {
    private val formatter = HebrewDateFormatter().apply { isHebrewFormat = true }

    fun infoFor(now: ZonedDateTime): ShabbatInfo {
        val date = effectiveDate(now)
        return ShabbatInfo(
            hebrewDate = HebrewDate.of(date),
            title = titleFor(date),
            zman = zmanFor(date),
        )
    }

    /** The Jewish day starts at sunset: after sunset, everything refers to tomorrow. */
    fun effectiveDate(now: ZonedDateTime): LocalDate {
        val today = now.withZoneSameInstant(times.zone).toLocalDate()
        return if (times.isAfterSunset(now)) today.plusDays(1) else today
    }

    fun titleFor(date: LocalDate): DayTitle? {
        val today = jewish(date)
        if (today.isCholHamoed || today.isYomTovAssurBemelacha) return holidayTitle(today)
        if (today.parshah != JewishCalendar.Parsha.NONE) return parshaTitle(today)
        return nextHolyDay(date)?.let { titleFor(it) }
    }

    fun zmanFor(date: LocalDate): ZmanLine? {
        if (isHoly(date)) {
            var last = date
            while (isHoly(last.plusDays(1))) last = last.plusDays(1)
            val kind = if (last.dayOfWeek == DayOfWeek.SATURDAY) ZmanKind.SHABBAT_ENDS else ZmanKind.HOLIDAY_ENDS
            return times.tzais(last)?.let { ZmanLine(kind, it) }
        }
        val holyDay = nextHolyDay(date) ?: return null
        return times.candleLighting(holyDay.minusDays(1))?.let { ZmanLine(ZmanKind.CANDLE_LIGHTING, it) }
    }

    fun isHoly(date: LocalDate): Boolean = jewish(date).isAssurBemelacha

    private fun nextHolyDay(from: LocalDate): LocalDate? =
        (1..Constants.LOOKAHEAD_DAYS_FOR_NEXT_HOLY_DAY)
            .map { from.plusDays(it.toLong()) }
            .firstOrNull { isHoly(it) }

    private fun holidayTitle(day: JewishCalendar): DayTitle {
        val name = if (day.yomTovIndex == JewishCalendar.SHEMINI_ATZERES) {
            SHEMINI_ATZERES_ISRAEL
        } else {
            formatter.formatYomTov(day)
        }
        return DayTitle(TitleKind.HOLIDAY, name)
    }

    private fun parshaTitle(day: JewishCalendar): DayTitle {
        val special = day.specialShabbos
            .takeIf { it != JewishCalendar.Parsha.NONE }
            ?.let { formatter.formatSpecialParsha(day) }
        return DayTitle(TitleKind.PARSHA, formatter.formatParsha(day), special)
    }

    private fun jewish(date: LocalDate): JewishCalendar =
        JewishCalendar(date).apply { inIsrael = true }

    private companion object {
        /** In Israel the 22nd of Tishrei is both; the common name is Simchat Torah. */
        const val SHEMINI_ATZERES_ISRAEL = "שמיני עצרת ושמחת תורה"
    }
}
