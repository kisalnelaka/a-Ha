package org.audhd.aha.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundScapeEngineTest {

    @Test
    fun verifyAllNoiseTypesConfigured() {
        val types = NoiseType.values()
        assertEquals(4, types.size)
        assertTrue(types.contains(NoiseType.RAIN))
        assertTrue(types.contains(NoiseType.BROWN))
        assertTrue(types.contains(NoiseType.PINK))
        assertTrue(types.contains(NoiseType.WHITE))
    }

    @Test
    fun verifyRainDisplayName() {
        val rain = NoiseType.RAIN
        assertNotNull(rain.displayName)
        assertTrue(rain.displayName.contains("Rainfall"))
    }

    @Test
    fun verifySampleRateAndBufferConstants() {
        assertEquals(44100, SoundScapeEngine.SAMPLE_RATE)
        assertEquals(4096, SoundScapeEngine.BUFFER_CHUNK_SIZE)
    }
}
