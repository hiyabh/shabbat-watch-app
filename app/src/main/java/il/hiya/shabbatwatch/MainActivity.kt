package il.hiya.shabbatwatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import il.hiya.shabbatwatch.mode.ShabbatModeController
import il.hiya.shabbatwatch.ui.HomeScreen
import il.hiya.shabbatwatch.ui.theme.ShabbatWatchTheme

class MainActivity : ComponentActivity() {
    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationPermission()
        val controller = ShabbatModeController(this)
        controller.ensureGuardRunningAsync()
        handleDebugAutoOffTest(controller)
        setContent {
            ShabbatWatchTheme { HomeScreen(controller = controller) }
        }
    }

    /**
     * QA hook, debug builds only:
     * `adb shell am start -n il.hiya.shabbatwatch/.MainActivity --el debug_auto_off_seconds 90`
     */
    private fun handleDebugAutoOffTest(controller: ShabbatModeController) {
        if (!BuildConfig.DEBUG) return
        val seconds = intent.getLongExtra(EXTRA_DEBUG_AUTO_OFF_SECONDS, 0L)
        if (seconds > 0L) controller.activateForTestAsync(seconds)
    }

    /** Needed on Android 13+ for the guard service's ongoing notification to be visible. */
    private fun ensureNotificationPermission() {
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val EXTRA_DEBUG_AUTO_OFF_SECONDS = "debug_auto_off_seconds"
