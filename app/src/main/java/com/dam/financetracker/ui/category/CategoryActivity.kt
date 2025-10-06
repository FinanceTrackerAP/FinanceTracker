package com.dam.financetracker.ui.category

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dam.financetracker.databinding.ActivityCategoryBinding


class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure you have the activity_category.xml layout created
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Listener for adding custom category (HU-003: 1)
        binding.btnAddCategory.setOnClickListener {
            // TODO: Launch dialog or activity to input new category details
        }

        // TODO: Implement RecyclerView to list existing categories
    }
}