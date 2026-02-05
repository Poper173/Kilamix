package com.itech.kilamix.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.databinding.ActivityCreatorBinding
import com.itech.kilamix.model.Video
import com.itech.kilamix.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatorBinding
    private lateinit var authToken: String
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: VideoAdapter

    companion object {
        private const val TAG = "CreatorActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get token from SessionManager
        sessionManager = SessionManager(this)
        authToken = "Bearer ${sessionManager.getToken() ?: ""}"

        setupRecyclerView()
        loadCreatorData()

        binding.btnUploadVideo.setOnClickListener {
            val intent = Intent(this, UploadVideoActivity::class.java)
            intent.putExtra("token", authToken.removePrefix("Bearer "))
            startActivity(intent)
        }

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCreatorData() // Refresh when returning from upload/edit
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter(emptyList(), {}, {}, {}, { video ->
            showVideoOptions(video)
        })

        binding.recyclerViewVideos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewVideos.adapter = adapter
    }

    private fun loadCreatorData() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        // Load channel info
        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        apiService.getCreatorChannel(authToken).enqueue(object : Callback<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.ChannelResponse>> {
            override fun onResponse(
                call: Call<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.ChannelResponse>>,
                response: Response<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.ChannelResponse>>
            ) {
                binding.progressBar.visibility = android.view.View.GONE
                
                // Handle both "success" and "message" based responses
                val channelResponse = response.body()
                val channel = when {
                    channelResponse?.data != null -> channelResponse.data
                    else -> {
                        // Try to get from error body or use default values
                        null
                    }
                }
                
                if (response.isSuccessful && channel != null) {
                    binding.tvTotalVideos.text = channel.videosCount.toString()
                    binding.tvTotalViews.text = channel.totalViews.toString()
                    loadMyVideos()
                } else {
                    // Still load videos even if channel fails
                    loadMyVideos()
                }
            }

            override fun onFailure(call: Call<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.ChannelResponse>>, t: Throwable) {
                binding.progressBar.visibility = android.view.View.GONE
                // Try to load videos anyway
                loadMyVideos()
            }
        })
    }

    private fun loadMyVideos() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        // Use paginated endpoint for better reliability
        apiService.getMyVideosPaginated(authToken, 1, 20).enqueue(object : Callback<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>> {
            override fun onResponse(
                call: Call<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>>,
                response: Response<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>>
            ) {
                binding.progressBar.visibility = android.view.View.GONE
                
                Log.d(TAG, "My videos response code: ${response.code()}")
                Log.d(TAG, "My videos response body: ${response.body()}")
                
                if (response.isSuccessful) {
                    val videoResponse = response.body()
                    val videos = videoResponse?.data
                    
                    if (videos != null && videos.isNotEmpty()) {
                        adapter.updateVideos(videos)
                        binding.tvTotalVideos.text = videos.size.toString()
                    } else {
                        // Empty or no videos
                        adapter.updateVideos(emptyList())
                        Toast.makeText(this@CreatorActivity, "No videos uploaded yet", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Try alternative endpoint without pagination
                    fallbackLoadMyVideos()
                }
            }

            override fun onFailure(call: Call<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>>, t: Throwable) {
                binding.progressBar.visibility = android.view.View.GONE
                Log.e(TAG, "Failed to load videos: ${t.message}")
                Toast.makeText(this@CreatorActivity, "Error loading videos: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun fallbackLoadMyVideos() {
        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        apiService.getMyVideos(authToken).enqueue(object : Callback<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>> {
            override fun onResponse(
                call: Call<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>>,
                response: Response<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>>
            ) {
                binding.progressBar.visibility = android.view.View.GONE
                
                if (response.isSuccessful) {
                    val videos = response.body()?.data
                    if (videos != null) {
                        adapter.updateVideos(videos)
                    } else {
                        adapter.updateVideos(emptyList())
                    }
                } else {
                    Toast.makeText(this@CreatorActivity, "Failed to load videos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.itech.kilamix.api.ApiResponse<List<com.itech.kilamix.model.Video>>>, t: Throwable) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@CreatorActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showVideoOptions(video: Video) {
        android.app.AlertDialog.Builder(this)
            .setTitle(video.title)
            .setItems(arrayOf("Edit", "Delete", "View Stats")) { _, which ->
                when (which) {
                    0 -> editVideo(video)
                    1 -> deleteVideo(video.id)
                    2 -> viewVideoStats(video)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editVideo(video: Video) {
        val intent = Intent(this, EditVideoActivity::class.java)
        intent.putExtra("token", authToken.removePrefix("Bearer "))
        intent.putExtra("video_id", video.id)
        intent.putExtra("video_title", video.title)
        intent.putExtra("video_description", video.description ?: "")
        startActivity(intent)
    }

    private fun deleteVideo(videoId: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Video")
            .setMessage("Are you sure you want to delete this video?")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(videoId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(videoId: Int) {
        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        apiService.deleteVideo(authToken, videoId).enqueue(object : Callback<com.itech.kilamix.api.ApiResponse<Void>> {
            override fun onResponse(
                call: Call<com.itech.kilamix.api.ApiResponse<Void>>,
                response: Response<com.itech.kilamix.api.ApiResponse<Void>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@CreatorActivity, "Video deleted successfully", Toast.LENGTH_SHORT).show()
                    loadCreatorData() // Refresh data
                } else {
                    Toast.makeText(this@CreatorActivity, "Failed to delete video", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.itech.kilamix.api.ApiResponse<Void>>, t: Throwable) {
                Toast.makeText(this@CreatorActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun viewVideoStats(video: Video) {
        Toast.makeText(this, "Stats for: ${video.title}\nViews: ${video.views_count}\nLikes: ${video.likes_count}", Toast.LENGTH_LONG).show()
    }

    private fun logout() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        apiService.logout(authToken).enqueue(object : retrofit2.Callback<com.itech.kilamix.api.ApiResponse<Void>> {
            override fun onResponse(
                call: retrofit2.Call<com.itech.kilamix.api.ApiResponse<Void>>,
                response: retrofit2.Response<com.itech.kilamix.api.ApiResponse<Void>>
            ) {
                // Clear session regardless of API response
                sessionManager.logout()

                Toast.makeText(this@CreatorActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()

                // Navigate to login
                val intent = Intent(this@CreatorActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            override fun onFailure(call: retrofit2.Call<com.itech.kilamix.api.ApiResponse<Void>>, t: Throwable) {
                // Clear session even if API call fails
                sessionManager.logout()

                Toast.makeText(this@CreatorActivity, "Logged out locally", Toast.LENGTH_SHORT).show()

                // Navigate to login
                val intent = Intent(this@CreatorActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        })
    }
}

