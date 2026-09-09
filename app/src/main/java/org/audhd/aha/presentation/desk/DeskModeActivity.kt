package org.audhd.aha.presentation.desk

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.audhd.aha.domain.flowmodoro.FlowmodoroEngine
import org.audhd.aha.presentation.theme.AHaTheme

/**
 * Native fullscreen immersive Desk Mode Activity.
 *
 * Designed for bedside docks and desktop phone stands:
 * - Keeps screen awake persistently via [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON].
 * - Completely hides system bars for pure AMOLED immersion.
 * - Houses the brutalist StandBy clock, Flowmodoro sprint engine, and circadian night filter.
 */
class DeskModeActivity : ComponentActivity() {

    private val flowmodoroEngine = FlowmodoroEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive fullscreen mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            AHaTheme {
                DeskModeScreen(
                    flowmodoroEngine = flowmodoroEngine,
                    onExit = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        flowmodoroEngine.reset()
    }
}
