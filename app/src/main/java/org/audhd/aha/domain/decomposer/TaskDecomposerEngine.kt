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

private object SafeLog {
    fun i(tag: String, msg: String) {
        try { android.util.Log.i(tag, msg) } catch (_: Throwable) {}
    }
    fun w(tag: String, msg: String) {
        try { android.util.Log.w(tag, msg) } catch (_: Throwable) {}
    }
    fun e(tag: String, msg: String) {
        try { android.util.Log.e(tag, msg) } catch (_: Throwable) {}
    }
}

/**
 * High-leverage task decomposer ("Goblin Mode") designed specifically for AuDHD executive dysfunction.
 * Translates ambiguous, overwhelming demands into 3-5 concrete, low-friction physical micro-actions.
 *
 * Resilience Architecture:
 * 1. Content-hash SQLite cache hit (instant 0ms)
 * 2. Multi-provider BYOK AI Inference with resilient multi-format response extraction
 * 3. Domain-specific offline heuristic breakdown engine (0 network dependency, 100% reliable)
 */
class TaskDecomposerEngine(
    private val taskDao: TaskDao? = null,
    private val keystoreManager: KeystoreManager? = null
) {

    companion object {
        private const val TAG = "TaskDecomposer"

        const val SYSTEM_PROMPT =
            "You are an AuDHD executive functioning assistant. Your job is to defeat task paralysis. " +
            "Decompose the user's task into 3 to 5 ridiculously small, concrete, low-friction micro-actions. " +
            "Start each action with an unambiguous physical verb (e.g., 'Open', 'Find', 'Write', 'Put', 'Close'). " +
            "Avoid high cognitive load, vague advice, or planning. " +
            "Respond ONLY with a JSON array of strings, for example: [\"Step 1\", \"Step 2\", \"Step 3\"]."

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

        /**
         * Robust, multi-strategy step parser.
         * Handles:
         * 1. Direct JSON array: ["Step 1", "Step 2"]
         * 2. Markdown fenced JSON: ```json [...] ```
         * 3. Embedded JSON array in conversational text: "Here is your plan: [...]"
         * 4. JSON objects with a steps list: {"steps": ["..."]}
         * 5. Fallback line-by-line list parser: "1. Step 1\n2. Step 2" or "- Step 1\n- Step 2"
         */
        fun parseJsonSteps(rawText: String): List<String> {
            val text = rawText.trim()
            if (text.isBlank()) return emptyList()

            // Strategy 1: Find JSON array bounds anywhere in the text
            val firstBracket = text.indexOf('[')
            val lastBracket = text.lastIndexOf(']')

            if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                val arraySubstring = text.substring(firstBracket, lastBracket + 1)
                try {
                    val jsonArray = JSONArray(arraySubstring)
                    val result = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.optString(i, "").trim()
                        if (item.isNotEmpty()) {
                            result.add(item)
                        }
                    }
                    if (result.isNotEmpty()) return result
                } catch (_: Exception) {
                    // Fallthrough to custom scanner
                }

                // Custom manual scanner for resilient string extraction inside brackets
                val inner = arraySubstring.substring(1, arraySubstring.length - 1).trim()
                if (inner.isNotEmpty()) {
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

                    if (result.isNotEmpty()) return result
                }
            }

            // Strategy 2: Check for JSON object with "steps" key
            val firstBrace = text.indexOf('{')
            val lastBrace = text.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                try {
                    val obj = JSONObject(text.substring(firstBrace, lastBrace + 1))
                    val stepsArray = obj.optJSONArray("steps")
                        ?: obj.optJSONArray("actions")
                        ?: obj.optJSONArray("micro_steps")
                    if (stepsArray != null) {
                        val result = mutableListOf<String>()
                        for (i in 0 until stepsArray.length()) {
                            val item = stepsArray.optString(i, "").trim()
                            if (item.isNotEmpty()) result.add(item)
                        }
                        if (result.isNotEmpty()) return result
                    }
                } catch (_: Exception) {
                    // Fallthrough
                }
            }

            // Strategy 3: Parse numbered or bulleted markdown lines (e.g., "1. Action" or "- Action")
            val lines = text.lines()
            val listItems = mutableListOf<String>()
            val itemRegex = Regex("""^\s*(?:\d+[\.\)]|[-*•])\s*(.+)""")

            for (line in lines) {
                val match = itemRegex.find(line)
                if (match != null) {
                    val content = match.groupValues[1]
                        .replace(Regex("""^["'`]|["'`]$"""), "")
                        .trim()
                    if (content.isNotEmpty()) {
                        listItems.add(content)
                    }
                }
            }

            if (listItems.size >= 2) {
                return listItems.take(5)
            }

            return emptyList()
        }
    }

    fun parseJsonSteps(jsonStr: String): List<String> = Companion.parseJsonSteps(jsonStr)

    /**
     * Decomposes a task title into micro-actions with multi-tier fallback.
     * @param bypassCache Set to true when regenerating task steps explicitly.
     */
    suspend fun decompose(taskTitle: String, bypassCache: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val cleanTitle = taskTitle.trim()
        if (cleanTitle.isBlank()) return@withContext emptyList()

        val hash = computeHash(cleanTitle)

        // Tier 1: Cache verification (unless regenerating)
        if (!bypassCache) {
            taskDao?.findCachedDecomposition(hash)?.let { cached ->
                val parsed = parseJsonSteps(cached.subStepsJson)
                if (parsed.isNotEmpty()) return@withContext parsed
            }
        }

        // Tier 2: BYOK AI Inference with Multi-Provider Fallback
        val activeProvider = keystoreManager?.getSelectedProvider() ?: AIProvider.GROQ
        val candidateProviders = mutableListOf(activeProvider)
        AIProvider.values().forEach { if (it != activeProvider) candidateProviders.add(it) }

        for (provider in candidateProviders) {
            val apiKey = keystoreManager?.getApiKey(provider)
            if (!apiKey.isNullOrBlank()) {
                try {
                    val aiSteps = callAiProvider(provider, apiKey, cleanTitle)
                    if (aiSteps.isNotEmpty()) {
                        SafeLog.i(TAG, "Successfully decomposed '$cleanTitle' using ${provider.name}")
                        return@withContext aiSteps
                    }
                } catch (e: Exception) {
                    SafeLog.w(TAG, "AI decomposition failed for provider ${provider.name}: ${e.message}")
                }
            }
        }

        // Tier 3: Domain-Aware Offline Heuristic Engine
        SafeLog.i(TAG, "Using offline domain heuristics for '$cleanTitle'")
        generateOfflineMicroSteps(cleanTitle)
    }

    /**
     * Verifies connectivity and key validity for a given provider.
     */
    suspend fun testConnection(provider: AIProvider, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty"))
        }

        try {
            val steps = callAiProvider(provider, apiKey.trim(), "Drink a glass of water")
            if (steps.isNotEmpty()) {
                Result.success("Connected successfully to ${provider.displayName}")
            } else {
                Result.failure(Exception("Provider returned empty response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callAiProvider(provider: AIProvider, apiKey: String, title: String): List<String> {
        val (endpoint, requestBody) = when (provider) {
            AIProvider.GROQ -> {
                val url = "https://api.groq.com/openai/v1/chat/completions"
                val body = JSONObject().apply {
                    put("model", provider.defaultModel)
                    put("temperature", 0.2)
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
                    put("temperature", 0.2)
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
            connectTimeout = 12000
            readTimeout = 12000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "a-Ha-Launcher/1.0 (Android; AuDHD)")

            when (provider) {
                AIProvider.GROQ -> {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                AIProvider.OPENROUTER -> {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("HTTP-Referer", "https://github.com/audhd/aha")
                    setRequestProperty("X-Title", "a-Ha AuDHD Launcher")
                }
                AIProvider.GEMINI -> {
                    setRequestProperty("x-goog-api-key", apiKey)
                }
            }
        }

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(requestBody) }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = try {
                BufferedReader(InputStreamReader(conn.errorStream, StandardCharsets.UTF_8)).use { it.readText() }
            } catch (_: Exception) {
                "Unable to read error stream"
            }
            SafeLog.e(TAG, "API HTTP error $responseCode from ${provider.name}: $errorBody")
            throw Exception("HTTP $responseCode from ${provider.displayName}: $errorBody")
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
                val choices = root.optJSONArray("choices") ?: return emptyList()
                if (choices.length() == 0) return emptyList()
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
            AIProvider.GEMINI -> {
                val candidates = root.optJSONArray("candidates") ?: return emptyList()
                if (candidates.length() == 0) return emptyList()
                candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        }

        return parseJsonSteps(contentText)
    }

    /**
     * Resilient domain-aware offline heuristic decomposition engine.
     * Categorizes tasks into practical real-world domains rather than generic placeholders.
     */
    internal fun generateOfflineMicroSteps(title: String): List<String> {
        val lower = title.lowercase()
        return when {
            // Career / Job Search / Resumes
            lower.contains("job") || lower.contains("resume") || lower.contains("cv") ||
            lower.contains("apply") || lower.contains("interview") || lower.contains("career") ||
            lower.contains("hiring") || lower.contains("linkedin") -> listOf(
                "Open your computer or phone and open your primary job folder",
                "Find your most recent resume file and open it to review",
                "Open exactly 1 job board tab (e.g. LinkedIn or Indeed)",
                "Bookmark 1 promising listing without reading requirements yet",
                "Step away for 2 minutes to evaluate your momentum"
            )

            // Cleaning / Organizing / Physical Space
            lower.contains("clean") || lower.contains("room") || lower.contains("house") || lower.contains("declutter") -> listOf(
                "Pick up 3 pieces of trash and put them directly in the bin",
                "Bring all dirty dishes or cups to the sink area (don't wash yet)",
                "Collect all loose clothes and drop them into a hamper or single pile",
                "Wipe down one flat surface with a damp cloth or wet wipe",
                "Stop here and evaluate your spoon / energy level"
            )

            // Kitchen / Dishes
            lower.contains("dish") || lower.contains("kitchen") || lower.contains("sink") -> listOf(
                "Put 5 utensils into the sink or dishwasher rack",
                "Rinse 2 plates or cups with warm water",
                "Wipe down the counter space directly around the sink",
                "Drink a glass of water and decide if you want to continue"
            )

            // Laundry / Clothing
            lower.contains("laundry") || lower.contains("wash") || lower.contains("clothes") -> listOf(
                "Put all clothes from the floor into the laundry basket",
                "Walk the basket to the washing machine",
                "Put in one detergent pod and press start",
                "Set a timer on your phone for 45 minutes"
            )

            // Email / Communication / Messages
            lower.contains("email") || lower.contains("inbox") || lower.contains("message") || lower.contains("reply") -> listOf(
                "Open email app and archive 3 newsletters without reading them",
                "Find the single most urgent message and read only the first sentence",
                "Draft a 1-sentence reply: 'Thanks for reaching out, looking into this now.'",
                "Close the app and take 3 deep breaths"
            )

            // Writing / Study / Reading / Academic
            lower.contains("write") || lower.contains("study") || lower.contains("paper") ||
            lower.contains("report") || lower.contains("homework") || lower.contains("essay") -> listOf(
                "Open the blank document or book to the required page",
                "Write just 1 sentence or read just 1 paragraph imperfectly",
                "Jot down 3 rough bullet points without self-editing",
                "Step away for 60 seconds before deciding to proceed"
            )

            // Finance / Paperwork / Taxes / Bureaucracy
            lower.contains("tax") || lower.contains("bill") || lower.contains("paperwork") ||
            lower.contains("bank") || lower.contains("money") || lower.contains("insurance") -> listOf(
                "Find the envelope, bill, or login link and place it on your desk",
                "Log in or open the document to verify only the due date or balance",
                "Fill out only the first field or locate your account/ID number",
                "Save draft and take a brief physical stretch"
            )

            // Cooking / Nourishment
            lower.contains("cook") || lower.contains("eat") || lower.contains("meal") ||
            lower.contains("food") || lower.contains("dinner") || lower.contains("lunch") -> listOf(
                "Walk to the kitchen and drink one glass of water",
                "Take out 2 ingredients or one simple meal option",
                "Place food on plate or pan and apply heat",
                "Sit down in a comfortable chair to eat"
            )

            // Medical / Health / Appointments
            lower.contains("doctor") || lower.contains("dentist") || lower.contains("appointment") ||
            lower.contains("med") || lower.contains("pill") || lower.contains("health") -> listOf(
                "Find the clinic or doctor's phone number or portal URL",
                "Write down the single main question or symptom on paper",
                "Dial the number or click the booking link",
                "Record the date and time immediately on your calendar"
            )

            // General Purpose AuDHD Momentum Starter
            else -> listOf(
                "Gather whatever physical items or tools are needed for '$title'",
                "Set a 3-minute timer and do the easiest initial step imperfectly",
                "Write down the next single action on a scratchpad",
                "Pause, breathe, and evaluate if you have momentum to continue"
            )
        }
    }
}
