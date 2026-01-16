package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Shop
import com.teashop.pos.databinding.ItemShopCardBinding
import com.teashop.pos.ui.viewmodel.ShopWithProfit

class ShopAdapter(
    private val onShopClick: (Shop) -> Unit,
    private val onLongClick: (Shop) -> Unit
) : ListAdapter<ShopWithProfit, ShopAdapter.ShopViewHolder>(ShopDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShopViewHolder(private val binding: ItemShopCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(shopWithProfit: ShopWithProfit) {
            val shop = shopWithProfit.shop
            binding.tvShopName.text = shop.name
            binding.tvLocation.text = shop.location
            binding.tvShopNetProfit.text = String.format("₹ %.2f", shopWithProfit.profit)
            binding.root.setOnClickListener { onShopClick(shop) }
            binding.root.setOnLongClickListener {
                onLongClick(shop)
                true
            }
        }
    }

    class ShopDiffCallback : DiffUtil.ItemCallback<ShopWithProfit>() {
        override fun areItemsTheSame(oldItem: ShopWithProfit, newItem: ShopWithProfit) = oldItem.shop.shopId == newItem.shop.shopId
        override fun areContentsTheSame(oldItem: ShopWithProfit, newItem: ShopWithProfit) = oldItem == newItem
    }
}
