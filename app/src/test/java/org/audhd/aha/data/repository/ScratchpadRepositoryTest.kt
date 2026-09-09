package org.audhd.aha.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests verifying [ScratchpadRepository] file operations, atomic appending, and timestamp integrity.
 */
class ScratchpadRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var repository: ScratchpadRepository

    @Before
    fun setUp() {
        tempDir = File.createTempFile("scratchpad_test", "").apply {
            delete()
            mkdir()
        }
        repository = ScratchpadRepository(context = null, baseDir = tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `appendThought appends thought with timestamp`() = runBlocking {
        val thought1 = "Remember to hydrate and take deep breaths."
        val result = repository.appendThought(thought1)
        assertTrue(result.isSuccess)

        val contents = repository.readAllThoughts()
        assertTrue(contents.contains(thought1))
        assertTrue(contents.contains("=== ["))
    }

    @Test
    fun `appendThought appends multiple thoughts consecutively without overwrite`() = runBlocking {
        repository.appendThought("Task 1: Inspect logs")
        repository.appendThought("Task 2: Fix bug")

        val contents = repository.readAllThoughts()
        assertTrue(contents.contains("Task 1: Inspect logs"))
        assertTrue(contents.contains("Task 2: Fix bug"))
    }

    @Test
    fun `appendThought ignores empty or blank entries`() = runBlocking {
        repository.appendThought("   ")
        val contents = repository.readAllThoughts()
        assertEquals("", contents)
    }

    @Test
    fun `clearDump wipes all stored thoughts`() = runBlocking {
        repository.appendThought("Temporary distraction")
        repository.clearDump()

        val contents = repository.readAllThoughts()
        assertEquals("", contents)
    }
}
