package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.databinding.ItemCartRowBinding
import com.teashop.pos.ui.viewmodel.CartItem
import java.util.Locale

class CartAdapter(
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onParcelClick: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ItemCartRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(cartItem: CartItem) {
            val itemName = cartItem.item.name.ifBlank { "Item" }

            binding.tvCartItemName.text = itemName
            binding.tvCartQty.text = String.format(Locale.getDefault(), "%d", cartItem.quantity.toInt())
            binding.tvCartSubtotal.text = String.format(Locale.getDefault(), "₹ %.2f", (cartItem.price * cartItem.quantity) + cartItem.parcelCharge)

            if (cartItem.parcelCharge > 0) {
                binding.tvParcelLink.text = "+ ₹%.2f (Parcel 📦)".format(cartItem.parcelCharge)
            } else {
                binding.tvParcelLink.text = "Add Parcel 📦"
            }

            binding.btnPlus.setOnClickListener { onIncrease(cartItem) }
            binding.btnMinus.setOnClickListener { onDecrease(cartItem) }
            binding.tvParcelLink.setOnClickListener { onParcelClick(cartItem) }
        }
    }
}

class CartItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem):
            Boolean = oldItem.item.itemId == newItem.item.itemId

    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem):
            Boolean = oldItem == newItem
}
