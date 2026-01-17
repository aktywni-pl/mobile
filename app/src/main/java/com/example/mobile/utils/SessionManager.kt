package com.example.mobile.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences("AppSession", Context.MODE_PRIVATE)

    companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_LAST_NAME = "last_name"
        const val KEY_BIRTH_DATE = "birth_date"
        const val KEY_GENDER = "gender"
        const val KEY_HEIGHT = "height"
        const val KEY_WEIGHT = "weight"

        const val KEY_TOTAL_KM = "total_km"
        const val KEY_TOTAL_TIME = "total_time"
        const val KEY_ACTIVITIES_COUNT = "activities_count"
    }

    fun saveAuthToken(token: String) { prefs.edit().putString(KEY_TOKEN, token).apply() }
    fun fetchAuthToken(): String? { return prefs.getString(KEY_TOKEN, null) }

    fun saveUserId(id: Int) { prefs.edit().putInt(KEY_USER_ID, id).apply() }
    fun getUserId(): Int { return prefs.getInt(KEY_USER_ID, 0) }

    fun saveEmail(email: String) { prefs.edit().putString(KEY_EMAIL, email).apply() }
    fun getEmail(): String? { return prefs.getString(KEY_EMAIL, null) }

    fun saveProfileCache(
        firstName: String,
        lastName: String,
        birthDate: String,
        gender: String,
        height: String,
        weight: String
    ) {
        val editor = prefs.edit()
        editor.putString(KEY_FIRST_NAME, firstName)
        editor.putString(KEY_LAST_NAME, lastName)
        editor.putString(KEY_BIRTH_DATE, birthDate)
        editor.putString(KEY_GENDER, gender)
        editor.putString(KEY_HEIGHT, height)
        editor.putString(KEY_WEIGHT, weight)
        editor.apply()
    }

    fun getFirstName(): String? = prefs.getString(KEY_FIRST_NAME, "")
    fun getLastName(): String? = prefs.getString(KEY_LAST_NAME, "")
    fun getBirthDate(): String? = prefs.getString(KEY_BIRTH_DATE, "")
    fun getGender(): String? = prefs.getString(KEY_GENDER, "other")
    fun getHeight(): String? = prefs.getString(KEY_HEIGHT, "")
    fun getWeight(): String? = prefs.getString(KEY_WEIGHT, "")

    fun saveStatsCache(count: Int, km: Double, timeStr: String) {
        val editor = prefs.edit()
        editor.putInt(KEY_ACTIVITIES_COUNT, count)
        editor.putFloat(KEY_TOTAL_KM, km.toFloat())
        editor.putString(KEY_TOTAL_TIME, timeStr)
        editor.apply()
    }

    fun getActivitiesCount(): Int = prefs.getInt(KEY_ACTIVITIES_COUNT, 0)
    fun getTotalKm(): Double = prefs.getFloat(KEY_TOTAL_KM, 0.0f).toDouble()
    fun getTotalTime(): String = prefs.getString(KEY_TOTAL_TIME, "00:00") ?: "00:00"

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}