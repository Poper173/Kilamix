package com.itech.kilamix.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.databinding.ActivityEditVideoBinding
import com.itech.kilamix.model.VideoUpdateRequest
import com.itech.kilamix.utils.SessionManager

class EditVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditVideoBinding
    private lateinit var authToken: String
    private lateinit var sessionManager: SessionManager
    private var videoId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get token from SessionManager
        sessionManager = SessionManager(this)
        authToken = "Bearer ${sessionManager.getToken() ?: ""}"
        
        videoId = intent.getIntExtra("video_id", 0)

        // Load existing data
        binding.etTitle.setText(intent.getStringExtra("video_title") ?: "")
        binding.etDescription.setText(intent.getStringExtra("video_description") ?: "")

        binding.btnUpdate.setOnClickListener {
            updateVideo()
        }
    }

    private fun updateVideo() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter video title", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnUpdate.isEnabled = false

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        val updateRequest = VideoUpdateRequest(
            title = title,
            description = description,
            category_id = 1 // You can add category spinner if needed
        )

        apiService.updateVideo(authToken, videoId, updateRequest)
            .enqueue(object : retrofit2.Callback<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>> {
                override fun onResponse(
                    call: retrofit2.Call<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>>,
                    response: retrofit2.Response<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>>
                ) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnUpdate.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditVideoActivity, "Video updated successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditVideoActivity, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>>,
                    t: Throwable
                ) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnUpdate.isEnabled = true
                    Toast.makeText(this@EditVideoActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
