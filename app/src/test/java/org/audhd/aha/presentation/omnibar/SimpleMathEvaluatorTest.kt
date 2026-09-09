package org.audhd.aha.presentation.omnibar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimpleMathEvaluatorTest {

    @Test
    fun testBasicArithmetic() {
        assertEquals(114.0, SimpleMathEvaluator.evaluate("14 * 8 + 2")!!, 0.001)
        assertEquals(25.0, SimpleMathEvaluator.evaluate("(10 + 15)")!!, 0.001)
        assertEquals(8.0, SimpleMathEvaluator.evaluate("2 ^ 3")!!, 0.001)
        assertEquals(1.5, SimpleMathEvaluator.evaluate("3 / 2")!!, 0.001)
        assertEquals(1.0, SimpleMathEvaluator.evaluate("10 % 3")!!, 0.001)
    }

    @Test
    fun testInvalidInputHandledSafely() {
        assertNull(SimpleMathEvaluator.evaluate(""))
        assertNull(SimpleMathEvaluator.evaluate("abc"))
        assertNull(SimpleMathEvaluator.evaluate("10 / 0"))
    }
}
