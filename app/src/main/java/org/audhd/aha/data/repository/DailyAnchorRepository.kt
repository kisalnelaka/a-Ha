package org.audhd.aha.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's single daily focus anchor ("one thing today") to SharedPreferences.
 *
 * Intentionally single-field: cognitive overhead is minimized by preventing the user from
 * writing a second goal until the first is explicitly cleared.
 */
class DailyAnchorRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("aha_daily_anchor", Context.MODE_PRIVATE)

    private val _anchor = MutableStateFlow(prefs.getString(KEY_ANCHOR, "") ?: "")
    val anchor: StateFlow<String> = _anchor.asStateFlow()

    fun saveAnchor(text: String) {
        _anchor.value = text
        prefs.edit().putString(KEY_ANCHOR, text).apply()
    }

    fun clearAnchor() {
        _anchor.value = ""
        prefs.edit().remove(KEY_ANCHOR).apply()
    }

    companion object {
        private const val KEY_ANCHOR = "daily_anchor_text"
    }
}
