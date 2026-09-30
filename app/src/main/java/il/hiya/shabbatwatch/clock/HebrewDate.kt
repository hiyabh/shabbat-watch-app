package il.hiya.shabbatwatch.clock

import java.time.LocalDate

/**
 * Hebrew calendar date computed with the arithmetic calendar
 * (Reingold & Dershowitz, "Calendrical Calculations"). No external dependency.
 * Months are numbered from Nisan = 1 ... Adar = 12, Adar II = 13 in a leap year.
 * Note: the day is taken from the civil date, so after sunset the Hebrew date shown is still
 * the outgoing one.
 */
data class HebrewDate(val year: Int, val month: Int, val day: Int) {

    fun format(): String = "${HebrewNumerals.of(day)} ${monthName()} ${HebrewNumerals.of(year % 1000)}"

    fun monthName(): String {
        val leap = isLeapYear(year)
        return when (month) {
            12 -> if (leap) "אדר א׳" else "אדר"
            13 -> "אדר ב׳"
            else -> MONTH_NAMES[month - 1]
        }
    }

    companion object {
        private const val HEBREW_EPOCH_FIXED = -1_373_427L
        private const val FIXED_OF_UNIX_EPOCH = 719_163L
        private val MONTH_NAMES = listOf(
            "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול",
            "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר", "אדר ב׳",
        )
        private const val TISHREI = 7
        private const val NISAN = 1

        fun of(date: LocalDate): HebrewDate = fromFixed(date.toEpochDay() + FIXED_OF_UNIX_EPOCH)

        fun isLeapYear(year: Int): Boolean = Math.floorMod(7 * year + 1, 19) < 7

        private fun lastMonthOfYear(year: Int): Int = if (isLeapYear(year)) 13 else 12

        private fun elapsedDays(year: Int): Long {
            val monthsElapsed = Math.floorDiv(235L * year - 234L, 19L)
            val partsElapsed = 12_084L + 13_753L * monthsElapsed
            val day = 29L * monthsElapsed + Math.floorDiv(partsElapsed, 25_920L)
            return if (Math.floorMod(3L * (day + 1), 7L) < 3L) day + 1 else day
        }

        private fun yearLengthCorrection(year: Int): Long {
            val previous = elapsedDays(year - 1)
            val current = elapsedDays(year)
            val next = elapsedDays(year + 1)
            return when {
                next - current == 356L -> 2L
                current - previous == 382L -> 1L
                else -> 0L
            }
        }

        private fun newYear(year: Int): Long =
            HEBREW_EPOCH_FIXED + elapsedDays(year) + yearLengthCorrection(year)

        private fun daysInYear(year: Int): Long = newYear(year + 1) - newYear(year)

        fun lastDayOfMonth(month: Int, year: Int): Int {
            val yearLength = daysInYear(year)
            val shortMonth = when (month) {
                2, 4, 6, 10, 13 -> true
                12 -> !isLeapYear(year)
                8 -> yearLength != 355L && yearLength != 385L
                9 -> yearLength == 353L || yearLength == 383L
                else -> false
            }
            return if (shortMonth) 29 else 30
        }

        private fun toFixed(year: Int, month: Int, day: Int): Long {
            var fixed = newYear(year) + day - 1
            if (month < TISHREI) {
                for (m in TISHREI..lastMonthOfYear(year)) fixed += lastDayOfMonth(m, year)
                for (m in NISAN until month) fixed += lastDayOfMonth(m, year)
            } else {
                for (m in TISHREI until month) fixed += lastDayOfMonth(m, year)
            }
            return fixed
        }

        private fun fromFixed(fixed: Long): HebrewDate {
            val approx = Math.floorDiv((fixed - HEBREW_EPOCH_FIXED) * 98_496L, 35_975_000L).toInt() + 1
            var year = approx - 1
            while (newYear(year + 1) <= fixed) year++
            val start = if (fixed < toFixed(year, NISAN, 1)) TISHREI else NISAN
            var month = start
            while (fixed > toFixed(year, month, lastDayOfMonth(month, year))) month++
            val day = (fixed - toFixed(year, month, 1) + 1).toInt()
            return HebrewDate(year, month, day)
        }
    }
}

/** Hebrew letter numerals (gematria) with the customary ט״ו / ט״ז forms. */
object HebrewNumerals {
    private val ONES = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
    private val TENS = listOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
    private val HUNDREDS = listOf("", "ק", "ר", "ש", "ת", "תק", "תר", "תש", "תת", "תתק")
    private const val GERESH = "׳"
    private const val GERSHAYIM = "״"

    fun of(number: Int): String {
        require(number in 1..999) { "Supported range is 1..999, got $number" }
        val letters = lettersOf(number)
        return if (letters.length == 1) letters + GERESH
        else letters.dropLast(1) + GERSHAYIM + letters.last()
    }

    private fun lettersOf(number: Int): String {
        val hundreds = HUNDREDS[number / 100]
        val remainder = number % 100
        val tensAndOnes = when (remainder) {
            15 -> "טו"
            16 -> "טז"
            else -> TENS[remainder / 10] + ONES[remainder % 10]
        }
        return hundreds + tensAndOnes
    }
}
