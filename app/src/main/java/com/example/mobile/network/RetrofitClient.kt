package com.example.mobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

// Singleton
object RetrofitInstance {
    private const val BASE_URL = "https://fattish-gilda-metrical.ngrok-free.dev/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

object UserSession {
    var token: String? = null
    var userId: Int? = null
}