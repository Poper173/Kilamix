package com.itech.kilamix.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.itech.kilamix.R
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.api.ApiService
import com.itech.kilamix.model.RegisterRequest
import com.itech.kilamix.model.AuthResponse
import com.itech.kilamix.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val name = findViewById<EditText>(R.id.name)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val passwordConfirm = findViewById<EditText>(R.id.passwordConfirm)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val api = ApiClient.retrofit.create(ApiService::class.java)
        val session = SessionManager(this)

        btnRegister.setOnClickListener {

            // basic validation
            if (password.text.toString() != passwordConfirm.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                name = name.text.toString(),
                email = email.text.toString(),
                password = password.text.toString(),
                password_confirmation = passwordConfirm.text.toString(),
                role = "user"
            )

            api.register(request).enqueue(object : Callback<AuthResponse> {

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val data = response.body()!!.data
                        session.saveAuth(data.token, data.user.role)

                        startActivity(
                            Intent(this@RegisterActivity, HomeActivity::class.java)
                        )
                        finish()

                    } else {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(
                        this@RegisterActivity,
                        t.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }
}
