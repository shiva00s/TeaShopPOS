package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.OrderWithItems
import com.teashop.pos.databinding.ItemHeldOrderBubbleBinding

class HeldOrderBubbleAdapter(
    private val onBubbleClick: (OrderWithItems) -> Unit
) : ListAdapter<OrderWithItems, HeldOrderBubbleAdapter.BubbleViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BubbleViewHolder {
        val binding = ItemHeldOrderBubbleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BubbleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BubbleViewHolder, position: Int) {
        val orderWithItems = getItem(position)
        holder.bind(orderWithItems)
    }

    inner class BubbleViewHolder(private val binding: ItemHeldOrderBubbleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(orderWithItems: OrderWithItems) {
            val order = orderWithItems.order
            val identifier = if (order.serviceType == "TABLE") {
                "Table ${order.tableId}"
            } else {
                order.serviceType
            }
            binding.tvBubbleIdentifier.text = identifier
            binding.tvBubbleTotal.text = String.format("₹%.2f", order.totalAmount)
            itemView.setOnClickListener { onBubbleClick(orderWithItems) }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderWithItems>() {
        override fun areItemsTheSame(oldItem: OrderWithItems, newItem: OrderWithItems):
            Boolean = oldItem.order.orderId == newItem.order.orderId

        override fun areContentsTheSame(oldItem: OrderWithItems, newItem: OrderWithItems):
            Boolean = oldItem == newItem
    }
}
