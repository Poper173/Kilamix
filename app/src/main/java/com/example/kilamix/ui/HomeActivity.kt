package com.itech.kilamix.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itech.kilamix.R
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.api.ApiResponse
import com.itech.kilamix.model.LikeResponse
import com.itech.kilamix.model.Video
import com.itech.kilamix.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var sessionManager: SessionManager
    private var videoAdapter: VideoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // CONNECT TOOLBAR
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recycler = findViewById(R.id.recyclerVideos)
        recycler.layoutManager = LinearLayoutManager(this)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        loadVideos()
    }

    // =========================
    // LOAD VIDEOS
    // =========================
    private fun loadVideos() {
        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        // API returns: {"success":true, "data": [...], "message":"..."}
        apiService.getVideos().enqueue(object : Callback<ApiResponse<List<Video>>> {

            override fun onResponse(
                call: Call<ApiResponse<List<Video>>>,
                response: Response<ApiResponse<List<Video>>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val videos = apiResponse.data ?: emptyList()

                    if (videos.isNotEmpty()) {
                        videoAdapter = VideoAdapter(
                            videos = videos,
                            onLike = { likeVideo(it) },
                            onShare = { shareVideo(it) },
                            onDownload = { downloadVideo(it) },
                            onPlay = { playVideo(it) }
                        )
                        recycler.adapter = videoAdapter
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            apiResponse.message ?: "No videos available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load videos: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Video>>>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    // =========================
    // MENU (LOGOUT ICON)
    // =========================
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_logout -> {
                logout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // =========================
    // LOGOUT LOGIC
    // =========================
    private fun logout() {
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // =========================
    // ACTIONS
    // =========================

    /**
     * Like a video with real-time UI update
     */
    private fun likeVideo(video: Video) {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please login to like videos", Toast.LENGTH_SHORT).show()
            return
        }

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        // Optimistic UI update - toggle like state locally first
        val wasLiked = video.is_liked
        val currentLikesCount = video.likes_count
        val newLikesCount = if (wasLiked) currentLikesCount - 1 else currentLikesCount + 1
        
        // Update UI immediately for better UX
        videoAdapter?.updateVideoLike(video.id, !wasLiked, newLikesCount)

        apiService.likeVideo(video.id, "Bearer $token")
            .enqueue(object : Callback<ApiResponse<LikeResponse>> {

                override fun onResponse(
                    call: Call<ApiResponse<LikeResponse>>,
                    response: Response<ApiResponse<LikeResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            // API returned success, update with actual values from server
                            val actualLiked = apiResponse.data.liked
                            val actualLikesCount = apiResponse.data.likes_count
                            videoAdapter?.updateVideoLike(video.id, actualLiked, actualLikesCount)
                            
                            // Show feedback
                            if (actualLiked) {
                                Toast.makeText(this@HomeActivity, "Added to likes", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@HomeActivity, "Removed from likes", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Revert optimistic update on API error
                            videoAdapter?.updateVideoLike(video.id, wasLiked, currentLikesCount)
                            Toast.makeText(this@HomeActivity, apiResponse.message ?: "Like failed", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Revert optimistic update on HTTP error
                        videoAdapter?.updateVideoLike(video.id, wasLiked, currentLikesCount)
                        Toast.makeText(this@HomeActivity, "Like failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<LikeResponse>>, t: Throwable) {
                    // Revert optimistic update on network failure
                    videoAdapter?.updateVideoLike(video.id, wasLiked, currentLikesCount)
                    Toast.makeText(this@HomeActivity, "Like failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun shareVideo(video: Video) {
        val videoUrl = getVideoUrl(video)

        // Create share content with title and URL
        val shareText = buildString {
            append("Check out this video: ${video.title}\n\n")
            append("$videoUrl\n\n")
            video.user?.name?.let { append("Shared from iTechTube") }
        }

        val intent: Intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun downloadVideo(video: Video) {
        val url = getVideoUrl(video)
        if (url.isNotEmpty()) {
            try {
                Toast.makeText(this, "Opening video for download...", Toast.LENGTH_SHORT).show()
                // Open video URL in browser/download manager
                val downloadIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "video/*")
                }
                startActivity(downloadIntent)
            } catch (e: Exception) {
                // Fallback to just ACTION_VIEW if MIME type fails
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "Unable to download: No app available to handle video", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Video URL not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo(video: Video) {
        val url = getVideoUrl(video)
        if (url.isNotEmpty()) {
            val intent: Intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("video_url", url)
            intent.putExtra("video_title", video.title)
            intent.putExtra("video_id", video.id)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Video URL not available", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Converts relative or localhost video URLs to full URLs usable by ExoPlayer.
     * Handles cases like:
     * - "/api/videos/21/stream" -> "http://10.0.2.2:8000/api/videos/21/stream"
     * - "http://127.0.0.1:8000/api/videos/21/stream" -> "http://10.0.2.2:8000/api/videos/21/stream"
     * - "http://localhost:8000/api/videos/21/stream" -> "http://10.0.2.2:8000/api/videos/21/stream"
     */
    private fun getVideoUrl(video: Video): String {
        val baseUrl = "http://10.0.2.2:8000"
        val videoUrl = video.video_url?.takeIf { it.isNotEmpty() } 
            ?: video.video_file_url?.takeIf { it.isNotEmpty() } 
            ?: ""
        
        if (videoUrl.isEmpty()) return ""

        return when {
            // Relative path starting with "/"
            videoUrl.startsWith("/") -> "$baseUrl$videoUrl"
            // Path starting with "api/" (no leading slash)
            videoUrl.startsWith("api/") -> "$baseUrl/$videoUrl"
            // Already has full URL, replace localhost/127.0.0.1 with 10.0.2.2
            videoUrl.startsWith("http://") -> {
                videoUrl.replace("http://localhost:8000", baseUrl)
                    .replace("http://127.0.0.1:8000", baseUrl)
            }
            // HTTPS URLs - convert to HTTP for local development
            videoUrl.startsWith("https://") -> {
                videoUrl.replace("https://", "http://")
                    .replace("https://localhost:8000", baseUrl)
                    .replace("https://127.0.0.1:8000", baseUrl)
            }
            // Unknown format, return as-is
            else -> videoUrl
        }
    }

    // =========================
    // REFRESH ON RESUME
    // =========================
    override fun onResume() {
        super.onResume()
        // Optionally refresh videos when returning to this screen
        // loadVideos()
    }
}

