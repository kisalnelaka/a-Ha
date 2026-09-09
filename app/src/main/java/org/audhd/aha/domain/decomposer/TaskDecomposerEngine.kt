package org.audhd.aha.domain.decomposer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.audhd.aha.data.local.dao.TaskDao
import org.audhd.aha.data.security.AIProvider
import org.audhd.aha.data.security.KeystoreManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * High-leverage task decomposer ("Goblin Mode") designed specifically for AuDHD executive dysfunction.
 * Translates ambiguous, overwhelming demands into 3-5 concrete, low-friction physical micro-actions.
 * Enforces a 4-tier resilience hierarchy:
 * 1. Content-hash SQLite cache hit (instant 0ms)
 * 2. BYOK AI call via Groq/OpenRouter/Gemini (direct on-device HTTPS, 0 telemetry)
 * 3. Offline heuristic rule-based breakdown engine (0 network dependency, 100% reliable)
 */
class TaskDecomposerEngine(
    private val taskDao: TaskDao? = null,
    private val keystoreManager: KeystoreManager? = null
) {

    companion object {
        const val SYSTEM_PROMPT =
            "You are an AuDHD executive functioning assistant. Your job is to defeat task paralysis. " +
            "Decompose the user's task into 3 to 5 ridiculously small, concrete, unambiguous physical micro-actions. " +
            "Start each action with an unambiguous physical verb (e.g., 'Pick up', 'Move', 'Open', 'Wipe'). " +
            "Avoid high cognitive load, planning, or vague instructions. " +
            "Respond ONLY with a valid JSON array of strings, e.g. [\"Step 1\", \"Step 2\", \"Step 3\"]."

        fun computeHash(text: String): String {
            val normalized = text.trim().lowercase()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(normalized.toByteArray(StandardCharsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun stepsToJson(steps: List<String>): String {
            return "[" + steps.joinToString(",") { step ->
                "\"" + step.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            } + "]"
        }


        fun parseJsonSteps(jsonStr: String): List<String> {
            val clean = jsonStr.trim()
            if (!clean.startsWith("[") || !clean.endsWith("]")) return emptyList()
            val inner = clean.substring(1, clean.length - 1).trim()
            if (inner.isEmpty()) return emptyList()

            val result = mutableListOf<String>()
            var inQuotes = false
            var escape = false
            val current = StringBuilder()

            for (char in inner) {
                if (escape) {
                    current.append(char)
                    escape = false
                    continue
                }
                if (char == '\\') {
                    escape = true
                    continue
                }
                if (char == '"') {
                    inQuotes = !inQuotes
                    continue
                }
                if (!inQuotes && char.isWhitespace()) {
                    continue
                }
                if (char == ',' && !inQuotes) {
                    val s = current.toString().trim()
                    if (s.isNotEmpty()) result.add(s)
                    current.clear()
                    continue
                }
                current.append(char)
            }
            val finalStr = current.toString().trim()
            if (finalStr.isNotEmpty()) result.add(finalStr)

            return result
        }
    }

    fun parseJsonSteps(jsonStr: String): List<String> = Companion.parseJsonSteps(jsonStr)

    suspend fun decompose(taskTitle: String): List<String> = withContext(Dispatchers.IO) {

        val cleanTitle = taskTitle.trim()
        if (cleanTitle.isBlank()) return@withContext emptyList()

        val hash = computeHash(cleanTitle)

        // Tier 1: Cache verification
        taskDao?.findCachedDecomposition(hash)?.let { cached ->
            val parsed = parseJsonSteps(cached.subStepsJson)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        // Tier 2: BYOK AI Inference
        val provider = keystoreManager?.getSelectedProvider() ?: AIProvider.GROQ
        val apiKey = keystoreManager?.getApiKey(provider)

        if (!apiKey.isNullOrBlank()) {
            try {
                val aiSteps = callAiProvider(provider, apiKey, cleanTitle)
                if (aiSteps.isNotEmpty()) {
                    return@withContext aiSteps
                }
            } catch (_: Exception) {
                // Fail gracefully to Tier 3 on any network/parsing failure
            }
        }

        // Tier 3: Offline Heuristic Engine
        generateOfflineMicroSteps(cleanTitle)
    }


    private fun callAiProvider(provider: AIProvider, apiKey: String, title: String): List<String> {
        val (endpoint, requestBody) = when (provider) {
            AIProvider.GROQ -> {
                val url = "https://api.groq.com/openai/v1/chat/completions"
                val body = JSONObject().apply {
                    put("model", provider.defaultModel)
                    put("temperature", 0.3)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Task: $title")
                        })
                    })
                }.toString()
                Pair(url, body)
            }
            AIProvider.OPENROUTER -> {
                val url = "https://openrouter.ai/api/v1/chat/completions"
                val body = JSONObject().apply {
                    put("model", provider.defaultModel)
                    put("temperature", 0.3)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", SYSTEM_PROMPT)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Task: $title")
                        })
                    })
                }.toString()
                Pair(url, body)
            }
            AIProvider.GEMINI -> {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val body = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "$SYSTEM_PROMPT\n\nTask: $title")
                                })
                            })
                        })
                    })
                }.toString()
                Pair(url, body)
            }
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            if (provider != AIProvider.GEMINI) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(requestBody) }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            return emptyList()
        }

        val responseText = BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)).use {
            it.readText()
        }

        return extractStepsFromResponse(provider, responseText)
    }

    private fun extractStepsFromResponse(provider: AIProvider, jsonResponse: String): List<String> {
        val root = JSONObject(jsonResponse)
        val contentText = when (provider) {
            AIProvider.GROQ, AIProvider.OPENROUTER -> {
                val choices = root.getJSONArray("choices")
                if (choices.length() == 0) return emptyList()
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
            AIProvider.GEMINI -> {
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() == 0) return emptyList()
                candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        }

        // Clean any markdown formatting like ```json ... ```
        val sanitized = contentText.trim()
            .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^```\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        return parseJsonSteps(sanitized)
    }

    /**
     * Resilient offline heuristic decomposition engine when network or BYOK keys are unavailable.
     */
    internal fun generateOfflineMicroSteps(title: String): List<String> {
        val lower = title.lowercase()
        return when {
            lower.contains("clean") || lower.contains("room") || lower.contains("house") || lower.contains("declutter") -> listOf(
                "Pick up 3 pieces of trash and put them directly in the bin",
                "Bring all dirty dishes or cups to the sink area (don't wash yet)",
                "Collect all loose clothes and drop them into a hamper or single pile",
                "Wipe down one flat surface with a damp cloth or wet wipe",
                "Stop here and evaluate your spoon / energy level"
            )
            lower.contains("dish") || lower.contains("kitchen") -> listOf(
                "Put 5 utensils into the sink or dishwasher rack",
                "Rinse 2 plates or cups with warm water",
                "Wipe down the counter space around the sink",
                "Drink a glass of water and decide if you want to continue"
            )
            lower.contains("laundry") || lower.contains("wash") || lower.contains("clothes") -> listOf(
                "Put all clothes from the floor into the laundry basket",
                "Walk the basket to the washing machine",
                "Put in one detergent pod and press start",
                "Set a timer on your phone for 45 minutes"
            )
            lower.contains("email") || lower.contains("inbox") || lower.contains("message") -> listOf(
                "Open email app and archive 3 newsletters without reading them",
                "Find the single most urgent message and read only the first sentence",
                "Draft a 1-sentence reply: 'Thanks for reaching out, looking into this now.'",
                "Close the app and take 3 deep breaths"
            )
            lower.contains("write") || lower.contains("study") || lower.contains("paper") || lower.contains("report") -> listOf(
                "Open the blank document or book to the required page",
                "Write just 1 sentence or read just 1 paragraph badly (no editing)",
                "Jot down 3 bullet points of rough thoughts",
                "Step away for 60 seconds before deciding to proceed"
            )
            lower.contains("tax") || lower.contains("bill") || lower.contains("paperwork") -> listOf(
                "Find the envelope or login link and place it on your desk",
                "Log in or open the document to verify the due date",
                "Fill out only the first field or locate your ID number",
                "Save draft and take a brief physical stretch"
            )
            lower.contains("cook") || lower.contains("eat") || lower.contains("meal") -> listOf(
                "Walk to the kitchen and drink one glass of water",
                "Take out 2 ingredients or one simple microwave option",
                "Place on plate or pan and apply heat",
                "Sit down in a comfortable chair to eat"
            )
            else -> listOf(
                "Gather whatever physical items you need for '$title' into one spot",
                "Set a 3-minute timer and do the easiest first part badly",
                "Put away 2 items or complete 1 small sub-action",
                "Stop, breathe, and evaluate your current energy spoons"
            )
        }
    }
}
