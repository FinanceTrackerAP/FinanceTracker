package com.dam.financetracker.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.databinding.ActivityRegisterBinding
import com.dam.financetracker.models.AuthResult
import com.dam.financetracker.models.Business
import com.dam.financetracker.utils.ValidationUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeAuthResult()
    }

    private fun setupViews() {
        binding.apply {
            // Botón de registro
            btnRegister.setOnClickListener {
                val fullName = etFullName.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (validateRegisterForm(fullName, email, phone, password)) {
                    // Guardar el email para usarlo en modo local
                    saveUserEmail(email)
                    registerUser(fullName, email, phone, password)
                }
            }

            // Link para ir a login
            tvLoginLink.setOnClickListener {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun validateRegisterForm(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Boolean {
        var isValid = true

        // Validar nombre completo
        if (fullName.isBlank()) {
            binding.tilFullName.error = "El nombre completo es requerido"
            isValid = false
        } else if (fullName.length < 2) {
            binding.tilFullName.error = "El nombre debe tener al menos 2 caracteres"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        // Validar email
        val emailValidation = ValidationUtils.validateEmail(email)
        if (!emailValidation.isValid) {
            binding.tilEmail.error = emailValidation.errorMessage
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validar teléfono
        val phoneValidation = ValidationUtils.validatePhone(phone)
        if (!phoneValidation.isValid) {
            binding.tilPhone.error = phoneValidation.errorMessage
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        // Validar contraseña
        val passwordValidation = ValidationUtils.validatePassword(password)
        if (!passwordValidation.isValid) {
            binding.tilPassword.error = passwordValidation.errorMessage
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun registerUser(fullName: String, email: String, phone: String, password: String) {
        lifecycleScope.launch {
            // Crear datos del negocio con la información básica
            // Más tarde podemos agregar campos específicos del negocio
            val businessData = Business(
                name = "$fullName - Negocio", // Temporal, luego agregar campo específico
                businessType = "General", // Temporal
                ruc = "", // Por ahora vacío, luego agregar campo
                address = "", // Por ahora vacío
                phone = phone
            )

            viewModel.registerUser(email, password, businessData)
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
                        Toast.makeText(this@RegisterActivity, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()

                        // Ir al login para que inicie sesión
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    }
                    is AuthResult.Error -> {
                        showLoading(false)
                        Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        // Estado idle, no hacer nada
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                btnRegister.isEnabled = false
                btnRegister.text = "Creando cuenta..."
            } else {
                progressBar.visibility = View.GONE
                btnRegister.isEnabled = true
                btnRegister.text = "Crear Cuenta"
            }
        }
    }
    
    private fun saveUserEmail(email: String) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("user_email", email)
            .apply()
    }
}