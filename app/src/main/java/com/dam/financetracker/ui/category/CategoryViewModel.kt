package com.dam.financetracker.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dam.financetracker.models.TransactionCategory
import com.dam.financetracker.models.TransactionType
import com.dam.financetracker.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class CategoryViewModel : ViewModel() {

    private val categoryRepository = CategoryRepository()

    // Estados para las listas de categorías
    private val _incomeCategories = MutableStateFlow<List<TransactionCategory>>(emptyList())
    val incomeCategories: StateFlow<List<TransactionCategory>> = _incomeCategories.asStateFlow()

    private val _expenseCategories = MutableStateFlow<List<TransactionCategory>>(emptyList())
    val expenseCategories: StateFlow<List<TransactionCategory>> = _expenseCategories.asStateFlow()

    // Estado para el formulario de Creación
    private val _newCategoryName = MutableStateFlow("")
    val newCategoryName: StateFlow<String> = _newCategoryName.asStateFlow()

    private val _newCategoryType = MutableStateFlow(TransactionType.INCOME)
    val newCategoryType: StateFlow<TransactionType> = _newCategoryType.asStateFlow()

    private val _creationLoading = MutableStateFlow(false)
    val creationLoading: StateFlow<Boolean> = _creationLoading.asStateFlow()

    // NUEVO: Canal para notificar el resultado de la operación a la Activity
    private val _creationResult = MutableSharedFlow<Result<String>>()
    val creationResult: SharedFlow<Result<String>> = _creationResult.asSharedFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            val allIncome = categoryRepository.getAllCategories(TransactionType.INCOME)
            val allExpense = categoryRepository.getAllCategories(TransactionType.EXPENSE)

            _incomeCategories.value = allIncome
            _expenseCategories.value = allExpense
        }
    }

    // Funciones para el formulario de creación
    fun setNewCategoryName(name: String) {
        _newCategoryName.value = name
    }

    fun setNewCategoryType(type: TransactionType) {
        _newCategoryType.value = type
    }

    fun createNewCategory() { // CAMBIO: Ahora es 'fun' sin devolver Result<Unit>
        val name = _newCategoryName.value.trim()
        val type = _newCategoryType.value

        if (name.isEmpty()) {
            // Notificar error de validación
            viewModelScope.launch {
                _creationResult.emit(Result.failure(Exception("El nombre de la categoría es obligatorio.")))
            }
            return
        }

        viewModelScope.launch {
            _creationLoading.value = true

            val newCategory = TransactionCategory(
                name = name,
                type = type,
            )

            val result = categoryRepository.createCategory(newCategory)

            _creationLoading.value = false

            if (result.isSuccess) {
                // Notificar éxito
                _creationResult.emit(Result.success("Categoría creada exitosamente."))
                loadCategories()
                clearCreationForm()
            } else {
                // Notificar error de persistencia
                val errorMessage = result.exceptionOrNull()?.message ?: "Error desconocido al guardar."
                _creationResult.emit(Result.failure(Exception(errorMessage)))
            }
        }
    }

    private fun clearCreationForm() {
        _newCategoryName.value = ""
        _newCategoryType.value = TransactionType.INCOME
    }
}