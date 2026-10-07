package il.hiya.shabbatwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Guards the settings that keep the clock on screen for the whole of Shabbat.
 * Found on SM-L310 (2026-10-07): in ambient, sysui returns to the watch face when the
 * "show last app" timer (20 s by default) runs out, and the guard cannot relaunch the clock
 * while dozing without waking the screen.
 */
class ManagedSettingsTest {
    private fun valueOf(namespace: SettingsNamespace, key: String): String =
        Constants.MANAGED_SETTINGS.single { it.namespace == namespace && it.key == key }.shabbatValue

    private val longestDurationMillis: Long =
        TimeUnit.HOURS.toMillis(ShabbatDuration.entries.maxOf { it.hours })

    @Test
    fun autoResumeTimeout_outlastsLongestDuration() {
        val millis = valueOf(SettingsNamespace.GLOBAL, "wear_activity_auto_resume_timeout_ms").toLong()
        assertTrue(millis > longestDurationMillis)
        assertTrue("must fit an int setting", millis <= Int.MAX_VALUE)
    }

    @Test
    fun showLastAppTimer_outlastsLongestDuration() {
        val seconds = valueOf(SettingsNamespace.GLOBAL, "setting_show_last_app_within_time").toLong()
        assertTrue(TimeUnit.SECONDS.toMillis(seconds) > longestDurationMillis)
    }

    @Test
    fun autoBrightness_isTurnedOff() {
        assertEquals("0", valueOf(SettingsNamespace.SYSTEM, "screen_brightness_mode"))
    }

    @Test
    fun everyKeyIsManagedOnce() {
        val keys = Constants.MANAGED_SETTINGS.map { it.namespace to it.key }
        assertEquals(keys.size, keys.toSet().size)
    }
}
