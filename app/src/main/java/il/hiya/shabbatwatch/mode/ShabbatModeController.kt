package il.hiya.shabbatwatch.mode

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import il.hiya.shabbatwatch.Constants
import il.hiya.shabbatwatch.ShabbatDuration
import il.hiya.shabbatwatch.clock.ShabbatClockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which one-time adb grants are present. All of them are needed for the full experience. */
data class PermissionStatus(
    val secureSettings: Boolean,
    val dnd: Boolean,
    val overlay: Boolean,
    /** Exempt from battery optimisation, so the guard service is not stopped during Shabbat. */
    val batteryUnrestricted: Boolean,
) {
    val allGranted: Boolean get() = secureSettings && dnd && overlay && batteryUnrestricted
}

/**
 * Orchestrates activation and deactivation of Shabbat mode. Single entry point for the UI.
 * Transitions run in a process-wide scope and are non-cancellable: a UI scope would be cancelled
 * mid-way as soon as the persisted state flips and the calling composable leaves composition.
 */
class ShabbatModeController(context: Context) {
    private val appContext = context.applicationContext
    private val store = ShabbatStateStore(appContext)
    private val settings = SystemSettingsGateway(appContext)
    private val dnd = DndGateway(appContext)
    private val alarms = AutoOffScheduler(appContext)

    val state = store.state

    fun activateAsync(duration: ShabbatDuration): Job = transitionScope.launch {
        withContext(NonCancellable) { runLogged("activate") { activate(duration) } }
    }

    /** Debug builds only: activate with an auto-off a few seconds away to exercise the alarm path. */
    fun activateForTestAsync(autoOffInSeconds: Long): Job = transitionScope.launch {
        withContext(NonCancellable) {
            runLogged("activate-test") { activate(ShabbatDuration.SHABBAT, autoOffInSeconds * 1000L) }
        }
    }

    fun deactivateAsync(): Job = transitionScope.launch {
        withContext(NonCancellable) { runLogged("deactivate") { deactivate() } }
    }

    /** Restarts the guard service if the mode is active but the process was killed (or reinstalled). */
    fun ensureGuardRunningAsync(): Job = transitionScope.launch {
        if (store.read().active) ShabbatGuardService.start(appContext)
    }

    suspend fun activate(duration: ShabbatDuration, durationMillisOverride: Long? = null) {
        val existing = store.read()
        // Re-activating while active must keep the original snapshot, not the overridden values.
        val snapshot = if (existing.active) existing.savedSettings else settings.applyShabbatValues(Constants.MANAGED_SETTINGS)
        val previousDnd = if (existing.active) existing.previousDndFilter else dnd.currentFilter()
        dnd.silenceAll()
        Log.i(TAG, "activate: overrode ${snapshot.size} settings (already active: ${existing.active})")
        val now = System.currentTimeMillis()
        val endAt = durationMillisOverride?.let { now + it } ?: AutoOffScheduler.endTimeFor(now, duration)
        alarms.schedule(endAt)
        ShabbatGuardService.start(appContext)
        store.markActive(endAt, snapshot, previousDnd)
        ShabbatClockActivity.launch(appContext)
        Log.i(TAG, "activate: done, auto-off at $endAt")
    }

    suspend fun deactivate() {
        val current = store.read()
        alarms.cancel()
        ShabbatGuardService.stop(appContext)
        settings.restore(current.savedSettings)
        dnd.restore(current.previousDndFilter)
        // The clock activity observes the store and finishes itself once inactive.
        store.markInactive()
        Log.i(TAG, "deactivate: restored ${current.savedSettings.size} settings")
    }

    /** Called after a reboot: either resume the active mode or finish an expired one. */
    suspend fun resumeAfterBoot() {
        val current = store.read()
        if (!current.active) return
        if (System.currentTimeMillis() >= current.endAtMillis) {
            deactivate()
            return
        }
        alarms.schedule(current.endAtMillis)
        ShabbatGuardService.start(appContext)
        ShabbatClockActivity.launch(appContext)
    }

    fun permissionStatus(): PermissionStatus = PermissionStatus(
        secureSettings = settings.hasPermission(),
        dnd = dnd.hasAccess(),
        overlay = Settings.canDrawOverlays(appContext),
        batteryUnrestricted = appContext.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(appContext.packageName),
    )

    private suspend fun runLogged(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "$name failed", e)
        }
    }

    private companion object {
        const val TAG = "ShabbatModeController"
        val transitionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
