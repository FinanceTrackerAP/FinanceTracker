package com.dam.financetracker.ui.transaction

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityTransactionBinding
import com.dam.financetracker.models.DefaultExpenseCategories
import com.dam.financetracker.models.DefaultIncomeCategories
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.models.UserRole
import com.dam.financetracker.repository.AuthRepository
import com.dam.financetracker.utils.RoleManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    // Usar TransactionViewModel para conectar con Firebase
    private val viewModel: TransactionViewModel by viewModels()
    // private val viewModel: LocalTransactionViewModel by viewModels()
    
    private val authRepository = AuthRepository()
    private var currentUserRole: UserRole = UserRole.OWNER

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_TRANSACTION = "transaction"
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // HU-007: Obtener rol del usuario antes de configurar vistas
        lifecycleScope.launch {
            val user = authRepository.getCurrentUser()
            currentUserRole = user?.role ?: UserRole.OWNER
            
            setupViews()
            setupObservers()
            handleIntent()
            setupBottomNavigation()
            applyRoleBasedRestrictions()
        }
        
        // Evitar superposición con la barra de estado
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar.root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun setupViews() {
        // Configurar top bar título
        binding.topBar.tvTitle.text = "Transacciones"

        // Configurar selector de tipo con botones
        setupTypeToggle()

        // Configurar listeners de texto
        binding.etAmount.addTextChangedListener {
            viewModel.setAmount(it?.toString() ?: "")
        }

        binding.etDescription.addTextChangedListener {
            viewModel.setDescription(it?.toString() ?: "")
        }

        // Configurar selector de fecha
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Configurar botón guardar
        binding.btnSave.setOnClickListener {
            viewModel.saveTransaction()
        }

        // Configurar spinner de categorías por defecto
        setupCategorySpinner(TransactionType.INCOME)

        // Configurar fecha inicial
        updateDateDisplay(System.currentTimeMillis())
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observar tipo de transacción
            viewModel.transactionType.collect { type ->
                // Actualiza estilos de botones
                updateToggleStyles(type)
                setupCategorySpinner(type)
                updateTitle() // Actualizar título cuando cambie el tipo
                // Actualizar texto del botón principal
                binding.btnSave.text = if (type == TransactionType.INCOME) "Guardar Ingreso" else "Guardar Gasto"
            }
        }

        lifecycleScope.launch {
            // Observar monto
            viewModel.amount.collect { amount ->
                if (binding.etAmount.text.toString() != amount) {
                    binding.etAmount.setText(amount)
                }
            }
        }

        lifecycleScope.launch {
            // Observar descripción
            viewModel.description.collect { description ->
                if (binding.etDescription.text.toString() != description) {
                    binding.etDescription.setText(description)
                }
            }
        }

        lifecycleScope.launch {
            // Observar fecha seleccionada
            viewModel.selectedDate.collect { date ->
                updateDateDisplay(date)
            }
        }

        lifecycleScope.launch {
            // Observar modo de edición
            viewModel.isEditMode.collect { isEditMode ->
                binding.btnSave.text = if (isEditMode) "Actualizar" else "Guardar"
                updateTitle() // Actualizar título cuando cambie el modo de edición
            }
        }

        lifecycleScope.launch {
            // Observar errores de validación
            viewModel.amountError.collect { error ->
                binding.tilAmount.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.descriptionError.collect { error ->
                binding.tilDescription.error = error
            }
        }

        lifecycleScope.launch {
            viewModel.categoryError.collect { error ->
                binding.tilCategory.error = error
            }
        }

        lifecycleScope.launch {
            // Observar estado de carga
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            // Observar resultados de operaciones
            viewModel.operationResult.collect { result ->
                if (result.isSuccess) {
                    Toast.makeText(this@TransactionActivity, result.getOrNull(), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@TransactionActivity,
                        result.exceptionOrNull()?.message ?: "Error desconocido",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleIntent() {
        // Verificar si se está editando una transacción
        val transactionExtra = intent.getSerializableExtra(EXTRA_TRANSACTION) as? Transaction
        transactionExtra?.let { transaction ->
            viewModel.loadTransactionForEdit(transaction)
            setupCategorySpinner(transaction.type)
            updateTitle() // Actualizar título inmediatamente al cargar para editar
            return
        }

        // Verificar si se pasó un tipo de transacción específico
        val transactionTypeExtra = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
        transactionTypeExtra?.let { typeString ->
            val type = try {
                TransactionType.valueOf(typeString)
            } catch (e: IllegalArgumentException) {
                TransactionType.INCOME
            }
            viewModel.setTransactionType(type)
        }
    }

    private fun setupCategorySpinner(type: TransactionType) {
        val categories = when (type) {
            TransactionType.INCOME -> DefaultIncomeCategories.categories.map { it.name }
            TransactionType.EXPENSE -> DefaultExpenseCategories.categories.map { it.name }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        // Listener para cuando se selecciona una categoría
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            viewModel.setCategory(selectedCategory)
        }

        // Seleccionar categoría actual si está en modo edición
        lifecycleScope.launch {
            viewModel.category.collect { category ->
                if (categories.contains(category) && binding.spinnerCategory.text.toString() != category) {
                    binding.spinnerCategory.setText(category, false)
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.selectedDate.value

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                viewModel.setSelectedDate(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay(timestamp: Long) {
        binding.etDate.setText(dateFormat.format(Date(timestamp)))
    }

    private fun setupTypeToggle() {
        binding.btnIncome.setOnClickListener {
            viewModel.setTransactionType(TransactionType.INCOME)
        }
        binding.btnExpense.setOnClickListener {
            viewModel.setTransactionType(TransactionType.EXPENSE)
        }
        updateToggleStyles(TransactionType.INCOME)
        viewModel.setTransactionType(TransactionType.INCOME)
    }

    private fun updateToggleStyles(type: TransactionType) {
        val incomeSelected = type == TransactionType.INCOME
        if (incomeSelected) {
            binding.btnIncome.backgroundTintList = getColorStateList(R.color.success_green)
            binding.btnIncome.setTextColor(getColor(R.color.white))
            binding.btnExpense.backgroundTintList = getColorStateList(android.R.color.transparent)
            binding.btnExpense.setTextColor(getColor(R.color.primary_text))
        } else {
            binding.btnIncome.backgroundTintList = getColorStateList(android.R.color.transparent)
            binding.btnIncome.setTextColor(getColor(R.color.primary_text))
            binding.btnExpense.backgroundTintList = getColorStateList(R.color.error_red)
            binding.btnExpense.setTextColor(getColor(R.color.white))
        }
    }

    private fun updateTitle() {
        lifecycleScope.launch {
            val isEditMode = viewModel.isEditMode.value
            val transactionType = viewModel.transactionType.value

            binding.tvTitle.text = when {
                isEditMode && transactionType == TransactionType.INCOME -> "Editar Ingreso"
                isEditMode && transactionType == TransactionType.EXPENSE -> "Editar Gasto"
                !isEditMode && transactionType == TransactionType.INCOME -> "Registrar Ingreso"
                !isEditMode && transactionType == TransactionType.EXPENSE -> "Registrar Gasto"
                else -> "Nueva Transacción"
            }
        }
    }

    private fun setupBottomNavigation() {
        val menu = binding.bottomNavigation.root.menu
        
        // HU-007 y HU-008: Ocultar opciones según el rol
        menu.findItem(com.dam.financetracker.R.id.nav_home)?.isVisible = RoleManager.canAccessDashboard(currentUserRole)
        menu.findItem(com.dam.financetracker.R.id.nav_reports)?.isVisible = RoleManager.canAccessReports(currentUserRole)
        menu.findItem(com.dam.financetracker.R.id.nav_settings)?.isVisible = RoleManager.canAccessSettings(currentUserRole)
        
        // Mantener seleccionado el tab de Transacciones
        binding.bottomNavigation.root.selectedItemId = com.dam.financetracker.R.id.nav_transactions
        binding.bottomNavigation.root.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.dam.financetracker.R.id.nav_home -> {
                    finish() // volver a dashboard
                    true
                }
                com.dam.financetracker.R.id.nav_transactions -> true
                com.dam.financetracker.R.id.nav_reports -> {
                    if (RoleManager.canAccessReports(currentUserRole)) {
                        startActivity(Intent(this, com.dam.financetracker.ui.reports.ReportsActivity::class.java))
                        finish()
                    }
                    true
                }
                com.dam.financetracker.R.id.nav_settings -> {
                    if (RoleManager.canAccessSettings(currentUserRole)) {
                        startActivity(Intent(this, com.dam.financetracker.ui.settings.SettingsActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> true
            }
        }
    }
    
    /**
     * HU-007: Aplica restricciones según el rol del usuario
     * EMPLOYEE (Empleado): SOLO puede registrar INGRESOS (ventas), NO puede registrar gastos
     * ACCOUNTANT (Contador): No puede crear transacciones (esta pantalla no debería abrirse)
     */
    private fun applyRoleBasedRestrictions() {
        when (currentUserRole) {
            UserRole.EMPLOYEE -> {
                // HU-007: Empleado SOLO puede registrar INGRESOS, NO gastos
                if (!RoleManager.canCreateExpenses(currentUserRole)) {
                    // Ocultar botón de Gastos
                    binding.btnExpense.visibility = View.GONE
                    
                    // Forzar tipo INCOME
                    viewModel.setTransactionType(TransactionType.INCOME)
                    binding.btnIncome.isEnabled = false // No puede cambiar
                    
                    // Mostrar mensaje informativo
                    Toast.makeText(
                        this,
                        "Como empleado, solo puedes registrar ventas e ingresos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            UserRole.ACCOUNTANT -> {
                // HU-008: Contador no puede crear transacciones
                if (!RoleManager.canCreateTransactions(currentUserRole)) {
                    // Deshabilitar todos los campos
                    binding.etAmount.isEnabled = false
                    binding.etDescription.isEnabled = false
                    binding.spinnerCategory.isEnabled = false
                    binding.etDate.isEnabled = false
                    binding.btnSave.isEnabled = false
                    binding.btnIncome.isEnabled = false
                    binding.btnExpense.isEnabled = false
                    
                    Toast.makeText(
                        this,
                        "Como contador, solo puedes consultar transacciones",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            UserRole.OWNER -> {
                // Propietario tiene acceso completo, no hay restricciones
            }
        }
    }
}
