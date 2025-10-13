package com.dam.financetracker.ui.settings

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityChangePasswordBinding
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.utils.ValidationUtils


class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupBottomNavigation()
    }

    private fun setupViews() {
        // Volver atrás (flecha con rotación de 180 en el layout)
        binding.btnBack.setOnClickListener { finish() }

        // Mock de Guardar Cambios
        binding.btnSaveChanges.setOnClickListener {
            val currentPass = binding.etCurrentPassword.text.toString()
            val newPass = binding.etNewPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // Aquí se usaría ValidationUtils.validatePassword, validatePasswordConfirmation
            // para la validación real antes de llamar a Firebase.

            if (newPass.isEmpty() || currentPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos de contraseña.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Validando y Guardando nueva contraseña... (Lógica de Firebase pendiente)", Toast.LENGTH_LONG).show()
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
                // Si el item seleccionado es el de Transacciones, podrías volver atrás
                R.id.nav_transactions -> {
                    // Si TransactionsActivity es la anterior, podríamos usar finish(), o navegar explícitamente
                    // Por ahora solo aseguramos el flujo de Ajustes a Home
                    true
                }
                else -> false
            }
        }
    }
}