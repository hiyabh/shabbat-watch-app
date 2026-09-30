package il.hiya.shabbatwatch.mode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.ShabbatDuration
import java.util.concurrent.TimeUnit

/** Schedules the safety-net auto-off alarm. Manual deactivation always works regardless. */
class AutoOffScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(endAtMillis: Long) {
        val pendingIntent = pendingIntent()
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        Constants.AUTO_OFF_REQUEST_CODE,
        Intent(appContext, AutoOffReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        fun endTimeFor(nowMillis: Long, duration: ShabbatDuration): Long =
            nowMillis + TimeUnit.HOURS.toMillis(duration.hours)
    }
}
