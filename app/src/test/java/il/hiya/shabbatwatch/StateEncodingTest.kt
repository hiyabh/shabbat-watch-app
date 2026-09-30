package il.hiya.shabbatwatch

import il.hiya.shabbatwatch.mode.AutoOffScheduler
import il.hiya.shabbatwatch.mode.ShabbatStateStore
import il.hiya.shabbatwatch.mode.SystemSettingsGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StateEncodingTest {

    @Test
    fun settingsSnapshotRoundTrips() {
        val snapshot = mapOf("GLOBAL:ambient_enabled" to "1", "SECURE:some_key" to "a=b")
        val decoded = ShabbatStateStore.decodeSettings(ShabbatStateStore.encodeSettings(snapshot))
        assertEquals(snapshot, decoded)
    }

    @Test
    fun emptySnapshotDecodesToEmptyMap() {
        assertEquals(emptyMap<String, String>(), ShabbatStateStore.decodeSettings(""))
    }

    @Test
    fun snapshotKeyParsesNamespaceAndKey() {
        val parsed = SystemSettingsGateway.parseSnapshotKey("GLOBAL:ambient_tilt_to_wake")
        assertEquals(SettingsNamespace.GLOBAL to "ambient_tilt_to_wake", parsed)
        assertNull(SystemSettingsGateway.parseSnapshotKey("bogus"))
    }

    @Test
    fun endTimeAddsWholeHours() {
        val now = 1_000L
        val hourMillis = 3_600_000L
        assertEquals(now + 26 * hourMillis, AutoOffScheduler.endTimeFor(now, ShabbatDuration.SHABBAT))
        assertEquals(now + 74 * hourMillis, AutoOffScheduler.endTimeFor(now, ShabbatDuration.THREE_DAYS))
    }
}
