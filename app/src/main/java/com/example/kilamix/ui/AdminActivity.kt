package com.itech.kilamix.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.itech.kilamix.R
import com.itech.kilamix.api.ApiClient
import com.itech.kilamix.api.ApiResponse
import com.itech.kilamix.api.ApiService
import com.itech.kilamix.model.AdminUser
import com.itech.kilamix.model.RoleUpdateRequest
import com.itech.kilamix.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminActivity : AppCompatActivity() {

    private lateinit var recyclerUsers: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var emptyState: android.widget.LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var txtTotalUsers: android.widget.TextView
    private lateinit var txtActiveUsers: android.widget.TextView
    private lateinit var txtInactiveUsers: android.widget.TextView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var tabLayout: TabLayout

    private lateinit var sessionManager: SessionManager
    private lateinit var userAdapter: UserAdapter
    private var users: MutableList<AdminUser> = mutableListOf()
    private var currentPage = 1
    private var isLastPage = false
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        loadUsers()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerUsers = findViewById(R.id.recyclerUsers)
        progressBar = findViewById(R.id.progressBar)
        emptyState = findViewById(R.id.emptyState)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        txtTotalUsers = findViewById(R.id.txtTotalUsers)
        txtActiveUsers = findViewById(R.id.txtActiveUsers)
        txtInactiveUsers = findViewById(R.id.txtInactiveUsers)

        sessionManager = SessionManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Admin Dashboard"
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onEditRole = { user -> showEditRoleDialog(user) },
            onToggleStatus = { user -> toggleUserStatus(user) },
            onDeleteUser = { user -> showDeleteConfirmation(user) }
        )

        recyclerUsers.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = userAdapter
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.purple_500)
        swipeRefresh.setOnRefreshListener {
            loadUsers()
        }
    }

    // =====================
    // LOAD USERS
    // =====================
    private fun loadUsers(resetList: Boolean = false) {
        if (isLoading) return
        
        if (resetList) {
            currentPage = 1
            isLastPage = false
            users.clear()
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
            return
        }

        showLoading(resetList)

        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        apiService.getAdminUsers("Bearer $token", currentPage, 20).enqueue(object : Callback<ApiResponse<List<AdminUser>>> {

            override fun onResponse(
                call: Call<ApiResponse<List<AdminUser>>>,
                response: Response<ApiResponse<List<AdminUser>>>
            ) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        val newUsers = apiResponse.data
                        
                        // Determine if this is the last page
                        // API returns meta with current_page and last_page
                        isLastPage = currentPage >= 2 || newUsers.size < 20
                        
                        users.addAll(newUsers)
                        updateAdapter()
                        updateStats(users)
                    } else {
                        if (resetList) showEmptyState()
                        Toast.makeText(
                            this@AdminActivity,
                            apiResponse.message ?: "Failed to load users",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    if (resetList) showEmptyState()
                    Toast.makeText(
                        this@AdminActivity,
                        "Failed to load users: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<AdminUser>>>, t: Throwable) {
                isLoading = false
                showLoading(false)
                swipeRefresh.isRefreshing = false
                if (resetList) showEmptyState()
                Toast.makeText(
                    this@AdminActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    /**
     * Load more users for pagination (call when reaching end of list)
     */
    private fun loadMoreUsers() {
        if (!isLastPage && !isLoading) {
            currentPage++
            loadUsers(resetList = false)
        }
    }

    /**
     * Refresh users (reset list and reload from page 1)
     */
    fun refreshUsers() {
        loadUsers(resetList = true)
    }

    // =====================
    // UPDATE USER ROLE
    // =====================
    private fun showEditRoleDialog(user: AdminUser) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_role, null)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val txtUserName = dialogView.findViewById<TextInputEditText>(R.id.txtUserName)

        // Setup role spinner
        val roles = arrayOf("user", "creator", "admin")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = roleAdapter

        // Set current role
        txtUserName.setText(user.name)
        val currentIndex = roles.indexOf(user.role.lowercase())
        if (currentIndex >= 0) {
            spinnerRole.setSelection(currentIndex)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit User Role")
            .setMessage("Change role for: ${user.email}")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newRole = spinnerRole.selectedItem.toString()
                updateUserRole(user, newRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserRole(user: AdminUser, newRole: String) {
        val token = sessionManager.getToken() ?: return

        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        val request = RoleUpdateRequest(newRole)

        apiService.updateUserRole("Bearer $token", user.id, request)
            .enqueue(object : Callback<ApiResponse<AdminUser>> {

                override fun onResponse(
                    call: Call<ApiResponse<AdminUser>>,
                    response: Response<ApiResponse<AdminUser>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            userAdapter.updateUser(apiResponse.data)
                            Toast.makeText(
                                this@AdminActivity,
                                "Role updated to ${apiResponse.data.role}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@AdminActivity,
                                apiResponse.message ?: "Failed to update role",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@AdminActivity,
                            "Failed to update role: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<AdminUser>>, t: Throwable) {
                    Toast.makeText(
                        this@AdminActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // =====================
    // TOGGLE USER STATUS
    // =====================
    private fun toggleUserStatus(user: AdminUser) {
        val token = sessionManager.getToken() ?: return

        val action = if (user.isActive) "deactivate" else "activate"

        AlertDialog.Builder(this)
            .setTitle("$action User")
            .setMessage("Are you sure you want to $action ${user.name}?")
            .setPositiveButton("Yes") { _, _ ->
                performToggleStatus(user)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performToggleStatus(user: AdminUser) {
        val token = sessionManager.getToken() ?: return

        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        apiService.toggleUserStatus("Bearer $token", user.id)
            .enqueue(object : Callback<ApiResponse<AdminUser>> {

                override fun onResponse(
                    call: Call<ApiResponse<AdminUser>>,
                    response: Response<ApiResponse<AdminUser>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.success && apiResponse.data != null) {
                            userAdapter.updateUser(apiResponse.data)
                            val status = if (apiResponse.data.isActive) "activated" else "deactivated"
                            Toast.makeText(
                                this@AdminActivity,
                                "User $status successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@AdminActivity,
                                apiResponse.message ?: "Failed to toggle status",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@AdminActivity,
                            "Failed to toggle status: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<AdminUser>>, t: Throwable) {
                    Toast.makeText(
                        this@AdminActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // =====================
    // DELETE USER
    // =====================
    private fun showDeleteConfirmation(user: AdminUser) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteUser(user: AdminUser) {
        val token = sessionManager.getToken() ?: return

        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        apiService.deleteUser("Bearer $token", user.id)
            .enqueue(object : Callback<ApiResponse<Void>> {

                override fun onResponse(
                    call: Call<ApiResponse<Void>>,
                    response: Response<ApiResponse<Void>>
                ) {
                    if (response.isSuccessful) {
                        userAdapter.removeUser(user.id)
                        updateStats(users)
                        Toast.makeText(
                            this@AdminActivity,
                            "User deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@AdminActivity,
                            "Failed to delete user: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                    Toast.makeText(
                        this@AdminActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // =====================
    // UI HELPERS
    // =====================
    private fun updateAdapter() {
        if (users.isEmpty()) {
            showEmptyState()
        } else {
            showContent()
            userAdapter.submitList(users.toList())
        }
    }

    private fun updateStats(userList: List<AdminUser>) {
        txtTotalUsers.text = userList.size.toString()
        txtActiveUsers.text = userList.count { it.isActive }.toString()
        txtInactiveUsers.text = userList.count { !it.isActive }.toString()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showContent() {
        emptyState.visibility = View.GONE
        recyclerUsers.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        recyclerUsers.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
    }

    private fun redirectToLogin() {
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // =====================
    // MENU
    // =====================
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
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

    private fun logout() {
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // =====================
    // REFRESH ON RESUME
    // =====================
    override fun onResume() {
        super.onResume()
        loadUsers()
    }
}

