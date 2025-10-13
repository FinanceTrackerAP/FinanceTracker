package com.dam.financetracker.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivitySettingsBinding
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import com.dam.financetracker.ui.category.CategoryActivity
import com.dam.financetracker.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserInfo()
        setupViews()
        setupBottomNavigation()
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    // Mocks de datos de perfil (Según el Figma)
                    // NOTA: Estos datos se cargarían realmente desde el documento 'users' de Firestore
                    val fullName = "Alfredo Guzmán Moscol"
                    val phoneNumber = "(51) 998 765 432"

                    // Mostrar datos en la cabecera
                    binding.tvUserFullName.text = fullName
                    binding.tvUserEmail.text = user.email

                    // Llenar formulario de edición
                    binding.etFullName.setText(fullName)
                    binding.etEmail.setText(user.email) // Campo Email deshabilitado
                    binding.etPhone.setText(phoneNumber)

                } else {
                    binding.tvUserFullName.text = "Usuario Desconocido"
                    binding.tvUserEmail.text = "Error de sesión. Vuelve a iniciar."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@SettingsActivity, "Error al cargar datos de perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViews() {
        // Guardar Cambios (HU-003: Editar Perfil - Mock)
        binding.btnSaveChanges.setOnClickListener {
            // Aquí iría la lógica real de validación y actualización en el repositorio (pendiente).
            val newName = binding.etFullName.text.toString()
            val newPhone = binding.etPhone.text.toString()
            Toast.makeText(this, "Guardado (Mock): $newName, $newPhone. (Lógica de persistencia pendiente)", Toast.LENGTH_LONG).show()
        }

        // Navegar a Administrar Categorías
        binding.btnManageCategories.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }

        // Navegar a Cambiar Contraseña (NUEVA ACTIVIDAD)
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // CERRAR SESIÓN (HU-003: Cierre de sesión)
        binding.btnLogout.setOnClickListener {
            authRepository.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun setupBottomNavigation() {
        // Aseguramos que el botón de Ajustes esté seleccionado
        binding.bottomNavigation.bottomNavigation.selectedItemId = R.id.nav_settings
        binding.bottomNavigation.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }
}
