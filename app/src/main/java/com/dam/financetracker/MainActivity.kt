package com.dam.financetracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.databinding.ActivityMainBinding
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
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
                    // Usuario autenticado - por ahora ir al login también
                    binding.tvLoading.text = "Usuario autenticado..."
                    delay(1000)
                    // TODO: Ir al Dashboard cuando lo creemos
                    navigateToLogin()
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