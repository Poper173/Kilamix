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
                            onDownload = { downloadVideo(it) },
                            onPlay = { playVideo(it) } // ✅ FIXED
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
                    Toast.makeText(
                        this@HomeActivity,
                        t.message ?: "Network error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun likeVideo(video: Video) {
        val token = getSharedPreferences("auth", MODE_PRIVATE)
            .getString("token", null) ?: return

        RetrofitClient.api.likeVideo(video.id, "Bearer $token")
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                video.video_file_url.replace("localhost", "10.0.2.2")
            )
        }
        startActivity(Intent.createChooser(intent, "Share Video"))
    }

    private fun downloadVideo(video: Video) {
        val url = video.video_file_url.replace("localhost", "10.0.2.2")
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun playVideo(video: Video) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra(
            "video_url",
            video.video_file_url.replace("localhost", "10.0.2.2")
        )
        startActivity(intent)
    }
}
