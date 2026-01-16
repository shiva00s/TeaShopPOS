package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.ShopMenuItem
import com.teashop.pos.databinding.ItemMenuGridBinding

class MenuAdapter(private val onItemClick: (ShopMenuItem) -> Unit) :
    RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private var items = emptyList<ShopMenuItem>()

    fun submitList(newItems: List<ShopMenuItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    inner class MenuViewHolder(private val binding: ItemMenuGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItem: ShopMenuItem) {
            binding.tvItemName.text = menuItem.item.name
            binding.tvItemPrice.text = String.format("₹ %.2f", menuItem.finalPrice)
            binding.root.setOnClickListener { onItemClick(menuItem) }
        }
    }
}
