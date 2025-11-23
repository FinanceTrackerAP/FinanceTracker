package com.dam.financetracker.ui.transaction

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityTransactionHistoryBinding
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.ui.dashboard.DashboardActivity
import com.dam.financetracker.ui.dashboard.TransactionAdapter
import com.dam.financetracker.ui.reports.ReportsActivity
import com.dam.financetracker.ui.settings.SettingsActivity
import com.dam.financetracker.utils.RoleManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * HU-005: Actividad para mostrar historial completo de transacciones con filtros
 */
class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    
    private val authRepository = AuthRepository()
    private var currentUserRole: UserRole = UserRole.OWNER
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
    
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupTopBar()
        setupRecyclerView()
        setupFilters()
        setupObservers()
        
        // HU-007 y HU-008: Configurar según rol
        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            currentUserRole = user?.role ?: UserRole.OWNER
            setupBottomNavigation()
        }
    }

    private fun setupTopBar() {
        binding.topBar.tvTitle.text = "Historial de Transacciones"
        
        // Evitar superposición con la barra de estado
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar.root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            // HU-007 y HU-008: Solo permitir editar si tiene permisos
            if (RoleManager.canEditTransactions(currentUserRole)) {
                openTransactionForEdit(transaction)
            } else {
                // HU-008: Contador solo puede ver
                android.widget.Toast.makeText(
                    this,
                    "No tienes permisos para editar transacciones",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        binding.rvFilteredTransactions.apply {
            adapter = this@TransactionHistoryActivity.adapter
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
        }
    }

    private fun setupFilters() {
        // HU-005.3: Búsqueda por descripción
        binding.etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
        
        // HU-005.2: Filtro por categoría
        setupCategoryFilter()
        
        // HU-005.1: Filtros de fecha
        binding.btnStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }
        
        binding.btnEndDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }
        
        // Limpiar filtros
        binding.btnClearFilters.setOnClickListener {
            clearAllFilters()
        }
    }

    private fun setupCategoryFilter() {
        lifecycleScope.launch {
            // Obtener categorías únicas
            viewModel.transactions.collect { transactions ->
                val categories = transactions
                    .map { it.category }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toMutableList()
                
                // Agregar opción "Todas"
                categories.add(0, "Todas las categorías")
                
                val adapter = ArrayAdapter(
                    this@TransactionHistoryActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    categories
                )
                
                binding.actvCategory.setAdapter(adapter)
                binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
                    val selectedCategory = categories[position]
                    if (selectedCategory == "Todas las categorías") {
                        viewModel.setCategoryFilter(null)
                    } else {
                        viewModel.setCategoryFilter(selectedCategory)
                    }
                }
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        // Si ya hay una fecha seleccionada, usarla como fecha inicial
        if (isStartDate && selectedStartDate != null) {
            calendar.timeInMillis = selectedStartDate!!
        } else if (!isStartDate && selectedEndDate != null) {
            calendar.timeInMillis = selectedEndDate!!
        }
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                
                val selectedDate = selectedCalendar.timeInMillis
                
                if (isStartDate) {
                    selectedStartDate = selectedDate
                    binding.btnStartDate.text = dateFormat.format(Date(selectedDate))
                } else {
                    selectedEndDate = selectedDate
                    binding.btnEndDate.text = dateFormat.format(Date(selectedDate))
                }
                
                viewModel.setDateFilter(selectedStartDate, selectedEndDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun clearAllFilters() {
        // Limpiar campos de UI
        binding.etSearch.setText("")
        binding.actvCategory.setText("", false)
        binding.btnStartDate.text = "Fecha inicio"
        binding.btnEndDate.text = "Fecha fin"
        
        // Limpiar variables locales
        selectedStartDate = null
        selectedEndDate = null
        
        // Limpiar filtros en ViewModel
        viewModel.clearAllFilters()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.filteredTransactions.collect { transactions ->
                updateUI(transactions)
            }
        }
    }

    private fun updateUI(transactions: List<Transaction>) {
        // Actualizar RecyclerView
        adapter.submitList(transactions)
        
        // Mostrar/ocultar empty state
        if (transactions.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvFilteredTransactions.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvFilteredTransactions.visibility = View.VISIBLE
        }
        
        // Actualizar contador de resultados
        val count = transactions.size
        binding.tvResultsCount.text = "Total: $count transaccion${if (count == 1) "" else "es"}"
        
        // Calcular y mostrar monto total
        var totalAmount = 0.0
        transactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> totalAmount += transaction.amount
                TransactionType.EXPENSE -> totalAmount -= transaction.amount
            }
        }
        binding.tvTotalAmount.text = currencyFormat.format(totalAmount)
    }

    private fun setupBottomNavigation() {
        val menu = binding.bottomNavigation.bottomNavigation.menu
        
        // HU-007 y HU-008: Ocultar opciones según el rol
        menu.findItem(R.id.nav_home)?.isVisible = RoleManager.canAccessDashboard(currentUserRole)
        menu.findItem(R.id.nav_reports)?.isVisible = RoleManager.canAccessReports(currentUserRole)
        menu.findItem(R.id.nav_settings)?.isVisible = RoleManager.canAccessSettings(currentUserRole)
        
        // Seleccionar el tab de transacciones
        binding.bottomNavigation.bottomNavigation.selectedItemId = R.id.nav_transactions
        
        binding.bottomNavigation.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    // Ya estamos en transacciones
                    true
                }
                R.id.nav_reports -> {
                    if (RoleManager.canAccessReports(currentUserRole)) {
                        startActivity(Intent(this, ReportsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_settings -> {
                    if (RoleManager.canAccessSettings(currentUserRole)) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun openTransactionForEdit(transaction: Transaction) {
        val intent = Intent(this, TransactionActivity::class.java)
        intent.putExtra(TransactionActivity.EXTRA_TRANSACTION, transaction)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTransactions()
    }
}

