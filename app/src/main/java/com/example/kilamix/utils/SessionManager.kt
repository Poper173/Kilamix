package com.itech.kilamix.utils


import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("itech_session", Context.MODE_PRIVATE)

    fun saveAuth(token: String, role: String) {
        prefs.edit()
            .putString("TOKEN", token)
            .putString("ROLE", role)
            .apply()
    }

    fun getToken(): String? = prefs.getString("TOKEN", null)
    fun getRole(): String? = prefs.getString("ROLE", null)

    fun logout() {
        prefs.edit().clear().apply()
    }
}
