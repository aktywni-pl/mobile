package com.example.mobile.network


data class LoginRequest(
    val email: String,
    val password: String
)


data class LoginResponse(
    val id: Int,
    val email: String,
    val role: String,
    val token: String
)


data class ErrorResponse(
    val message: String?
)

data class Activity(
    val id: Int,
    val user_id: Int,
    val name: String,
    val type: String,
    val distance_km: Double,
    val duration_min: Int,
    val started_at: String
)