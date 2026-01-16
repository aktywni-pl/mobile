package com.example.mobile.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "user_email"
    }

    fun saveAuthToken(token: String?) {
        if (token != null) {
            val editor = prefs.edit()
            editor.putString(KEY_TOKEN, token)
            editor.apply()
        }
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserDetails(id: Int, email: String) {
        val editor = prefs.edit()
        editor.putInt(KEY_USER_ID, id)
        editor.putString(KEY_EMAIL, email)
        editor.apply()
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}