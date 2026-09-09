package org.audhd.aha.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the set of hidden app packages.
 *
 * Hidden apps are excluded from the app drawer entirely without uninstallation.
 * The set is persisted as a comma-separated string in SharedPreferences.
 */
class HiddenAppsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("aha_hidden_apps", Context.MODE_PRIVATE)

    private fun loadHidden(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    private val _hiddenPackages = MutableStateFlow(loadHidden())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    fun hideApp(packageName: String) {
        val updated = _hiddenPackages.value + packageName
        _hiddenPackages.value = updated
        persist(updated)
    }

    fun unhideApp(packageName: String) {
        val updated = _hiddenPackages.value - packageName
        _hiddenPackages.value = updated
        persist(updated)
    }

    fun isHidden(packageName: String): Boolean = packageName in _hiddenPackages.value

    private fun persist(set: Set<String>) {
        prefs.edit().putString(KEY_HIDDEN, set.joinToString(",")).apply()
    }

    companion object {
        private const val KEY_HIDDEN = "hidden_packages"
    }
}
