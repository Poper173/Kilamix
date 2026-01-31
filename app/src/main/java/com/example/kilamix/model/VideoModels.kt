package com.itech.kilamix.model

data class VideoResponse(
    val success: Boolean,
    val data: List<Video>
)

data class Video(
    val id: Int,
    val title: String,
    val description: String,
    val thumbnail_url: String?,
    val video_file_url: String,
    val likes_count: Int,
    val views_count: Int,
    val user: Creator
)

data class Creator(
    val id: Int,
    val name: String,
    val avatar: String?
)
