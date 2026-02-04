package com.itech.kilamix.model

import com.google.gson.annotations.SerializedName

data class ChannelResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("channel_description") val description: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("channel_banner") val banner: String?,
    @SerializedName("total_views") val totalViews: Int,
    @SerializedName("total_subscribers") val totalSubscribers: Int,
    @SerializedName("videos_count") val videosCount: Int
)