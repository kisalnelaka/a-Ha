package org.audhd.aha.domain.typography

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying [FixationPointParser] logic, edge cases, and performance invariants.
 */
class FixationPointParserTest {

    @Test
    fun `calculateFixationLength computes 40 percent ceiling across word lengths`() {
        assertEquals(0, FixationPointParser.calculateFixationLength(0))
        assertEquals(1, FixationPointParser.calculateFixationLength(1))
        assertEquals(1, FixationPointParser.calculateFixationLength(2)) // ceil(0.8) = 1
        assertEquals(2, FixationPointParser.calculateFixationLength(3)) // ceil(1.2) = 2
        assertEquals(2, FixationPointParser.calculateFixationLength(4)) // ceil(1.6) = 2
        assertEquals(2, FixationPointParser.calculateFixationLength(5)) // ceil(2.0) = 2
        assertEquals(3, FixationPointParser.calculateFixationLength(6)) // ceil(2.4) = 3
        assertEquals(3, FixationPointParser.calculateFixationLength(7)) // ceil(2.8) = 3
        assertEquals(4, FixationPointParser.calculateFixationLength(8)) // ceil(3.2) = 4
        assertEquals(4, FixationPointParser.calculateFixationLength(9)) // ceil(3.6) = 4
        assertEquals(4, FixationPointParser.calculateFixationLength(10)) // ceil(4.0) = 4
    }

    @Test
    fun `parse on empty string returns empty annotated string with zero spans`() {
        val result = FixationPointParser.parse("")
        assertEquals("", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `parse on whitespace only returns plain text with zero spans`() {
        val result = FixationPointParser.parse("   \n\t  ")
        assertEquals("   \n\t  ", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `parse on single character words bolds the character`() {
        val result = FixationPointParser.parse("I a")
        assertEquals("I a", result.text)
        assertEquals(2, result.spanStyles.size)

        // "I" at index 0..1
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(1, result.spanStyles[0].end)
        assertEquals(FontWeight.Bold, result.spanStyles[0].item.fontWeight)

        // "a" at index 2..3
        assertEquals(2, result.spanStyles[1].start)
        assertEquals(3, result.spanStyles[1].end)
        assertEquals(FontWeight.Bold, result.spanStyles[1].item.fontWeight)
    }

    @Test
    fun `parse bolds correct fixation anchors across multiple words`() {
        val text = "The quick brown fox"
        val result = FixationPointParser.parse(text)

        assertEquals(text, result.text)
        assertEquals(4, result.spanStyles.size)

        // "The" (len 3 -> 2 chars): [0, 2)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)

        // "quick" (len 5 -> 2 chars): [4, 6)
        assertEquals(4, result.spanStyles[1].start)
        assertEquals(6, result.spanStyles[1].end)

        // "brown" (len 5 -> 2 chars): [10, 12)
        assertEquals(10, result.spanStyles[2].start)
        assertEquals(12, result.spanStyles[2].end)

        // "fox" (len 3 -> 2 chars): [16, 18)
        assertEquals(16, result.spanStyles[3].start)
        assertEquals(18, result.spanStyles[3].end)
    }

    @Test
    fun `parse isolates punctuation from word tokens cleanly`() {
        val text = "Don't panic (yet)!"
        val result = FixationPointParser.parse(text)

        assertEquals(text, result.text)
        // Tokens: "Don", "t", "panic", "yet"
        assertEquals(4, result.spanStyles.size)

        // "Don" (len 3 -> 2): [0, 2)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)

        // "t" (len 1 -> 1): [4, 5)
        assertEquals(4, result.spanStyles[1].start)
        assertEquals(5, result.spanStyles[1].end)

        // "panic" (len 5 -> 2): [6, 8)
        assertEquals(6, result.spanStyles[2].start)
        assertEquals(8, result.spanStyles[2].end)

        // "yet" (len 3 -> 2): [13, 15)
        assertEquals(13, result.spanStyles[3].start)
        assertEquals(15, result.spanStyles[3].end)
    }

    @Test
    fun `parse hyphenated compound words anchors each constituent`() {
        val text = "AuDHD-friendly"
        val result = FixationPointParser.parse(text)

        assertEquals(text, result.text)
        assertEquals(2, result.spanStyles.size)

        // "AuDHD" (len 5 -> 2): [0, 2)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)

        // "friendly" (len 8 -> 4): [6, 10)
        assertEquals(6, result.spanStyles[1].start)
        assertEquals(10, result.spanStyles[1].end)
    }

    @Test
    fun `parse preserves existing annotated string styles`() {
        val original = buildAnnotatedString {
            append("Focus Mode")
            addStyle(SpanStyle(color = Color.Red), 0, 5)
        }

        val parsed = FixationPointParser.parse(original)

        assertEquals("Focus Mode", parsed.text)
        // Should contain original red style + 2 fixation bold styles
        assertEquals(3, parsed.spanStyles.size)

        // Original style preserved
        val colorSpan = parsed.spanStyles.first { it.item.color == Color.Red }
        assertEquals(0, colorSpan.start)
        assertEquals(5, colorSpan.end)

        // Fixation bold spans exist
        val boldSpans = parsed.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, boldSpans.size)
        // "Focus" (len 5 -> 2): [0, 2)
        assertEquals(0, boldSpans[0].start)
        assertEquals(2, boldSpans[0].end)
        // "Mode" (len 4 -> 2): [6, 8)
        assertEquals(6, boldSpans[1].start)
        assertEquals(8, boldSpans[1].end)
    }

    @Test
    fun `extension function toFixationPoint mirrors parser output`() {
        val text = "Executive Function"
        val parsed = text.toFixationPoint()
        assertEquals(2, parsed.spanStyles.size)
        assertEquals(0, parsed.spanStyles[0].start)
        assertEquals(4, parsed.spanStyles[0].end) // "Executive" len 9 -> ceil(3.6) = 4
    }

    @Test
    fun `parser throughput benchmark satisfies 90Hz frame time budget`() {
        val sampleText = "The quick brown fox jumps over the lazy dog in a hyperfocus Flowmodoro sprint."
        
        // Warmup
        repeat(100) {
            FixationPointParser.parse(sampleText)
        }

        val startTime = System.nanoTime()
        val iterations = 1000
        repeat(iterations) {
            FixationPointParser.parse(sampleText)
        }
        val elapsedMillis = (System.nanoTime() - startTime) / 1_000_000.0

        // 1000 iterations must easily finish in under 150ms on modern JVM (sub-0.15ms per line)
        assertTrue("Elapsed time was ${elapsedMillis}ms for $iterations iterations", elapsedMillis < 150.0)
    }
}
