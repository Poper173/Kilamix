package com.itech.kilamix.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.itech.kilamix.R
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.api.ApiService
import com.itech.kilamix.model.AuthResponse
import com.itech.kilamix.model.LoginRequest
import com.itech.kilamix.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        // 🔐 AUTO-REDIRECT IF ALREADY LOGGED IN
        session.getRole()?.let { role ->
            val intent = when (role) {
                "admin" -> Intent(this, AdminActivity::class.java)
                "creator" -> Intent(this, CreatorActivity::class.java)
                else -> Intent(this, HomeActivity::class.java)
            }
            startActivity(intent)
            finish()
            return // stop loading login screen
        }

        // 👇 Only runs if user NOT logged in
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        val api = ApiClient.retrofit.create(ApiService::class.java)

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(emailText, passwordText)

            api.login(request).enqueue(object : Callback<AuthResponse> {

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!.data
                        session.saveAuth(data.token, data.user.role)

                        // Role-based redirect after login
                        val intent = when (data.user.role) {
                            "admin" -> Intent(this@LoginActivity, AdminActivity::class.java)
                            "creator" -> Intent(this@LoginActivity, CreatorActivity::class.java)
                            else -> Intent(this@LoginActivity, HomeActivity::class.java)
                        }
                        intent.putExtra("token", data.token)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
