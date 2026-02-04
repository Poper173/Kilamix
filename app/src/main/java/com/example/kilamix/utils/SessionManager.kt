package com.itech.kilamix.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveAuth(token: String, role: String) {
        prefs.edit()
            .putString("token", token)
            .putString("role", role)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun getRole(): String? = prefs.getString("role", null)

    fun logout() {
        prefs.edit().clear().apply()
    }
}
