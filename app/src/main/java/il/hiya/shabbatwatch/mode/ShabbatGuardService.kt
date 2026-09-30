package il.hiya.shabbatwatch.mode

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.R
import il.hiya.shabbatwatch.clock.ClockPresence
import il.hiya.shabbatwatch.clock.ShabbatClockActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process alive while Shabbat mode is active and brings the
 * clock back if it was dismissed (e.g. by the hardware home button). Relaunching from the
 * background relies on the SYSTEM_ALERT_WINDOW app-op granted via adb.
 *
 * The service notification is published as a Wear OS Ongoing Activity: Wear OS keeps an app with
 * an ongoing activity on screen in ambient mode indefinitely instead of returning to the watch face.
 */
class ShabbatGuardService : LifecycleService() {
    private val store by lazy { ShabbatStateStore(this) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private var guardJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            Constants.GUARD_NOTIFICATION_ID,
            buildOngoingNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (guardJob == null) {
            guardJob = lifecycleScope.launch { guardLoop() }
        }
        return START_STICKY
    }

    private suspend fun guardLoop() {
        while (lifecycleScope.isActive) {
            delay(Constants.GUARD_POLL_MILLIS)
            if (!store.read().active) {
                stopSelf()
                return
            }
            if (clockDismissedWhileAwake()) {
                ShabbatClockActivity.launch(this)
            }
        }
    }

    /**
     * The clock is stopped both when the user leaves it (home button) and when the display dozes.
     * Only the first case needs a relaunch: launching while dozing would wake the screen.
     */
    private fun clockDismissedWhileAwake(): Boolean =
        powerManager.isInteractive &&
            ClockPresence.hiddenForMillis() >= Constants.GUARD_RELAUNCH_GRACE_MILLIS

    private fun createChannel() {
        val channel = NotificationChannel(
            Constants.GUARD_CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildOngoingNotification(): Notification {
        val openClock = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ShabbatClockActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, Constants.GUARD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(openClock)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
        OngoingActivity.Builder(applicationContext, Constants.GUARD_NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_launcher_foreground)
            .setTouchIntent(openClock)
            .setStatus(Status.forPart(Status.TextPart(getString(R.string.notif_title))))
            .build()
            .apply(applicationContext)
        return builder.build()
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, ShabbatGuardService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShabbatGuardService::class.java))
        }
    }
}
