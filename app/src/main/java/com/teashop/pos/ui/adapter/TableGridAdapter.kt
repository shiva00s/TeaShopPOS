package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.databinding.ItemTableButtonBinding

class TableGridAdapter(
    private val tables: List<String>,
    private val onTableClick: (String) -> Unit
) : RecyclerView.Adapter<TableGridAdapter.TableViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val binding = ItemTableButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val tableId = tables[position]
        holder.bind(tableId)
    }

    override fun getItemCount() = tables.size

    inner class TableViewHolder(private val binding: ItemTableButtonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tableId: String) {
            binding.tvTableName.text = tableId
            binding.root.setOnClickListener { onTableClick(tableId) }
        }
    }
}
