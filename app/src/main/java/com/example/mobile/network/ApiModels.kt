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