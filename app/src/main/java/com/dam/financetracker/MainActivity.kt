package com.dam.financetracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.databinding.ActivityMainBinding
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.ui.reports.ReportsActivity
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()

        checkAuthState()
    }

    private fun checkAuthState() {
        lifecycleScope.launch {
            // Mostrar splash screen por 2 segundos
            delay(2000)

            try {
                if (authRepository.isUserLoggedIn()) {
                    // Usuario autenticado - redirigir según rol
                    binding.tvLoading.text = "Usuario autenticado..."
                    delay(1000)
                    
                    // HU-008: Redirigir según el rol del usuario
                    val user = authRepository.getCurrentUser()
                    val userRole = user?.role ?: UserRole.OWNER
                    
                    val intent = if (userRole == UserRole.ACCOUNTANT) {
                        // Contador va directo a Reportes
                        Intent(this@MainActivity, ReportsActivity::class.java)
                    } else {
                        // Owner y Empleado van al Dashboard
                        Intent(this@MainActivity, DashboardActivity::class.java)
                    }
                    
                    startActivity(intent)
                    finish()
                } else {
                    // Usuario no autenticado - ir al login
                    binding.tvLoading.text = "Redirigiendo..."
                    delay(500)
                    navigateToLogin()
                }

            } catch (e: Exception) {
                // Error al verificar estado - ir al login
                binding.tvLoading.text = "Error: ${e.message}"
                delay(1000)
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish() // Cerrar MainActivity para que no regrese con el botón atrás
    }
}