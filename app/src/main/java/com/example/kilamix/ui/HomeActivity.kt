package com.itech.kilamix.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itech.kilamix.R
import com.itech.kilamix.api.RetrofitClient
import com.itech.kilamix.model.Video
import com.itech.kilamix.model.VideoResponse
import com.itech.kilamix.model.LikeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recycler = findViewById(R.id.recyclerVideos)
        recycler.layoutManager = LinearLayoutManager(this)

        loadVideos()
    }

    private fun loadVideos() {
        RetrofitClient.api.getVideos()
            .enqueue(object : Callback<VideoResponse> {

                override fun onResponse(
                    call: Call<VideoResponse>,
                    response: Response<VideoResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val videos = response.body()!!.data

                        recycler.adapter = VideoAdapter(
                            videos = videos,
                            onLike = { likeVideo(it) },
                            onShare = { shareVideo(it) },
                            onDownload = { downloadVideo(it) }
                        )
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            "Failed to load videos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                    Toast.makeText(this@HomeActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun likeVideo(video: Video) {
        val token = "Bearer " + getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", "")

        RetrofitClient.api.likeVideo(video.id, token!!)
            .enqueue(object : Callback<LikeResponse> {

                override fun onResponse(
                    call: Call<LikeResponse>,
                    response: Response<LikeResponse>
                ) {
                    Toast.makeText(this@HomeActivity, "Liked!", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                    Toast.makeText(this@HomeActivity, "Like failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun shareVideo(video: Video) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, video.video_file_url)
        startActivity(Intent.createChooser(intent, "Share Video"))
    }

    private fun downloadVideo(video: Video) {
        val videoUrl = video.video_file_url

        if (videoUrl.isBlank()) {
            Toast.makeText(this, "Video URL missing", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open video", Toast.LENGTH_SHORT).show()
        }
    }
}
