package org.audhd.aha.domain.flowmodoro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State definitions for the Flowmodoro ADHD hyperfocus engine.
 */
sealed class FlowmodoroState {
    object Idle : FlowmodoroState()

    data class Focusing(val elapsedSeconds: Long) : FlowmodoroState() {
        val formattedTime: String
            get() = FlowmodoroEngine.formatTime(elapsedSeconds)
    }

    data class OnBreak(val remainingSeconds: Long, val totalBreakSeconds: Long) : FlowmodoroState() {
        val formattedTime: String
            get() = FlowmodoroEngine.formatTime(remainingSeconds)

        val progress: Float
            get() = if (totalBreakSeconds > 0) remainingSeconds.toFloat() / totalBreakSeconds else 0f
    }
}

/**
 * Flowmodoro count-up stopwatch engine for ADHD hyperfocus scaffolding.
 *
 * ADHD hyperfocus cannot be effectively constrained by arbitrary 25-minute countdown blocks (Pomodoro),
 * which frequently shatter fragile task-initiation momentum. Flowmodoro counts upward as long as focus
 * lasts. Once stopped, it calculates a restorative break:
 *
 *   breakSeconds = max(60, elapsedSeconds / 5)
 *
 * Allowing proportional autonomic nervous system recovery before the next focus cycle.
 */
class FlowmodoroEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _state = MutableStateFlow<FlowmodoroState>(FlowmodoroState.Idle)
    val state: StateFlow<FlowmodoroState> = _state.asStateFlow()

    private var activeTickerJob: Job? = null

    /**
     * Initiates a focus sprint counting up from 0 seconds.
     */
    fun startFocus() {
        activeTickerJob?.cancel()
        _state.value = FlowmodoroState.Focusing(elapsedSeconds = 0)

        activeTickerJob = scope.launch {
            var elapsed = 0L
            while (true) {
                delay(1000)
                elapsed += 1
                _state.value = FlowmodoroState.Focusing(elapsedSeconds = elapsed)
            }
        }
    }

    /**
     * Concludes the active focus sprint and transitions into a proportional restorative break.
     *
     * @return Calculated break duration in seconds.
     */
    fun stopFocus(): Long {
        activeTickerJob?.cancel()
        val current = _state.value
        if (current !is FlowmodoroState.Focusing) {
            _state.value = FlowmodoroState.Idle
            return 0L
        }

        val elapsed = current.elapsedSeconds
        // Proportional break calculation: 1 minute of break per 5 minutes of focus, minimum 60 seconds
        val breakDuration = calculateBreakSeconds(elapsed)
        _state.value = FlowmodoroState.OnBreak(
            remainingSeconds = breakDuration,
            totalBreakSeconds = breakDuration
        )

        activeTickerJob = scope.launch {
            var remaining = breakDuration
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _state.value = FlowmodoroState.OnBreak(
                    remainingSeconds = remaining,
                    totalBreakSeconds = breakDuration
                )
            }
            _state.value = FlowmodoroState.Idle
        }

        return breakDuration
    }

    /**
     * Resets the engine back to [FlowmodoroState.Idle].
     */
    fun reset() {
        activeTickerJob?.cancel()
        _state.value = FlowmodoroState.Idle
    }

    companion object {
        /**
         * Calculates proportional restorative break seconds from elapsed focus seconds.
         * Invariant: elapsed / 5, minimum 60s if elapsed >= 60s, else minimum 30s.
         */
        fun calculateBreakSeconds(elapsedSeconds: Long): Long {
            if (elapsedSeconds <= 0) return 0L
            val computed = elapsedSeconds / 5
            return if (elapsedSeconds >= 60) maxOf(60L, computed) else maxOf(10L, computed)
        }

        /**
         * Formats seconds into HH:MM:SS or MM:SS format.
         */
        fun formatTime(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
