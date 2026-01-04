package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.databinding.ItemCartRowBinding
import com.teashop.pos.ui.viewmodel.CartItem

class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private var items = emptyList<CartItem>()

    fun submitList(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class CartViewHolder(private val binding: ItemCartRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(cartItem: CartItem) {
            binding.tvCartItemName.text = cartItem.item.name
            binding.tvCartQty.text = cartItem.quantity.toInt().toString()
            binding.tvCartSubtotal.text = String.format("₹ %.2f", cartItem.price * cartItem.quantity)
            
            binding.btnPlus.setOnClickListener { onIncrease(cartItem) }
            binding.btnMinus.setOnClickListener { onDecrease(cartItem) }
        }
    }
}
