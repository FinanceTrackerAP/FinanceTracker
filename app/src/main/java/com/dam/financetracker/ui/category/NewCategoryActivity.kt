package com.dam.financetracker.ui.category

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ActivityNewCategoryBinding
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch


class NewCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewCategoryBinding
    // 1. Inicializa el ViewModel que contiene la lógica de persistencia
    private val viewModel: CategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
        setupBottomNavigation()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        setupTypeToggle()

        // 2. Enlazar campo de texto a la función del ViewModel
        binding.etCategoryName.doAfterTextChanged { text ->
            viewModel.setNewCategoryName(text?.toString() ?: "")
        }

        // 3. Botón de Guardar: dispara la lógica del ViewModel
        binding.btnSaveCategory.setOnClickListener {
            // Llamamos a la función que inicia la coroutine
            viewModel.createNewCategory()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // Observar el estado del nombre para actualizar el campo si es necesario
            viewModel.newCategoryName.collect { name ->
                if (binding.etCategoryName.text.toString() != name) {
                    binding.etCategoryName.setText(name)
                }
            }
        }

        // CORRECCIÓN CLAVE: Observar el estado de carga (creationLoading)
        lifecycleScope.launch {
            viewModel.creationLoading.collect { isLoading ->
                // Consumo de la propiedad: Esto elimina la advertencia de 'never used'
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSaveCategory.isEnabled = !isLoading
                binding.btnSaveCategory.text = if (isLoading) "Guardando..." else "Guardar"
            }
        }

        // Observar el resultado final de la operación asíncrona (éxito/fallo)
        lifecycleScope.launch {
            viewModel.creationResult.collect { result ->
                if (result.isSuccess) {
                    Toast.makeText(this@NewCategoryActivity, result.getOrNull(), Toast.LENGTH_SHORT).show()
                    finish() // Cerramos si el guardado en DB fue exitoso
                } else {
                    // Mostrar error de validación o error de DB/Red
                    binding.tilCategoryName.error = result.exceptionOrNull()?.message
                    Toast.makeText(this@NewCategoryActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }


        lifecycleScope.launch {
            // Observar el tipo seleccionado para actualizar los botones
            viewModel.newCategoryType.collect { type ->
                updateToggleStyles(type)
            }
        }
    }

    private fun setupTypeToggle() {
        binding.btnIncome.setOnClickListener {
            viewModel.setNewCategoryType(TransactionType.INCOME)
        }
        binding.btnExpense.setOnClickListener {
            viewModel.setNewCategoryType(TransactionType.EXPENSE)
        }
        viewModel.setNewCategoryType(TransactionType.INCOME)
    }

    private fun updateToggleStyles(type: TransactionType) {
        val incomeSelected = type == TransactionType.INCOME

        if (incomeSelected) {
            binding.btnIncome.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_green)
            binding.btnIncome.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnExpense.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
            binding.btnExpense.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
        } else {
            binding.btnIncome.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
            binding.btnIncome.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
            binding.btnExpense.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error_red)
            binding.btnExpense.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun setupBottomNavigation() {
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