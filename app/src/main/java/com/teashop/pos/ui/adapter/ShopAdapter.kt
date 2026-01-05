package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Shop
import com.teashop.pos.databinding.ItemShopCardBinding

class ShopAdapter(private val onShopClick: (Shop) -> Unit) :
    ListAdapter<Shop, ShopAdapter.ShopViewHolder>(ShopDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShopViewHolder(private val binding: ItemShopCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(shop: Shop) {
            binding.tvShopName.text = shop.name
            binding.tvLocation.text = shop.location
            binding.tvShopTodaySales.text = "OPEN POS" // Standard clear action
            binding.root.setOnClickListener { onShopClick(shop) }
        }
    }

    class ShopDiffCallback : DiffUtil.ItemCallback<Shop>() {
        override fun areItemsTheSame(oldItem: Shop, newItem: Shop) = oldItem.shopId == newItem.shopId
        override fun areContentsTheSame(oldItem: Shop, newItem: Shop) = oldItem == newItem
    }
}
