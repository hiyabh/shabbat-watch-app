package il.hiya.shabbatwatch.clock

import android.os.SystemClock

/** Process-wide record of whether the clock activity is currently on screen. */
object ClockPresence {
    @Volatile
    private var visible: Boolean = false

    @Volatile
    private var hiddenSinceMillis: Long = SystemClock.elapsedRealtime()

    fun markVisible() {
        visible = true
    }

    fun markHidden() {
        visible = false
        hiddenSinceMillis = SystemClock.elapsedRealtime()
    }

    /** 0 while visible; otherwise how long the clock has been off screen. */
    fun hiddenForMillis(): Long =
        if (visible) 0L else SystemClock.elapsedRealtime() - hiddenSinceMillis
}
