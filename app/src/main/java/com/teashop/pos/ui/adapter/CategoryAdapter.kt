package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.R
import com.teashop.pos.databinding.ItemCategoryGridBinding

class CategoryAdapter(private val onCategoryClick: (String?) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var categories = mutableListOf<String?>()
    private var selectedCategories = mutableSetOf<String>()

    fun submitList(newCategories: List<String?>, selected: Set<String>) {
        categories = newCategories.toMutableList()
        selectedCategories = selected.toMutableSet()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
    }

    override fun getItemCount() = categories.size

    inner class CategoryViewHolder(private val binding: ItemCategoryGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: String?) {
            binding.tvCategoryName.text = category ?: "All"
            
            val isSelected = if (category == null) {
                selectedCategories.isEmpty()
            } else {
                selectedCategories.contains(category)
            }

            if (isSelected) {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                binding.root.strokeWidth = 0
                binding.tvCategoryName.typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.card_bg))
                binding.tvCategoryName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
                binding.root.strokeWidth = 1
                binding.tvCategoryName.typeface = android.graphics.Typeface.DEFAULT
            }

            binding.root.setOnClickListener { onCategoryClick(category) }
        }
    }
}
