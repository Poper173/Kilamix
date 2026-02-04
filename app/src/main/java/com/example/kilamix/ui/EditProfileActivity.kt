package com.itech.kilamix.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.api.ApiResponse
import com.itech.kilamix.databinding.ActivityEditProfileBinding
import com.itech.kilamix.model.ChannelResponse
import com.itech.kilamix.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var authToken: String
    private lateinit var sessionManager: SessionManager
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 100
        private const val TAG = "EditProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get token from SessionManager
        sessionManager = SessionManager(this)
        authToken = "Bearer ${sessionManager.getToken() ?: ""}"

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnChangeImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter channel name", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSave.isEnabled = false

        val apiService = ApiClient.retrofit.create(com.itech.kilamix.api.ApiService::class.java)

        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())

        var avatarPart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            try {
                // Copy file from content URI to a temporary file in cache
                val file = copyUriToTempFile(uri)
                if (file != null && file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                    Log.d(TAG, "Avatar file prepared: ${file.name}, size: ${file.length()} bytes")
                } else {
                    Log.e(TAG, "Failed to create temp file for avatar")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing avatar: ${e.message}")
            }
        }

        apiService.updateChannelProfile(authToken, nameBody, descBody, avatarPart)
            .enqueue(object : retrofit2.Callback<ApiResponse<ChannelResponse>> {
                override fun onResponse(
                    call: retrofit2.Call<ApiResponse<ChannelResponse>>,
                    response: retrofit2.Response<ApiResponse<ChannelResponse>>
                ) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnSave.isEnabled = true

                    Log.d(TAG, "Update response code: ${response.code()}")
                    Log.d(TAG, "Update response body: ${response.body()}")

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // Parse error message
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = parseUpdateErrors(errorBody)
                        Toast.makeText(this@EditProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(
                    call: retrofit2.Call<ApiResponse<ChannelResponse>>,
                    t: Throwable
                ) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnSave.isEnabled = true
                    Log.e(TAG, "Update failure: ${t.message}", t)
                    Toast.makeText(this@EditProfileActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Copy content URI to a temporary file in cache directory
     * This works on all Android versions including Android 10+
     */
    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val fileName = getFileNameFromUri(uri) ?: "avatar_${System.currentTimeMillis()}.jpg"
            val tempFile = File(cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.exists()) {
                Log.d(TAG, "Temp file created: ${tempFile.absolutePath}")
                tempFile
            } else {
                Log.e(TAG, "Failed to create temp file")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file: ${e.message}")
            null
        }
    }

    /**
     * Get file name from content URI
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        return fileName ?: run {
            // Fallback: generate name based on mime type
            val mimeType = contentResolver.getType(uri)
            when {
                mimeType?.contains("jpeg") == true -> "avatar_${System.currentTimeMillis()}.jpg"
                mimeType?.contains("png") == true -> "avatar_${System.currentTimeMillis()}.png"
                mimeType?.contains("webp") == true -> "avatar_${System.currentTimeMillis()}.webp"
                else -> "avatar_${System.currentTimeMillis()}.jpg"
            }
        }
    }

    /**
     * Parse error messages from backend response
     */
    private fun parseUpdateErrors(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) {
            return "Update failed"
        }

        return try {
            // Try to get message field
            val message = errorBody.substringAfter("\"message\":\"")
                .substringBefore("\"")
            if (message.isNotEmpty()) {
                message
            } else {
                "Update failed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing error body: ${e.message}")
            "Update failed"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfile.setImageURI(uri)
                Log.d(TAG, "Image selected: $uri")
            }
        }
    }
}
