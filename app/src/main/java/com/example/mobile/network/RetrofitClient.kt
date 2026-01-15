package com.example.mobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("api/activities")
    suspend fun getActivities(@Header("Authorization") token: String): List<Activity>

    @GET("api/activities/{id}")
    suspend fun getActivity(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Activity

    @GET("api/activities/{id}/track")
    suspend fun getActivityTrack(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): TrackResponse

    @POST("api/activities")
    suspend fun createActivity(
        @Header("Authorization") token: String,
        @Body request: CreateActivityRequest
    ): ActivityResponse

    @PUT("api/activities/{id}/track")
    suspend fun uploadTrack(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: TrackRequest
    ): retrofit2.Response<Unit>
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