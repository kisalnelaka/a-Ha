package org.audhd.aha.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository orchestrating instant plain-text thought persistence for the Working Memory Scratchpad.
 *
 * Invariants:
 * - Commits thoughts directly to a plain-text append-only log without database or transaction overhead.
 * - Operates safely off the UI thread via [Dispatchers.IO].
 * - Prepend ISO-8601-formatted timestamps to every committed block.
 */
class ScratchpadRepository(
    private val context: Context? = null,
    private val baseDir: File = context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
) {

    private val dumpFile: File
        get() = File(baseDir, DUMP_FILE_NAME)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Commits a thought entry to the persistent `.txt` dump.
     */
    suspend fun appendThought(thought: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = thought.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(Unit)

        runCatching {
            val timestamp = dateFormat.format(Date())
            val formattedEntry = buildString {
                append("=== [")
                append(timestamp)
                append("] ===\n")
                append(trimmed)
                append("\n\n")
            }

            FileOutputStream(dumpFile, true).use { output ->
                output.write(formattedEntry.toByteArray(Charsets.UTF_8))
                output.flush()
            }
        }
    }

    /**
     * Reads all accumulated notes from the plain-text scratchpad dump.
     */
    suspend fun readAllThoughts(): String = withContext(Dispatchers.IO) {
        if (!dumpFile.exists()) return@withContext ""
        runCatching {
            dumpFile.readText(Charsets.UTF_8)
        }.getOrDefault("")
    }

    /**
     * Clears all content from the scratchpad dump.
     */
    suspend fun clearDump(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (dumpFile.exists()) {
                dumpFile.writeText("")
            }
            Unit
        }
    }

    companion object {
        const val DUMP_FILE_NAME = "scratchpad_dump.txt"
    }
}
