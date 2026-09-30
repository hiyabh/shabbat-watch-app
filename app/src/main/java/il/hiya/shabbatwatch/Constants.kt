package il.hiya.shabbatwatch

/** Durations offered on the home screen. Hours are generous on purpose (safety net only). */
enum class ShabbatDuration(val hours: Long) {
    SHABBAT(26),
    SHABBAT_AND_YOM_TOV(50),
    THREE_DAYS(74),
}

/** Namespace of a system setting as used by `adb shell settings`. */
enum class SettingsNamespace { GLOBAL, SECURE, SYSTEM }

/**
 * A system setting that Shabbat mode overrides while active.
 * [shabbatValue] is written on activation; the original value is saved and restored on deactivation.
 * Keys that do not exist on the device are skipped (see SystemSettingsGateway).
 */
data class ManagedSetting(val namespace: SettingsNamespace, val key: String, val shabbatValue: String)

/** Fixed place used for candle lighting / Shabbat end times and the after-sunset Hebrew date. */
data class ShabbatLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
    val timeZoneId: String,
    /** Local custom: minutes before sunset for candle lighting. */
    val candleLightingMinutes: Int,
)

private const val ISRAEL_TIME_ZONE = "Asia/Jerusalem"
private const val DEFAULT_CANDLE_MINUTES = 20
private const val JERUSALEM_CANDLE_MINUTES = 40
private const val HAIFA_CANDLE_MINUTES = 30

private fun city(
    name: String,
    latitude: Double,
    longitude: Double,
    elevationMeters: Double,
    candleLightingMinutes: Int = DEFAULT_CANDLE_MINUTES,
) = ShabbatLocation(name, latitude, longitude, elevationMeters, ISRAEL_TIME_ZONE, candleLightingMinutes)

object Constants {
    const val PACKAGE_NAME = "il.hiya.shabbatwatch"

    /** Interactive-mode clock refresh. Ambient refresh is driven by the system (once per minute). */
    const val CLOCK_TICK_MILLIS = 1_000L

    /** Small pixel shift per minute in ambient mode - burn-in protection on AMOLED. */
    const val BURN_IN_SHIFT_DP = 2

    /** How often the guard service checks that the clock is still on screen. */
    const val GUARD_POLL_MILLIS = 3_000L

    /** Grace period after the clock leaves the screen before the guard relaunches it. */
    const val GUARD_RELAUNCH_GRACE_MILLIS = 2_000L

    const val GUARD_NOTIFICATION_ID = 1
    const val GUARD_CHANNEL_ID = "shabbat_guard"
    const val AUTO_OFF_REQUEST_CODE = 100

    /**
     * Modi'in, Israel (coordinates from public sources, 2026-09-27). Candle lighting 20 minutes
     * before sunset by the owner's choice (published Modi'in tables use 30).
     */
    val LOCATION = ShabbatLocation(
        name = "מודיעין",
        latitude = 31.8903,
        longitude = 35.0104,
        elevationMeters = 209.0,
        timeZoneId = "Asia/Jerusalem",
        candleLightingMinutes = 20,
    )

    /**
     * Cities offered on the home screen. Coordinates and elevation from GeoNames via the Hebcal
     * API (2026-09-30); [LOCATION] keeps its original values. Candle-lighting minutes are only
     * the default applied when the city is picked - the user can change them afterwards.
     * Tiberias is below sea level; KosherJava rejects negative elevation, so 0 is used.
     */
    val CITIES: List<ShabbatLocation> = listOf(
        city("ירושלים", 31.76904, 35.21633, 786.0, JERUSALEM_CANDLE_MINUTES),
        city("תל אביב", 32.08088, 34.78057, 15.0),
        city("חיפה", 32.81303, 34.99928, 101.0, HAIFA_CANDLE_MINUTES),
        LOCATION,
        city("באר שבע", 31.25181, 34.7913, 285.0),
        city("בני ברק", 32.08074, 34.8338, 51.0),
        city("פתח תקווה", 32.08707, 34.88747, 54.0),
        city("ראשון לציון", 31.97102, 34.78939, 56.0),
        city("אשדוד", 31.79213, 34.64966, 27.0),
        city("אשקלון", 31.66926, 34.57149, 42.0),
        city("בית שמש", 31.73072, 34.99293, 282.0),
        city("נתניה", 32.33294, 34.85917, 38.0),
        city("רחובות", 31.89421, 34.81199, 47.0),
        city("רעננה", 32.1836, 34.87386, 49.0),
        city("כפר סבא", 32.175, 34.90694, 57.0),
        city("הרצליה", 32.16627, 34.82536, 28.0),
        city("חולון", 32.01034, 34.77918, 31.0),
        city("רמת גן", 32.08227, 34.81065, 57.0),
        city("לוד", 31.9467, 34.8903, 68.0),
        city("חדרה", 32.44192, 34.9039, 13.0),
        city("טבריה", 32.79396, 35.53152, 0.0),
        city("צפת", 32.96465, 35.496, 779.0),
        city("אילת", 29.55805, 34.94821, 63.0),
    )

    /** Minutes before sunset offered for candle lighting. */
    val CANDLE_MINUTES_OPTIONS: List<Int> = listOf(18, 20, 22, 30, 40)

    /** Installation and permissions guide shown when the adb grants are missing. */
    const val GUIDE_URL = "github.com/hiyabh/shabbat-watch-app"

    /** Shabbat end: "tzeit hakochavim" at 8.5 degrees below the horizon (ZmanimCalendar.getTzais). */
    const val LOOKAHEAD_DAYS_FOR_NEXT_HOLY_DAY = 8

    /**
     * Wear OS global settings (AOSP `Settings.Global.Wear`) that control wake gestures and the
     * always-on display. Samsung One UI Watch is built on Wear OS and is expected to honour them;
     * scripts/discover-settings.ps1 confirms the exact keys on the real watch.
     */
    val MANAGED_SETTINGS: List<ManagedSetting> = listOf(
        // Always-on display must stay on so the clock remains visible in ambient mode.
        ManagedSetting(SettingsNamespace.GLOBAL, "ambient_enabled", "1"),
        // Wake gestures (AOSP Wear keys, confirmed present on One UI 8 Watch / SM-L310).
        ManagedSetting(SettingsNamespace.GLOBAL, "ambient_tilt_to_wake", "0"),
        ManagedSetting(SettingsNamespace.GLOBAL, "ambient_touch_to_wake", "0"),
        ManagedSetting(SettingsNamespace.GLOBAL, "ambient_tilt_to_bright", "0"),
        ManagedSetting(SettingsNamespace.GLOBAL, "psm_ambient_tilt_to_wake", "0"),
        // Samsung-specific: turning the bezel wakes the screen.
        ManagedSetting(SettingsNamespace.GLOBAL, "setting_wake_by_turning_bezel", "0"),
        // Secure namespace wake gestures.
        ManagedSetting(SettingsNamespace.SECURE, "wake_gesture_enabled", "0"),
        ManagedSetting(SettingsNamespace.SECURE, "double_tap_to_wake", "0"),
    )
}
