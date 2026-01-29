package com.itech.kilamix.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.itechtube.R

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
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val api = ApiClient.retrofit.create(ApiService::class.java)
        val session = SessionManager(this)

        btnLogin.setOnClickListener {

            val request = LoginRequest(
                email.text.toString(),
                password.text.toString()
            )

            api.login(request).enqueue(object : Callback<AuthResponse> {

                override fun onResponse(
                    call: Call<AuthResponse>,
                    response: Response<AuthResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {

                        val data = response.body()!!.data
                        session.saveAuth(data.token, data.user.role)

                        when (data.user.role) {
                            "user" -> startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                            "creator" -> startActivity(Intent(this@LoginActivity, CreatorActivity::class.java))
                            "admin" -> startActivity(Intent(this@LoginActivity, AdminActivity::class.java))
                        }

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
