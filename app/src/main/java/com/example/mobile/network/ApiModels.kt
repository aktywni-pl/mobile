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
    val duration_min: Double,
    val started_at: String
)

data class ActivityResponse(
    val id: Int
)

data class CreateActivityRequest(
    val user_id: Int,
    val name: String,
    val type: String,
    val started_at: String,
    val distance_km: Double = 0.0,
    val duration_min: Double = 0.0
)

data class TrackRequest(
    val points: List<TrackPoint>
)

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val timestamp: String
)

data class TrackResponse(
    val activity_id: Int,
    val points: List<TrackPoint>
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val confirmPassword: String
)

data class RegisterResponse(
    val id: Int,
    val username: String,
    val email: String,
    val token: String?
)

data class ProfileResponse(
    val user_id: Int,
    val exists: Boolean,
    val profile: UserProfile?
)

data class UserProfile(
    val first_name: String,
    val last_name: String,
    val birth_date: String?,
    val gender: String?,
    val height_cm: Int?,
    val weight_kg: Double?,
    val avatar: String? = null
)

data class UpdateProfileRequest(
    val first_name: String,
    val last_name: String,
    val city: String = "",
    val bio: String = ""
)
