package com.itech.kilamix.api

import com.itech.kilamix.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST




interface ApiService {

    @POST("login")
    fun login(@Body request:LoginRequest): Call<AuthResponse>

    @POST("register")
    fun register(@Body request:RegisterRequest): Call<AuthResponse>
}
