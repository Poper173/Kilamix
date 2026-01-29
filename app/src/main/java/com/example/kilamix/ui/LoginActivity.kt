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
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        val api = ApiClient.retrofit.create(ApiService::class.java)
        val session = SessionManager(this)

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

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

                        startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
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
