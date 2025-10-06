package com.dam.financetracker.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R // Importar la clase R para acceder a los recursos
import com.dam.financetracker.databinding.ActivitySettingsBinding
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import com.dam.financetracker.ui.category.CategoryActivity // Se requiere si creaste la Activity en ese paquete
import kotlinx.coroutines.launch

/**
 * Activity para la Configuración de la cuenta y el perfil.
 * Implementa las funcionalidades de Cierre de Sesión y Navegación a la gestión de Categorías (HU-003).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserInfo()
        setupViews()
    }

    /**
     * Carga la información del usuario actual (email, businessId) del repositorio.
     */
    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    binding.tvUserEmail.text = user.email
                    // Usar string resource con placeholder (%s)
                    binding.tvBusinessName.text = getString(R.string.settings_business_id, user.businessId)
                } else {
                    // Usar string resources para los mensajes
                    binding.tvUserEmail.text = getString(R.string.error_session)
                    binding.tvBusinessName.text = getString(R.string.error_login_prompt)
                }
            } catch (e: Exception) {
                // Manejar la excepción 'e' para evitar la advertencia de parámetro no usado
                e.printStackTrace()
                Toast.makeText(this@SettingsActivity, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViews() {
        // Configuración de navegación de la Toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // HU-003: Cerrar Sesión
        binding.btnLogout.setOnClickListener {
            authRepository.signOut()
            Toast.makeText(this, getString(R.string.session_closed_message), Toast.LENGTH_SHORT).show()
            // Redirigir a Login y cerrar todas las actividades previas
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        // HU-003: Editar Perfil (Mock de UI)
        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(this, getString(R.string.feature_in_development), Toast.LENGTH_SHORT).show()
        }

        // HU-003: Navegar a Categorías (Asegúrate de que CategoryActivity esté en la ruta correcta)
        binding.cardCategories.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
    }
}
