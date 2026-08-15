package com.voro.houseassessment.data

import android.content.Context

class BaiduMapConfigRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getApiKey(): String = preferences.getString(KEY_API_KEY, "").orEmpty().trim()

    fun isPrivacyAccepted(): Boolean = preferences.getBoolean(KEY_PRIVACY_ACCEPTED, false)

    fun save(apiKey: String, privacyAccepted: Boolean) {
        preferences.edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .putBoolean(KEY_PRIVACY_ACCEPTED, privacyAccepted)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "baidu_map_config"
        const val KEY_API_KEY = "api_key"
        const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"
    }
}
