package il.hiya.shabbatwatch.mode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fired by AlarmManager when the chosen duration has elapsed: safety-net deactivation. */
class AutoOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ShabbatModeController(context).deactivate()
            } finally {
                pending.finish()
            }
        }
    }
}
