package com.dam.financetracker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dam.financetracker.databinding.ActivityDashboardBinding
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.auth.LoginActivity
import com.dam.financetracker.ui.transaction.TransactionActivity
import com.dam.financetracker.ui.transaction.TransactionHistoryActivity
import com.dam.financetracker.ui.transaction.TransactionViewModel
import com.dam.financetracker.ui.settings.SettingsActivity // Importar SettingsActivity
import com.dam.financetracker.ui.category.CategoryActivity // Aunque no se usa directamente, se mantiene la importación si fuera necesario en el futuro
import com.dam.financetracker.ui.reports.ReportsActivity
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.utils.RoleManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val authRepository = AuthRepository()
    // Usar TransactionViewModel para conectar con Firebase
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    
    // HU-007: Variable para almacenar el rol del usuario
    private var currentUserRole: UserRole = UserRole.OWNER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Título TopBar
        binding.topBar.tvTitle.text = "General"
        // Evitar superposición con la barra de estado
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar.root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupDemoUserIfNeeded()
        setupRecyclerView()
        loadUserInfo()
        setupViews()
        setupObservers()
        
        // HU-007: Configurar navegación según el rol del usuario
        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            currentUserRole = user?.role ?: UserRole.OWNER
            setupBottomNavigation()
            applyRoleBasedVisibility()
        }

        transactionViewModel.refreshTransactions()
    }

    private fun setupBottomNavigation() {
        // CORRECCIÓN CLAVE: Usar el ID de la BottomNavigationView anidada.
        binding.bottomNavigation.bottomNavigation.selectedItemId = com.dam.financetracker.R.id.nav_home

        binding.bottomNavigation.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.dam.financetracker.R.id.nav_home -> true
                com.dam.financetracker.R.id.nav_transactions -> {
                    // HU-007: Solo si puede crear transacciones
                    if (RoleManager.canCreateTransactions(currentUserRole)) {
                        openTransactionActivity(com.dam.financetracker.models.TransactionType.INCOME)
                    } else {
                        // HU-008: Contador solo puede ver
                        startActivity(Intent(this, TransactionHistoryActivity::class.java))
                    }
                    true
                }
                com.dam.financetracker.R.id.nav_reports -> {
                    // HU-007: Verificar si puede acceder a reportes
                    if (RoleManager.canAccessReports(currentUserRole)) {
                        startActivity(Intent(this, com.dam.financetracker.ui.reports.ReportsActivity::class.java))
                    }
                    true
                }
                com.dam.financetracker.R.id.nav_settings -> {
                    // HU-007: Verificar si puede acceder a ajustes
                    if (RoleManager.canAccessSettings(currentUserRole)) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                    true
                }
                else -> true
            }
        }
    }
    
    /**
     * HU-007: Aplica la visibilidad de elementos según el rol del usuario
     * EMPLOYEE (Empleado): Solo ve Inicio y Transacciones
     * ACCOUNTANT (Contador): Solo ve Inicio, Transacciones (lectura) y Reportes
     * OWNER (Propietario): Ve todo
     */
    private fun applyRoleBasedVisibility() {
        val menu = binding.bottomNavigation.bottomNavigation.menu
        
        // HU-008: Ocultar Inicio para Contadores (solo 3 pantallas)
        val homeItem = menu.findItem(com.dam.financetracker.R.id.nav_home)
        homeItem?.isVisible = RoleManager.canAccessDashboard(currentUserRole)
        
        // HU-007: Ocultar Reportes para Empleados
        val reportsItem = menu.findItem(com.dam.financetracker.R.id.nav_reports)
        reportsItem?.isVisible = RoleManager.canAccessReports(currentUserRole)
        
        // HU-007: Ocultar Ajustes para Empleados
        // HU-008: Contador SÍ puede ver Ajustes
        val settingsItem = menu.findItem(com.dam.financetracker.R.id.nav_settings)
        settingsItem?.isVisible = RoleManager.canAccessSettings(currentUserRole)
        
        // Actualizar título según el rol
        val roleName = RoleManager.getRoleName(currentUserRole)
        binding.topBar.tvTitle.text = "General - $roleName"
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter { transaction ->
            openTransactionForEdit(transaction)
        }

        binding.rvTransactions.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
        }
    }

    private fun loadUserInfo(){
        lifecycleScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    binding.tvWelcome.text = "Bienvenido"
                    binding.tvEmail.text = user.email
                    saveUserEmail(user.email)
                } else {
                    binding.tvWelcome.text = "Bienvenido"
                    val savedEmail = getSavedUserEmail()
                    binding.tvEmail.text = savedEmail
                }
            } catch (e: Exception){
                binding.tvWelcome.text = "Bienvenido"
                val savedEmail = getSavedUserEmail()
                binding.tvEmail.text = savedEmail
            }
        }
    }

    private fun saveUserEmail(email: String) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putString("user_email", email)
            .apply()
    }

    private fun getSavedUserEmail(): String {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("user_email", "demo@financetracker.com") ?: "demo@financetracker.com"
    }

    private fun setupDemoUserIfNeeded() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!prefs.contains("user_email")) {
            saveUserEmail("demo@financetracker.com")
        }
    }

    private fun setupViews() {
        // Botón 'Salir' del Header (Temporal)
        binding.btnLogout.setOnClickListener {
            authRepository.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnAddTransaction.setOnClickListener {
            openTransactionActivity(TransactionType.INCOME)
        }

        binding.btnAddFirstTransaction.setOnClickListener {
            openTransactionActivity(TransactionType.INCOME)
        }

        binding.tvViewAll.setOnClickListener {
            // HU-005: Abrir actividad de historial con filtros
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            transactionViewModel.transactions.collect { transactions ->
                transactionAdapter.submitList(transactions.take(5))

                if (transactions.isEmpty()) {
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    binding.rvTransactions.visibility = View.GONE
                } else {
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvTransactions.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            transactionViewModel.balance.collect { balance ->
                binding.tvBalance.text = currencyFormat.format(balance)
            }
        }
    }

    private fun openTransactionActivity(type: TransactionType) {
        val intent = Intent(this, TransactionActivity::class.java)
        intent.putExtra(TransactionActivity.EXTRA_TRANSACTION_TYPE, type.name)
        startActivity(intent)
    }

    private fun openTransactionForEdit(transaction: Transaction) {
        val intent = Intent(this, TransactionActivity::class.java)
        intent.putExtra(TransactionActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Asegurar que Home quede seleccionado al volver
        binding.bottomNavigation.bottomNavigation.selectedItemId = com.dam.financetracker.R.id.nav_home
        transactionViewModel.refreshTransactions()
    }
}
