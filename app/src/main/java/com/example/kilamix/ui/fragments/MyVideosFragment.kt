package com.itech.kilamix.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.itech.kilamix.R
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.databinding.FragmentMyVideosBinding
import com.itech.kilamix.model.Video
import com.itech.kilamix.ui.VideoAdapter
import com.itech.kilamix.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyVideosFragment : Fragment() {

    private var _binding: FragmentMyVideosBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: VideoAdapter
    private lateinit var authToken: String
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "MyVideosFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get token from SessionManager
        sessionManager = SessionManager(requireContext())
        authToken = "Bearer ${sessionManager.getToken() ?: ""}"

        setupRecyclerView()
        loadMyVideos()
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter(emptyList(), {}, {}, {}, { video ->
            // Handle video click (edit or delete)
            showVideoOptions(video)
        })

        binding.recyclerViewVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVideos.adapter = adapter
    }

    private fun loadMyVideos() {
        binding.progressBar.visibility = View.VISIBLE

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        apiService.getMyVideos(authToken).enqueue(object : Callback<com.itech.kilamix.api.ApiResponse<List<Video>>> {
            override fun onResponse(
                call: Call<com.itech.kilamix.api.ApiResponse<List<Video>>>,
                response: Response<com.itech.kilamix.api.ApiResponse<List<Video>>>
            ) {
                binding.progressBar.visibility = View.GONE

                Log.d(TAG, "Load videos response code: ${response.code()}")
                Log.d(TAG, "Load videos response body: ${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val videos = response.body()?.data ?: emptyList()
                    adapter.updateVideos(videos)
                    
                    if (videos.isEmpty()) {
                        Toast.makeText(requireContext(), "No videos uploaded yet", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Parse error message
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorResponse(errorBody)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Failed to load videos: $errorMessage")
                }
            }

            override fun onFailure(call: Call<com.itech.kilamix.api.ApiResponse<List<Video>>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Load videos failure: ${t.message}", t)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showVideoOptions(video: Video) {
        // Simple dialog to edit or delete video
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(video.title)
            .setItems(arrayOf("Edit Video", "Delete Video")) { _, which ->
                when (which) {
                    0 -> editVideo(video)
                    1 -> confirmDeleteVideo(video)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editVideo(video: Video) {
        Toast.makeText(requireContext(), "Edit: ${video.title}", Toast.LENGTH_SHORT).show()
        // You can open an edit activity here
    }

    private fun confirmDeleteVideo(video: Video) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Video")
            .setMessage("Are you sure you want to delete \"${video.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteVideo(video.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVideo(videoId: Int) {
        binding.progressBar.visibility = View.VISIBLE

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        apiService.deleteVideo(authToken, videoId).enqueue(object : Callback<com.itech.kilamix.api.ApiResponse<Void>> {
            override fun onResponse(
                call: Call<com.itech.kilamix.api.ApiResponse<Void>>,
                response: Response<com.itech.kilamix.api.ApiResponse<Void>>
            ) {
                binding.progressBar.visibility = View.GONE

                Log.d(TAG, "Delete response code: ${response.code()}")
                Log.d(TAG, "Delete response body: ${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(requireContext(), "Video deleted successfully", Toast.LENGTH_SHORT).show()
                    loadMyVideos() // Refresh list
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorResponse(errorBody)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Delete failed: $errorMessage")
                }
            }

            override fun onFailure(call: Call<com.itech.kilamix.api.ApiResponse<Void>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Delete failure: ${t.message}", t)
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Parse error messages from backend response
     */
    private fun parseErrorResponse(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) {
            return "Operation failed"
        }

        return try {
            // Try to get message field
            val message = errorBody.substringAfter("\"message\":\"")
                .substringBefore("\"")
            if (message.isNotEmpty()) {
                message
            } else {
                "Operation failed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing error body: ${e.message}")
            "Operation failed"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

