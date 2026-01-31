package com.itech.kilamix.api

import com.itech.kilamix.model.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ---------- AUTH ----------
    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    // ---------- VIDEOS ----------
    @GET("videos")
    fun getVideos(): Call<VideoResponse>

    @POST("videos/{id}/like")
    fun likeVideo(
        @Path("id") videoId: Int,
        @Header("Authorization") token: String
    ): Call<LikeResponse>
}

