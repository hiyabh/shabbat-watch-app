package il.hiya.shabbatwatch.mode

import android.app.NotificationManager
import android.content.Context
import android.util.Log

/**
 * Do-not-disturb control. Requires notification policy access, granted once via adb:
 * `cmd notification allow_dnd il.hiya.shabbatwatch` (scripts/watch-grant.ps1).
 */
class DndGateway(context: Context) {
    private val notificationManager =
        context.applicationContext.getSystemService(NotificationManager::class.java)

    fun hasAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun currentFilter(): Int = notificationManager.currentInterruptionFilter

    /** Silences everything (no sounds, no vibration, no visual interruptions). */
    fun silenceAll(): Boolean = setFilter(NotificationManager.INTERRUPTION_FILTER_NONE)

    fun restore(previousFilter: Int): Boolean {
        val target = if (previousFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            previousFilter
        }
        return setFilter(target)
    }

    private fun setFilter(filter: Int): Boolean {
        if (!hasAccess()) {
            Log.w(TAG, "Notification policy access missing - DND unchanged")
            return false
        }
        notificationManager.setInterruptionFilter(filter)
        return true
    }

    private companion object {
        const val TAG = "DndGateway"
    }
}
