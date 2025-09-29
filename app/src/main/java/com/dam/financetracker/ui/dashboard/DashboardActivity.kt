package com.dam.financetracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.databinding.ActivityDashboardBinding
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserInfo()
        setupViews()
    }
    private fun loadUserInfo(){
        lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser().first()
                user?.let {
                    binding.tvWelcome.text = "Bienvenido"
                    binding.tvEmail.text = it.email
                }
            } catch (e: Exception){
                binding.tvWelcome.text= "Bienvenido"
            }
        }
    }
    private fun setupViews() {
        binding.btnLogout.setOnClickListener {
            authRepository.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}