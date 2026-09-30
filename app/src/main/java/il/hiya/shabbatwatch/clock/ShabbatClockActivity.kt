package il.hiya.shabbatwatch.clock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import il.hiya.shabbatwatch.mode.ShabbatStateStore
import kotlinx.coroutines.launch

/**
 * Full-screen clock shown for the whole of Shabbat.
 * - Supports ambient mode so the system keeps it on screen, dimmed, updating once a minute.
 * - Swallows every touch so nothing on screen reacts to the wearer.
 * - Finishes itself as soon as the store reports Shabbat mode inactive.
 */
class ShabbatClockActivity : ComponentActivity() {
    private val isAmbient = mutableStateOf(false)
    private val ambientTick = mutableIntStateOf(0)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient.value = true
        }

        override fun onUpdateAmbient() {
            ambientTick.intValue++
        }

        override fun onExitAmbient() {
            isAmbient.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        setContent { ClockScreen(isAmbient = isAmbient.value, ambientTick = ambientTick.intValue) }
        finishWhenDeactivated()
    }

    override fun onStart() {
        super.onStart()
        ClockPresence.markVisible()
    }

    override fun onStop() {
        ClockPresence.markHidden()
        super.onStop()
    }

    /** Consume every touch: the screen must not react to the wearer. */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = true

    private fun finishWhenDeactivated() {
        val store = ShabbatStateStore(this)
        lifecycleScope.launch {
            store.state.collect { state -> if (!state.active) finish() }
        }
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, ShabbatClockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }
}
