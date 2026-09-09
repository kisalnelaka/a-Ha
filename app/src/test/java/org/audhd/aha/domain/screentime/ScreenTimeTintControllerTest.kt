package org.audhd.aha.domain.screentime

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeTintControllerTest {

    private val controller = ScreenTimeTintController(null)


    @Test
    fun computeCircadianTint_underThreshold_isTransparent() {
        val duration30Min = 30 * 60 * 1000L
        val tint = controller.computeCircadianTint(duration30Min)
        assertEquals(Color.Transparent, tint)
    }

    @Test
    fun computeCircadianTint_atThreshold_isTransparent() {
        val duration45Min = 45 * 60 * 1000L
        val tint = controller.computeCircadianTint(duration45Min)
        assertEquals(0f, tint.alpha, 0.01f)
    }

    @Test
    fun computeCircadianTint_at60Min_reachesMaxAlpha() {
        val duration60Min = 60 * 60 * 1000L
        val tint = controller.computeCircadianTint(duration60Min)
        assertTrue(tint.alpha > 0.30f)
        assertEquals(ScreenTimeTintController.MAX_TINT_ALPHA, tint.alpha, 0.01f)
    }

    @Test
    fun computeCircadianTint_at52Min_interpolatesAlpha() {
        val duration52Min = (45 + 7.5) * 60 * 1000L
        val tint = controller.computeCircadianTint(duration52Min.toLong())
        assertEquals(ScreenTimeTintController.MAX_TINT_ALPHA / 2f, tint.alpha, 0.02f)
    }
}
