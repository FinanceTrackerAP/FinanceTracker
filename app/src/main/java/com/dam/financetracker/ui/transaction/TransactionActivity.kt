package com.dam.financetracker.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityTransactionBinding
import com.dam.financetracker.models.DefaultExpenseCategories
import com.dam.financetracker.models.DefaultIncomeCategories
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTransactionBinding
    // Usar TransactionViewModel para conectar con Firebase
    private val viewModel: TransactionViewModel by viewModels()
    // private val viewModel: LocalTransactionViewModel by viewModels()
    
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
        
        setupViews()
        setupObservers()
        handleIntent()
    }
    
    private fun setupViews() {
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Configurar selector de tipo de transacción
        setupTypeSpinner()
        
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
        
        // Configurar botones
        binding.btnSave.setOnClickListener {
            viewModel.saveTransaction()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
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
                when (type) {
                    TransactionType.INCOME -> {
                        binding.spinnerType.setText("Ingreso", false)
                    }
                    TransactionType.EXPENSE -> {
                        binding.spinnerType.setText("Gasto", false)
                    }
                }
                setupCategorySpinner(type)
                updateTitle() // Actualizar título cuando cambie el tipo
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
                binding.btnCancel.isEnabled = !isLoading
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
    
    private fun setupTypeSpinner() {
        val types = arrayOf("Ingreso", "Gasto")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        binding.spinnerType.setAdapter(adapter)
        
        // Configurar listener para selección
        binding.spinnerType.setOnItemClickListener { _, _, position, _ ->
            val selectedType = when (position) {
                0 -> TransactionType.INCOME
                1 -> TransactionType.EXPENSE
                else -> TransactionType.INCOME
            }
            viewModel.setTransactionType(selectedType)
        }
        
        // Establecer valor por defecto
        binding.spinnerType.setText("Ingreso", false)
        viewModel.setTransactionType(TransactionType.INCOME)
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
}
