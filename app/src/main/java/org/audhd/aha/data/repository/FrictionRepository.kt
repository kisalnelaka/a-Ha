package org.audhd.aha.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists per-app friction delay overrides.
 *
 * Friction levels:
 *  0 = default (12s system default in MindfulDelayActivity)
 *  5, 15, 30, 60 = explicit seconds override
 *
 * Stored as "pkg=seconds,pkg=seconds" in SharedPreferences.
 */
class FrictionRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("aha_friction", Context.MODE_PRIVATE)

    /** Returns the delay seconds for the given package, or null to use the system default. */
    fun getDelay(packageName: String): Int? {
        val raw = prefs.getString(KEY_OVERRIDES, "") ?: ""
        if (raw.isBlank()) return null
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2 && parts[0] == packageName) parts[1].toIntOrNull() else null
        }.firstOrNull()
    }

    fun setDelay(packageName: String, seconds: Int) {
        val current = loadMap().toMutableMap()
        current[packageName] = seconds
        persist(current)
        _overrides.value = current
    }

    fun clearDelay(packageName: String) {
        val current = loadMap().toMutableMap()
        current.remove(packageName)
        persist(current)
        _overrides.value = current
    }

    private val _overrides = MutableStateFlow(loadMap())
    val overrides: StateFlow<Map<String, Int>> = _overrides.asStateFlow()

    private fun loadMap(): Map<String, Int> {
        val raw = prefs.getString(KEY_OVERRIDES, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val sec = parts[1].toIntOrNull() ?: return@mapNotNull null
                parts[0] to sec
            } else null
        }.toMap()
    }

    private fun persist(map: Map<String, Int>) {
        val raw = map.entries.joinToString(",") { "${it.key}=${it.value}" }
        prefs.edit().putString(KEY_OVERRIDES, raw).apply()
    }

    companion object {
        private const val KEY_OVERRIDES = "friction_overrides"
        val FRICTION_LEVELS = listOf(5, 15, 30, 60)
    }
}
