package com.dam.financetracker.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dam.financetracker.R
import com.dam.financetracker.databinding.ItemTransactionBinding
import com.dam.financetracker.models.Transaction
import com.dam.financetracker.models.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.apply {
                // Configurar información básica
                tvDescription.text = transaction.description
                tvCategory.text = transaction.category
                tvDate.text = dateFormat.format(Date(transaction.date))

                // Configurar monto y colores según el tipo
                when (transaction.type) {
                    TransactionType.INCOME -> {
                        tvAmount.text = "+ ${currencyFormat.format(transaction.amount)}"
                        tvAmount.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.success_green)
                        )
                        viewTypeIndicator.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.success_green)
                        )
                    }
                    TransactionType.EXPENSE -> {
                        tvAmount.text = "- ${currencyFormat.format(transaction.amount)}"
                        tvAmount.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.error_red)
                        )
                        viewTypeIndicator.setBackgroundColor(
                            ContextCompat.getColor(itemView.context, R.color.error_red)
                        )
                    }
                }

                // Configurar click listener
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
