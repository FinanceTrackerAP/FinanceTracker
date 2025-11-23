package com.dam.financetracker.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivitySettingsBinding
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import com.dam.financetracker.ui.category.CategoryActivity
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.ui.reports.ReportsActivity
import com.dam.financetracker.ui.transaction.TransactionActivity
import com.dam.financetracker.ui.transaction.TransactionHistoryActivity
import com.dam.financetracker.utils.RoleManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private var currentUserRole: UserRole = UserRole.OWNER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserInfo()
        setupViews()
        
        // Configurar navegación después de cargar el rol del usuario
        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            currentUserRole = user?.role ?: UserRole.OWNER
            setupBottomNavigation()
        }
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
                    
                    // HU-007: Guardar rol actual
                    currentUserRole = user.role

                    // Llenar formulario de edición
                    binding.etFullName.setText(fullName)
                    binding.etEmail.setText(user.email) // Campo Email deshabilitado
                    binding.etPhone.setText(phoneNumber)
                    
                    // TEMPORAL: Mostrar rol actual para testing
                    showCurrentRole()

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
        
        // HU-007 y HU-008: Selector de rol
        setupRoleSpinner()
    }
    
    /**
     * HU-007 y HU-008: Configurar selector de rol
     */
    private fun setupRoleSpinner() {
        val roles = listOf(
            "Propietario",
            "Empleado",
            "Contador"
        )
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRole.adapter = adapter
        
        // Establecer rol actual
        val currentPosition = when (currentUserRole) {
            UserRole.OWNER -> 0
            UserRole.EMPLOYEE -> 1
            UserRole.ACCOUNTANT -> 2
        }
        binding.spinnerRole.setSelection(currentPosition)
        
        // Listener para cambios
        binding.spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRole = when (position) {
                    0 -> UserRole.OWNER
                    1 -> UserRole.EMPLOYEE
                    2 -> UserRole.ACCOUNTANT
                    else -> UserRole.OWNER
                }
                
                // Solo mostrar confirmación si cambió
                if (selectedRole != currentUserRole) {
                    showRoleChangeConfirmation(selectedRole)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }
    
    /**
     * Mostrar alerta de confirmación antes de cambiar rol
     */
    private fun showRoleChangeConfirmation(newRole: UserRole) {
        val roleName = RoleManager.getRoleName(newRole)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Confirmar cambio de rol")
            .setMessage(
                "¿Estás seguro que deseas cambiar tu rol a $roleName?\n\n" +
                "⚠️ ADVERTENCIA: Esta decisión no se puede revertir fácilmente.\n\n" +
                "Tendrás que cerrar sesión y volver a iniciar para que el cambio tenga efecto."
            )
            .setPositiveButton("Confirmar") { _, _ ->
                changeUserRole(newRole)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                // Restaurar selección anterior
                val previousPosition = when (currentUserRole) {
                    UserRole.OWNER -> 0
                    UserRole.EMPLOYEE -> 1
                    UserRole.ACCOUNTANT -> 2
                }
                binding.spinnerRole.setSelection(previousPosition)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun changeUserRole(newRole: UserRole) {
        lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    // Actualizar rol en Firestore
                    firestore.collection("users")
                        .document(user.uid)
                        .update("role", newRole.name)
                        .await()
                    
                    currentUserRole = newRole
                    
                    Toast.makeText(
                        this@SettingsActivity,
                        "✅ Rol cambiado a: ${RoleManager.getRoleName(newRole)}\n\nCierra sesión y vuelve a entrar",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    showCurrentRole()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    "❌ Error al cambiar rol: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showCurrentRole() {
        // Temporal: Agregar info al nombre (SIN POPUP MOLESTO)
        val currentName = binding.tvUserFullName.text.toString()
        if (!currentName.contains("•")) {
            binding.tvUserFullName.text = "$currentName • ${RoleManager.getRoleName(currentUserRole)}"
        }
    }

    private fun setupBottomNavigation() {
        val menu = binding.bottomNavigation.bottomNavigation.menu
        
        // HU-007 y HU-008: Ocultar opciones según el rol
        menu.findItem(R.id.nav_home)?.isVisible = RoleManager.canAccessDashboard(currentUserRole)
        menu.findItem(R.id.nav_reports)?.isVisible = RoleManager.canAccessReports(currentUserRole)
        menu.findItem(R.id.nav_settings)?.isVisible = RoleManager.canAccessSettings(currentUserRole)
        
        // Aseguramos que el botón de Ajustes esté seleccionado
        binding.bottomNavigation.bottomNavigation.selectedItemId = R.id.nav_settings
        
        binding.bottomNavigation.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    // Si puede crear, abrir formulario; si no, abrir historial
                    if (RoleManager.canCreateTransactions(currentUserRole)) {
                        startActivity(Intent(this, TransactionActivity::class.java))
                    } else {
                        startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    }
                    finish()
                    true
                }
                R.id.nav_reports -> {
                    if (RoleManager.canAccessReports(currentUserRole)) {
                        startActivity(Intent(this, ReportsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }
}
