package com.itech.kilamix.model

// Wrapper for paginated video list response (Laravel format: message + data)
data class VideoListResponse(
    val message: String?,
    val data: List<Video>?,
    val links: Links?,
    val meta: Meta?
)

// Response wrapper for single video
data class VideoResponse(
    val message: String?,
    val data: Video?
)

data class Links(
    val first: String?,
    val last: String?,
    val prev: String?,
    val next: String?
)

data class Meta(
    val current_page: Int,
    val last_page: Int,
    val total: Int,
    val path: String?
)

data class Video(
    val id: Int,
    val title: String,
    val description: String?,
    val thumbnail_url: String?,
    val video_file_url: String?,
    val video_url: String?,
    val likes_count: Int,
    val views_count: Int,
    val duration: Long = 0,
    val is_liked: Boolean = false,
    val is_authenticated: Boolean = false,
    val user: Creator?,
    val category: Category?,
    val created_at: String?
)

data class Creator(
    val id: Int,
    val name: String,
    val avatar: String?
)

data class Category(
    val id: Int,
    val name: String
)
