package com.dam.financetracker.ui.category

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dam.financetracker.databinding.ActivityCategoryBinding
import com.dam.financetracker.models.TransactionCategory
import com.dam.financetracker.R
import com.dam.financetracker.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch

/**
 * Activity para la gestión y listado de categorías (Figma: Manage Categories).
 * Muestra categorías de Ingreso y Gasto por separado (HU-003.2).
 */
class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    // Inicializar el ViewModel para consumir las listas y la lógica
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var incomeAdapter: CategoryAdapter
    private lateinit var expenseAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupViews()
        setupObservers()
        setupBottomNavigation()
    }

    private fun setupAdapters() {
        incomeAdapter = CategoryAdapter { category -> onCategoryClick(category) }
        expenseAdapter = CategoryAdapter { category -> onCategoryClick(category) }

        binding.rvIncomeCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoryActivity)
            adapter = incomeAdapter
            isNestedScrollingEnabled = false
        }
        binding.rvExpenseCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoryActivity)
            adapter = expenseAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        // Botón principal de la HU-003.1 (Crear nueva categoría)
        // La clase NewCategoryActivity debería resolverse automáticamente aquí.
        binding.btnCreateNewCategory.setOnClickListener {
            startActivity(Intent(this, NewCategoryActivity::class.java))
        }

        binding.tvViewAllIncome.setOnClickListener {
            Toast.makeText(this, "Ver todas las categorías de Ingresos", Toast.LENGTH_SHORT).show()
        }

        binding.tvViewAllExpense.setOnClickListener {
            Toast.makeText(this, "Ver todas las categorías de Gastos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Consumir los StateFlows del ViewModel
        lifecycleScope.launch {
            viewModel.incomeCategories.collect { categories ->
                incomeAdapter.submitList(categories)
            }
        }

        lifecycleScope.launch {
            viewModel.expenseCategories.collect { categories ->
                expenseAdapter.submitList(categories)
            }
        }
    }

    private fun onCategoryClick(category: TransactionCategory) {
        Toast.makeText(this, "Click en ${category.name}. (Pendiente: Editar Categoría)", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        viewModel.loadCategories()
    }
}