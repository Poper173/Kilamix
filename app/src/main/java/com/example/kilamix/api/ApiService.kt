package com.itech.kilamix.api

import com.itech.kilamix.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    fun getVideos(): Call<ApiResponse<List<Video>>>

    @POST("videos/{id}/like")
    fun likeVideo(
        @Path("id") videoId: Int,
        @Header("Authorization") token: String
    ): Call<ApiResponse<LikeResponse>>

    @GET("videos/{id}")
    fun getVideo(
        @Path("id") videoId: Int
    ): Call<ApiResponse<Video>>

    // Get creator channel
    @GET("creator/channel")
    fun getCreatorChannel(
        @Header("Authorization") token: String
    ): Call<ApiResponse<ChannelResponse>>

    // Get my uploaded videos
    @GET("my-videos")
    fun getMyVideos(
        @Header("Authorization") token: String
    ): Call<ApiResponse<List<Video>>>

    // Upload video (multipart form)
    @Multipart
    @POST("videos")
    fun uploadVideo(
        @Header("Authorization") token: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category_id") categoryId: RequestBody,
        @Part video: MultipartBody.Part,
        @Part thumbnail: MultipartBody.Part? = null
    ): Call<ApiResponse<Video>>

    // Delete video
    @DELETE("videos/{id}")
    fun deleteVideo(
        @Header("Authorization") token: String,
        @Path("id") videoId: Int
    ): Call<ApiResponse<Void>>

    // Update video
    @PUT("videos/{id}")
    fun updateVideo(
        @Header("Authorization") token: String,
        @Path("id") videoId: Int,
        @Body videoUpdate: VideoUpdateRequest
    ): Call<ApiResponse<Video>>

    // Update channel profile
    @Multipart
    @PUT("creator/channel")
    fun updateChannelProfile(
        @Header("Authorization") token: String,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Call<ApiResponse<ChannelResponse>>

}
