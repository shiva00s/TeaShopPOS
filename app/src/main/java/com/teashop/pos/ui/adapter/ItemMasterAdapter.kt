package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Item
import com.teashop.pos.databinding.ItemMasterRowBinding

class ItemMasterAdapter(
    private val onSetPriceClick: (Item) -> Unit,
    private val onEditClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) :
    ListAdapter<Item, ItemMasterAdapter.ItemViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemMasterRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(private val binding: ItemMasterRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Item) {
            binding.tvItemName.text = item.name
            binding.tvCategory.text = "Category: ${item.category}"
            binding.btnSetPrice.setOnClickListener { onSetPriceClick(item) }
            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.itemId == newItem.itemId
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
