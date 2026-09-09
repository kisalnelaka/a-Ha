package org.audhd.aha.domain.decomposer

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDecomposerEngineTest {

    private val engine = TaskDecomposerEngine()

    @Test
    fun computeHash_producesConsistentSha256() {
        val hash1 = TaskDecomposerEngine.computeHash("Clean Bedroom")
        val hash2 = TaskDecomposerEngine.computeHash("  clean bedroom  ")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun parseJsonSteps_validJsonArray_returnsStringList() {
        val json = """["Pick up 3 cups", "Put in sink", "Wipe counter"]"""
        val steps = engine.parseJsonSteps(json)
        assertEquals(3, steps.size)
        assertEquals("Pick up 3 cups", steps[0])
        assertEquals("Put in sink", steps[1])
        assertEquals("Wipe counter", steps[2])
    }

    @Test
    fun parseJsonSteps_invalidJson_returnsEmptyList() {
        val invalid = "Not a json array"
        val steps = engine.parseJsonSteps(invalid)
        assertTrue(steps.isEmpty())
    }

    @Test
    fun generateOfflineMicroSteps_cleaningTask_returnsCleaningPhysicalActions() {
        val steps = engine.generateOfflineMicroSteps("Clean the living room")
        assertTrue(steps.size in 3..5)
        assertTrue(steps.any { it.contains("trash", ignoreCase = true) })
    }

    @Test
    fun generateOfflineMicroSteps_laundryTask_returnsLaundryActions() {
        val steps = engine.generateOfflineMicroSteps("Do the laundry")
        assertTrue(steps.size in 3..5)
        assertTrue(steps.any { it.contains("laundry", ignoreCase = true) || it.contains("machine", ignoreCase = true) })
    }

    @Test
    fun generateOfflineMicroSteps_emailTask_returnsLowCognitiveEmailSteps() {
        val steps = engine.generateOfflineMicroSteps("Check work email")
        assertTrue(steps.size in 3..5)
        assertTrue(steps.any { it.contains("email", ignoreCase = true) || it.contains("archive", ignoreCase = true) })
    }

    @Test
    fun generateOfflineMicroSteps_genericTask_returnsSafePhysicalGatheringSteps() {
        val steps = engine.generateOfflineMicroSteps("Organize workbench")
        assertTrue(steps.size in 3..5)
        assertTrue(steps.any { it.contains("Gather", ignoreCase = true) })
    }

    @Test
    fun decompose_withoutKeys_usesOfflineHeuristicSuccessfully() = runTest {
        val result = engine.decompose("Wash the dishes")
        assertFalse(result.isEmpty())
        assertTrue(result.any { it.contains("sink", ignoreCase = true) || it.contains("utensil", ignoreCase = true) })
    }
}
