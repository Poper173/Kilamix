


import com.itech.kilamix.model.LikeResponse

import com.itech.kilamix.model.VideoResponse

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("videos")
    fun getVideos(): Call<VideoResponse>

    @POST("videos/{id}/like")
    fun likeVideo(
        @Path("id") id: Int,
        @Header("Authorization") token: String
    ): Call<LikeResponse>

}