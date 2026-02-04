package com.itech.kilamix.model

data class VideoUpdateRequest(
    val title: String,
    val description: String,
    val category_id: Int
)