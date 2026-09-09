package org.audhd.aha.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class AIProvider(val displayName: String, val defaultModel: String) {
    GROQ("Groq Cloud", "llama-3.1-8b-instant"),
    OPENROUTER("OpenRouter", "meta-llama/llama-3.1-8b-instruct:free"),
    GEMINI("Google Gemini", "gemini-1.5-flash")
}

/**
 * Hardware-backed encrypted credential storage using Android KeyStore and EncryptedSharedPreferences.
 * Eliminates plain-text secret storage with zero external network telemetry.
 */
class KeystoreManager(context: Context) {

    companion object {
        private const val PREFS_FILE = "aha_secure_vault"
        private const val KEY_PREFIX_API_KEY = "api_key_"
        private const val KEY_SELECTED_PROVIDER = "selected_ai_provider"

        const val BILLING_SAFETY_WARNING =
            "CRITICAL BILLING SAFETY: a-Ha runs directly on your device with zero proxy, zero backend telemetry, " +
            "and zero tracking. When using cloud API keys (especially Google Gemini or OpenRouter), " +
            "ensure you configure strict \$0 spend caps or budget alerts in your provider console to eliminate financial risk."
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setApiKey(provider: AIProvider, key: String) {
        securePrefs.edit().putString(KEY_PREFIX_API_KEY + provider.name, key.trim()).apply()
    }

    fun getApiKey(provider: AIProvider): String? {
        val key = securePrefs.getString(KEY_PREFIX_API_KEY + provider.name, null)
        return if (key.isNullOrBlank()) null else key
    }

    fun clearApiKey(provider: AIProvider) {
        securePrefs.edit().remove(KEY_PREFIX_API_KEY + provider.name).apply()
    }

    fun getSelectedProvider(): AIProvider {
        val name = securePrefs.getString(KEY_SELECTED_PROVIDER, AIProvider.GROQ.name)
        return try {
            AIProvider.valueOf(name ?: AIProvider.GROQ.name)
        } catch (_: Exception) {
            AIProvider.GROQ
        }
    }

    fun setSelectedProvider(provider: AIProvider) {
        securePrefs.edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
    }
}
