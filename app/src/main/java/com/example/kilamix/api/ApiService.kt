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
        @Part("visibility") visibility: RequestBody,
        @Part video: MultipartBody.Part,
        @Part thumbnail: MultipartBody.Part? = null
    ): Call<ApiResponse<Video>>

    // Logout user
    @POST("logout")
    fun logout(
        @Header("Authorization") token: String
    ): Call<ApiResponse<Void>>

    // Get paginated my videos
    @GET("my-videos")
    fun getMyVideosPaginated(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Call<ApiResponse<List<Video>>>

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

    // =====================
    // ADMIN ENDPOINTS
    // =====================

    // Get all users (admin)
    @GET("admin/users")
    fun getAdminUsers(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Call<ApiResponse<List<AdminUser>>>

    // Toggle user status (activate/deactivate)
    @POST("admin/users/{id}/toggle-status")
    fun toggleUserStatus(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Call<ApiResponse<AdminUser>>

    // Update user role
    @POST("admin/users/{id}/role")
    fun updateUserRole(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body roleUpdate: RoleUpdateRequest
    ): Call<ApiResponse<AdminUser>>

    // Delete user
    @DELETE("admin/users/{id}")
    fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Call<ApiResponse<Void>>

    // Get dashboard stats
    @GET("admin/stats")
    fun getAdminStats(
        @Header("Authorization") token: String
    ): Call<ApiResponse<AdminStats>>

}
