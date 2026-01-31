package com.itech.kilamix.api

import com.itech.kilamix.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import com.itech.kilamix.model.LikeResponse

import retrofit2.http.GET
import retrofit2.http.Header

import retrofit2.http.Path




interface ApiService {

    @POST("login")
    fun login(@Body request:LoginRequest): Call<AuthResponse>

    @POST("register")
    fun register(@Body request:RegisterRequest): Call<AuthResponse>
    @GET("videos") // Replace with your actual endpoint
    fun getVideos(): Call<VideoResponse>

    @POST("videos/{id}/like") // Replace with your actual endpoint
    fun likeVideo(
        @Path("id") videoId: Int,
        @Header("Authorization") token: String
    ): Call<LikeResponse>
    @GET("api/videos")
    fun getVideos(
        @Header("Authorization") token: String
    ): Call<VideoResponse>


}

