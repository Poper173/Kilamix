package com.itech.kilamix.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.databinding.ActivityUploadVideoBinding
import com.itech.kilamix.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

class UploadVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadVideoBinding
    private lateinit var authToken: String
    private lateinit var sessionManager: SessionManager
    private var selectedVideoUri: Uri? = null
    private var selectedThumbnailUri: Uri? = null

    companion object {
        private const val PICK_VIDEO_REQUEST = 101
        private const val PICK_THUMBNAIL_REQUEST = 102
        private const val TAG = "UploadVideoActivity"
        private const val MAX_VIDEO_SIZE_MB = 500L
        private const val MIN_VIDEO_SIZE_BYTES = 1024L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get token from SessionManager
        sessionManager = SessionManager(this)
        authToken = "Bearer ${sessionManager.getToken() ?: ""}"

        setupCategorySpinner()
        setupClickListeners()
    }

    private fun setupCategorySpinner() {
        // Simple categories - you can load from API later
        val categories = listOf(
            "Technology" to 1,
            "Gaming" to 2,
            "Music" to 3,
            "Education" to 4,
            "Entertainment" to 5
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSelectVideo.setOnClickListener {
            openVideoPicker()
        }

        binding.btnSelectThumbnail.setOnClickListener {
            openThumbnailPicker()
        }

        binding.btnUpload.setOnClickListener {
            uploadVideo()
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
    }

    private fun openThumbnailPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_THUMBNAIL_REQUEST)
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        return cursor.getLong(sizeIndex)
                    }
                }
                0L
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size: ${e.message}")
            0L
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val df = DecimalFormat("#.##")
        return when {
            bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    /**
     * Parse validation errors from backend response
     * Backend format: {"message":"Validation failed","errors":{"field":["error message"]}}
     */
    private fun parseValidationErrors(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) {
            return "Upload failed: Unknown error"
        }

        return try {
            // Try to parse as JSON with errors object
            if (errorBody.contains("\"errors\"")) {
                val errorsJson = errorBody.substringAfter("\"errors\":")
                    .substringBefore("}}")
                    .trim()

                // Build a readable error message
                val errorMessages = mutableListOf<String>()
                
                // Common field patterns
                val fields = listOf("title", "description", "category_id", "video", "thumbnail")
                
                for (field in fields) {
                    if (errorsJson.contains("\"$field\"")) {
                        val fieldSection = errorsJson.substringAfter("\"$field\":")
                            .substringBefore("}")
                            .substringBefore(",\"")
                        
                        if (fieldSection.contains("[")) {
                            val messages = fieldSection.substringAfter("[")
                                .substringBefore("]")
                                .replace("\"", "")
                                .split(",")
                                .filter { it.isNotBlank() }
                            
                            if (messages.isNotEmpty()) {
                                errorMessages.add("$field: ${messages.joinToString(", ")}")
                            }
                        }
                    }
                }

                if (errorMessages.isNotEmpty()) {
                    errorMessages.joinToString("\n")
                } else {
                    // Try to get the message field
                    val message = errorBody.substringAfter("\"message\":\"")
                        .substringBefore("\"")
                    if (message.isNotEmpty()) {
                        message
                    } else {
                        "Upload failed: Validation error"
                    }
                }
            } else {
                // Try to get the message field
                val message = errorBody.substringAfter("\"message\":\"")
                    .substringBefore("\"")
                if (message.isNotEmpty()) {
                    message
                } else {
                    "Upload failed: Unknown error"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing validation errors: ${e.message}")
            "Upload failed: Unknown error"
        }
    }

    private fun validateVideoFile(uri: Uri): Pair<Boolean, String> {
        val fileName = getFileName(uri)
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val supportedFormats = listOf("mp4", "mov", "avi", "mkv", "webm", "flv", "wmv")

        if (extension.isNotEmpty() && extension !in supportedFormats) {
            return false to "Unsupported format: .$extension"
        }

        val fileSize = getFileSize(uri)
        if (fileSize <= 0) {
            return false to "Could not determine file size"
        }

        val maxSizeBytes = MAX_VIDEO_SIZE_MB * 1024 * 1024
        if (fileSize > maxSizeBytes) {
            return false to "File too large (${formatFileSize(fileSize)}). Max: ${MAX_VIDEO_SIZE_MB}MB"
        }

        if (fileSize < MIN_VIDEO_SIZE_BYTES) {
            return false to "File too small"
        }

        Log.d(TAG, "File validated: $fileName, size: ${formatFileSize(fileSize)}")
        return true to ""
    }

    private fun uploadVideo() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val categories = listOf(1, 2, 3, 4, 5)
        val categoryId = categories[binding.spinnerCategory.selectedItemPosition]

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter video title", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please select a video file", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate video file
        val (isValid, errorMessage) = validateVideoFile(selectedVideoUri!!)
        if (!isValid) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnUpload.isEnabled = false

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
        // Fix: category_id should use correct media type "text/plain" not "plain/text"
        val categoryBody = categoryId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        selectedVideoUri?.let { videoUri ->
            val videoFile = File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
            
            try {
                contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(videoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (videoFile.exists()) {
                    val requestFile = videoFile.asRequestBody("video/*".toMediaTypeOrNull())
                    val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)

                    // Handle optional thumbnail
                    val thumbnailPart = selectedThumbnailUri?.let { thumbUri ->
                        val thumbFile = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                        contentResolver.openInputStream(thumbUri)?.use { input ->
                            FileOutputStream(thumbFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (thumbFile.exists()) {
                            val thumbRequestFile = thumbFile.asRequestBody("image/*".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("thumbnail", thumbFile.name, thumbRequestFile)
                        } else null
                    }

                    Log.d(TAG, "Uploading video: ${videoFile.name}, size: ${videoFile.length()} bytes")
                    Log.d(TAG, "Title: $title, Description: $description, Category ID: $categoryId")

                    apiService.uploadVideo(authToken, titleBody, descBody, categoryBody, videoPart, thumbnailPart)
                        .enqueue(object : retrofit2.Callback<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>> {
                            override fun onResponse(
                                call: retrofit2.Call<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>>,
                                response: retrofit2.Response<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>>
                            ) {
                                binding.progressBar.visibility = android.view.View.GONE
                                binding.btnUpload.isEnabled = true

                                Log.d(TAG, "Upload response code: ${response.code()}")
                                Log.d(TAG, "Upload response body: ${response.body()}")
                                
                                // Log detailed error response for debugging
                                if (!response.isSuccessful) {
                                    val errorBody = response.errorBody()?.string()
                                    Log.e(TAG, "Upload response code: ${response.code()}")
                                    Log.e(TAG, "Upload error body: $errorBody")
                                    
                                    // Parse validation errors from backend
                                    val errorMessage = parseValidationErrors(errorBody)
                                    Toast.makeText(this@UploadVideoActivity, errorMessage, Toast.LENGTH_LONG).show()
                                    return
                                }

                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(this@UploadVideoActivity, "Video uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    val errorMessage = response.body()?.message ?: response.message() ?: "Unknown error"
                                    Toast.makeText(this@UploadVideoActivity, "Upload failed: $errorMessage", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onFailure(
                                call: retrofit2.Call<com.itech.kilamix.api.ApiResponse<com.itech.kilamix.model.Video>>,
                                t: Throwable
                            ) {
                                binding.progressBar.visibility = android.view.View.GONE
                                binding.btnUpload.isEnabled = true
                                Log.e(TAG, "Upload failure: ${t.message}", t)
                                Toast.makeText(this@UploadVideoActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnUpload.isEnabled = true
                    Toast.makeText(this, "File processing failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnUpload.isEnabled = true
                Log.e(TAG, "Error processing video: ${e.message}", e)
                Toast.makeText(this, "Error processing file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    fileName = cursor.getString(index)
                }
            }
        }
        return fileName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_VIDEO_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        selectedVideoUri = uri
                        val fileName = getFileName(uri)
                        val fileSize = getFileSize(uri)
                        binding.tvSelectedFile.text = if (fileSize > 0) {
                            "Selected: $fileName (${formatFileSize(fileSize)})"
                        } else {
                            "Selected: $fileName"
                        }
                    }
                }
            }
            PICK_THUMBNAIL_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        selectedThumbnailUri = uri
                        val fileName = getFileName(uri)
                        binding.tvSelectedFile.text = "${binding.tvSelectedFile.text}\nThumbnail: $fileName"
                    }
                }
            }
        }
    }
}
