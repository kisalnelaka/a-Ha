package org.audhd.aha.domain.flowmodoro

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [FlowmodoroEngine] state machine invariants, proportional break arithmetic,
 * and time formatting routines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlowmodoroEngineTest {

    @Test
    fun `calculateBreakSeconds computes proportional recovery time`() {
        assertEquals(0L, FlowmodoroEngine.calculateBreakSeconds(0L))
        assertEquals(10L, FlowmodoroEngine.calculateBreakSeconds(30L))
        assertEquals(60L, FlowmodoroEngine.calculateBreakSeconds(60L))
        assertEquals(60L, FlowmodoroEngine.calculateBreakSeconds(300L)) // 5 minutes -> 1 min break
        assertEquals(300L, FlowmodoroEngine.calculateBreakSeconds(1500L)) // 25 minutes -> 5 min break
        assertEquals(600L, FlowmodoroEngine.calculateBreakSeconds(3000L)) // 50 minutes -> 10 min break
    }

    @Test
    fun `formatTime correctly formats sub-hour and multi-hour durations`() {
        assertEquals("00:00", FlowmodoroEngine.formatTime(0L))
        assertEquals("00:09", FlowmodoroEngine.formatTime(9L))
        assertEquals("01:05", FlowmodoroEngine.formatTime(65L))
        assertEquals("59:59", FlowmodoroEngine.formatTime(3599L))
        assertEquals("01:00:00", FlowmodoroEngine.formatTime(3600L))
        assertEquals("01:23:45", FlowmodoroEngine.formatTime(5025L))
    }

    @Test
    fun `startFocus transitions state to Focusing and increments elapsed time`() = runTest {
        val engine = FlowmodoroEngine(this)

        assertEquals(FlowmodoroState.Idle, engine.state.value)

        engine.startFocus()
        assertTrue(engine.state.value is FlowmodoroState.Focusing)
        assertEquals(0L, (engine.state.value as FlowmodoroState.Focusing).elapsedSeconds)

        advanceTimeBy(3500)
        assertEquals(3L, (engine.state.value as FlowmodoroState.Focusing).elapsedSeconds)
        engine.reset()
    }

    @Test
    fun `stopFocus transitions to OnBreak with proportional break calculation`() = runTest {
        val engine = FlowmodoroEngine(this)

        engine.startFocus()
        advanceTimeBy(300000) // 300 seconds (5 mins) focus

        val calculatedBreak = engine.stopFocus()
        assertEquals(60L, calculatedBreak)

        val breakState = engine.state.value as FlowmodoroState.OnBreak
        assertEquals(60L, breakState.totalBreakSeconds)
        assertEquals(60L, breakState.remainingSeconds)

        // Advance 10 seconds into break
        advanceTimeBy(10500)
        val advancedState = engine.state.value as FlowmodoroState.OnBreak
        assertEquals(50L, advancedState.remainingSeconds)

        // Reset
        engine.reset()
        assertEquals(FlowmodoroState.Idle, engine.state.value)
    }
}
