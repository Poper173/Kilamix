
package com.itech.kilamix.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8000/api/"
    
    // Timeout durations for large file uploads
    private const val CONNECT_TIMEOUT = 60L // seconds
    private const val READ_TIMEOUT = 300L // 5 minutes for large file uploads
    private const val WRITE_TIMEOUT = 300L // 5 minutes for large file uploads

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Full body logging for debugging
    }

    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // For multipart requests (uploads), don't override Content-Type
        // Let Retrofit set it automatically with proper boundary
        val isMultipart = originalRequest.body?.contentType()?.type == "multipart"
        
        val requestWithHeaders = originalRequest.newBuilder()
            .header("Accept", "application/json")
            // Only set Content-Type for non-multipart requests
            .apply {
                if (!isMultipart) {
                    header("Content-Type", "application/json")
                }
            }
            .build()
        chain.proceed(requestWithHeaders)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(logging)
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
