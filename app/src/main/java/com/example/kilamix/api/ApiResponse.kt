package com.itech.kilamix.api

/**
 * Generic API response wrapper class for handling API responses.
 * This follows the same pattern as AuthResponse in the project.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)

