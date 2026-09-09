package org.audhd.aha.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundScapeEngineTest {

    @Test
    fun verifyAllNoiseTypesConfigured() {
        val types = NoiseType.values()
        assertEquals(6, types.size)
        assertTrue(types.contains(NoiseType.RAIN))
        assertTrue(types.contains(NoiseType.BINAURAL_GAMMA))
        assertTrue(types.contains(NoiseType.BINAURAL_BETA))
        assertTrue(types.contains(NoiseType.BROWN))
        assertTrue(types.contains(NoiseType.PINK))
        assertTrue(types.contains(NoiseType.WHITE))
    }

    @Test
    fun verifyDisplayNames() {
        assertTrue(NoiseType.RAIN.displayName.contains("Rainfall"))
        assertTrue(NoiseType.BINAURAL_GAMMA.displayName.contains("Gamma"))
        assertTrue(NoiseType.BINAURAL_BETA.displayName.contains("Beta"))
        assertTrue(NoiseType.BROWN.displayName.contains("Brown"))
    }

    @Test
    fun verifySampleRateAndBufferConstants() {
        assertEquals(44100, SoundScapeEngine.SAMPLE_RATE)
        assertEquals(4096, SoundScapeEngine.BUFFER_CHUNK_SIZE)
    }
}
