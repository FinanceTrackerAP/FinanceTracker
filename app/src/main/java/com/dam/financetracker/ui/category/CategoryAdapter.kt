package com.dam.financetracker.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dam.financetracker.databinding.ItemCategoryListBinding
import com.dam.financetracker.models.TransactionCategory

class CategoryAdapter(
    private val onCategoryClick: (TransactionCategory) -> Unit
) : ListAdapter<TransactionCategory, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: TransactionCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                // Usa la inicial de la categoría como icono
                tvCategoryIcon.text = category.name.substring(0, 1).uppercase()

                root.setOnClickListener {
                    onCategoryClick(category)
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<TransactionCategory>() {
        override fun areItemsTheSame(oldItem: TransactionCategory, newItem: TransactionCategory): Boolean {
            // Asumiendo que el ID es la clave primaria
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionCategory, newItem: TransactionCategory): Boolean {
            // Compara si todos los campos son iguales
            return oldItem == newItem
        }
    }
}