package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.FixedExpense
import com.teashop.pos.databinding.ItemFixedExpenseRowBinding

class FixedExpenseAdapter(
    private val onItemClick: (FixedExpense) -> Unit
) : ListAdapter<FixedExpense, FixedExpenseAdapter.ExpenseViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemFixedExpenseRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense)
    }

    inner class ExpenseViewHolder(private val binding: ItemFixedExpenseRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: FixedExpense) {
            binding.tvExpenseName.text = expense.name
            binding.tvExpenseAmount.text = "₹%.2f".format(expense.monthlyAmount)
            binding.root.setOnClickListener { onItemClick(expense) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FixedExpense>() {
        override fun areItemsTheSame(oldItem: FixedExpense, newItem: FixedExpense) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FixedExpense, newItem: FixedExpense) =
            oldItem == newItem
    }
}
