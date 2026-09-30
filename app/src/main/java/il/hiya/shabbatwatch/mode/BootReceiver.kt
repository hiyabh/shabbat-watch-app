package il.hiya.shabbatwatch.mode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-enters Shabbat mode after a reboot if it was active (and not yet expired). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ShabbatModeController(context).resumeAfterBoot()
            } finally {
                pending.finish()
            }
        }
    }
}
