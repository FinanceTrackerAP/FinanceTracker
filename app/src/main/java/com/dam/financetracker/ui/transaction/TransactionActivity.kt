package com.dam.financetracker.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged // <- NUEVO
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
        // Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Selector de tipo
        setupTypeSpinner()

        // Monto
        binding.etAmount.addTextChangedListener {
            viewModel.setAmount(it?.toString() ?: "")
        }

        // Descripción + disparo de sugerencia
        binding.etDescription.doAfterTextChanged { text ->
            val value = text?.toString() ?: ""
            viewModel.setDescription(value)
            if (value.length > 5) {
                viewModel.requestCategorySuggestion()
            } else {
                // Si es corto, ocultamos la banda de sugerencia en UI
                binding.layoutSuggestion.visibility = View.GONE
            }
        }

        // Fecha
        binding.etDate.setOnClickListener { showDatePicker() }

        // Botones
        binding.btnSave.setOnClickListener { viewModel.saveTransaction() }
        binding.btnCancel.setOnClickListener { finish() }

        // Categorías por defecto
        setupCategorySpinner(TransactionType.INCOME)

        // Fecha inicial
        updateDateDisplay(System.currentTimeMillis())
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Tipo
            viewModel.transactionType.collect { type ->
                when (type) {
                    TransactionType.INCOME -> binding.spinnerType.setText("Ingreso", false)
                    TransactionType.EXPENSE -> binding.spinnerType.setText("Gasto", false)
                }
                setupCategorySpinner(type)
                updateTitle()
            }
        }

        lifecycleScope.launch {
            // Monto
            viewModel.amount.collect { amount ->
                if (binding.etAmount.text.toString() != amount) {
                    binding.etAmount.setText(amount)
                }
            }
        }

        lifecycleScope.launch {
            // Descripción
            viewModel.description.collect { description ->
                if (binding.etDescription.text.toString() != description) {
                    binding.etDescription.setText(description)
                }
            }
        }

        lifecycleScope.launch {
            // Fecha
            viewModel.selectedDate.collect { date ->
                updateDateDisplay(date)
            }
        }

        lifecycleScope.launch {
            // Modo edición
            viewModel.isEditMode.collect { isEditMode ->
                binding.btnSave.text = if (isEditMode) "Actualizar" else "Guardar"
                updateTitle()
            }
        }

        lifecycleScope.launch {
            // Errores
            viewModel.amountError.collect { binding.tilAmount.error = it }
        }

        lifecycleScope.launch {
            viewModel.descriptionError.collect { binding.tilDescription.error = it }
        }

        lifecycleScope.launch {
            viewModel.categoryError.collect { binding.tilCategory.error = it }
        }

        lifecycleScope.launch {
            // Loading
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
                binding.btnCancel.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            // Resultado operación
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

        lifecycleScope.launch {
            // NUEVO: observar sugerencia de categoría (HU-003: Escenario 3)
            viewModel.suggestedCategory.collect { suggestion ->
                when {
                    suggestion.isNullOrEmpty() -> {
                        binding.layoutSuggestion.visibility = View.GONE
                    }
                    suggestion == "Cargando sugerencia..." -> {
                        binding.layoutSuggestion.visibility = View.VISIBLE
                        binding.btnSuggestCategory.text = suggestion
                        binding.btnSuggestCategory.setOnClickListener(null) // sin acción mientras carga
                    }
                    else -> {
                        binding.layoutSuggestion.visibility = View.VISIBLE
                        binding.btnSuggestCategory.text = suggestion
                        binding.btnSuggestCategory.setOnClickListener {
                            viewModel.setCategory(suggestion)
                            binding.spinnerCategory.setText(suggestion, false)
                            binding.layoutSuggestion.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent() {
        // Editar transacción
        val transactionExtra = intent.getSerializableExtra(EXTRA_TRANSACTION) as? Transaction
        transactionExtra?.let { transaction ->
            viewModel.loadTransactionForEdit(transaction)
            setupCategorySpinner(transaction.type)
            updateTitle()
            return
        }

        // Tipo específico
        val transactionTypeExtra = intent.getStringExtra(EXTRA_TRANSACTION_TYPE)
        transactionTypeExtra?.let { typeString ->
            val type = try {
                TransactionType.valueOf(typeString)
            } catch (_: IllegalArgumentException) {
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

        // Selección
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            viewModel.setCategory(selectedCategory)
        }

        // Mantener selección si edita
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

        binding.spinnerType.setOnItemClickListener { _, _, position, _ ->
            val selectedType = when (position) {
                0 -> TransactionType.INCOME
                1 -> TransactionType.EXPENSE
                else -> TransactionType.INCOME
            }
            viewModel.setTransactionType(selectedType)
        }

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
