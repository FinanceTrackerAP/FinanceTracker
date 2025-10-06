package com.dam.financetracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.databinding.ActivityLoginBinding
import com.dam.financetracker.models.AuthResult
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeAuthResult()
    }

    private fun setupViews() {
        binding.apply {
            // Botón de login
            btnLogin.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (validateLoginForm(email, password)) {
                    loginUser(email, password)
                }
            }

            // Link para registro
            tvRegisterLink.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }

            // Link para recuperar contraseña
            tvForgotPassword.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isNotEmpty()) {
                    showForgotPasswordDialog(email)
                } else {
                    Toast.makeText(this@LoginActivity, "Ingresa tu email primero", Toast.LENGTH_SHORT).show()
                }
            }

            // Botón de Google (por ahora solo mensaje)
            btnGoogleSignIn.setOnClickListener {
                Toast.makeText(this@LoginActivity, "Google Sign-In próximamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        var isValid = true

        // Validar email
        val emailValidation = ValidationUtils.validateEmail(email)
        if (!emailValidation.isValid) {
            binding.tilEmail.error = emailValidation.errorMessage
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validar password
        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.isValid) {
            binding.tilPassword.error = passwordValidation.errorMessage
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            // Guardar el email para usarlo en modo local
            saveUserEmail(email)
            viewModel.loginUser(email, password)
        }
    }

    private fun observeAuthResult() {
        lifecycleScope.launch {
            viewModel.authResult.collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        showLoading(true)
                    }
                    is AuthResult.Success -> {
                        showLoading(false)
                        Toast.makeText(this@LoginActivity, "Bienvenido", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                        finish()
                    }
                    is AuthResult.Error -> {
                        showLoading(false)
                        Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                btnLogin.isEnabled = false
                btnLogin.text = "Iniciando sesión..."
            } else {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
                btnLogin.text = "Iniciar Sesión"
            }
        }
    }

    private fun showForgotPasswordDialog(email: String) {
        lifecycleScope.launch {
            viewModel.resetPassword(email)
            Toast.makeText(this@LoginActivity, "Se envió enlace de recuperación a tu email", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveUserEmail(email: String) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("user_email", email)
            .apply()
    }
}