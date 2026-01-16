package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.OrderWithItems
import com.teashop.pos.databinding.DialogHeldOrdersBinding
import com.teashop.pos.databinding.ItemHeldOrderRowBinding
import com.teashop.pos.ui.viewmodel.POSViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HeldOrdersDialogFragment : DialogFragment() {

    private val viewModel: POSViewModel by activityViewModels()
    private lateinit var adapter: HeldOrdersAdapter

    companion object {
        const val TAG = "HeldOrdersDialog"
        fun newInstance(): HeldOrdersDialogFragment {
            return HeldOrdersDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogHeldOrdersBinding.inflate(LayoutInflater.from(context))
        binding.rvHeldOrders.layoutManager = LinearLayoutManager(context)
        adapter = HeldOrdersAdapter(
            onOrderClick = {
                viewModel.loadHeldOrder(it.order)
                dismiss()
            },
            onDeleteClick = {
                viewModel.deleteHeldOrder(it.order)
            }
        )
        binding.rvHeldOrders.adapter = adapter

        binding.btnClearAllHeld.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear All Held Orders?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Clear All") { _, _ -> viewModel.clearAllHeldOrders() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewModel.heldOrdersWithItems.observe(this) { ordersWithItems ->
            adapter.submitList(ordersWithItems)
            val itemCounts = mutableMapOf<String, Int>()
            ordersWithItems.forEach { order ->
                order.items.forEach { item ->
                    val nameWithoutEmoji = item.itemName.replace(Regex("[^\\p{L}\\p{N}\\s]"), "").trim()
                    itemCounts[nameWithoutEmoji] = (itemCounts[nameWithoutEmoji] ?: 0) + item.quantity.toInt()
                }
            }

            val summary = itemCounts.entries
                .sortedByDescending { it.value }
                .joinToString(" | ") { "${it.key} x${it.value}" }

            binding.tvItemsSummary.text = summary
            binding.tvItemsSummary.visibility = if (summary.isNotEmpty()) View.VISIBLE else View.GONE
        }

        return AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .create()
    }
}

class HeldOrdersAdapter(
    private val onOrderClick: (OrderWithItems) -> Unit,
    private val onDeleteClick: (OrderWithItems) -> Unit
) : ListAdapter<OrderWithItems, HeldOrdersAdapter.ViewHolder>(OrderDiffCallback()) {

    class ViewHolder(val binding: ItemHeldOrderRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHeldOrderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val orderWithItems = getItem(position)
        val order = orderWithItems.order

        val info = if (order.serviceType == "TABLE") "Table: ${order.tableId}" else order.serviceType
        holder.binding.tvOrderId.text = "$info (#${order.orderId.takeLast(4)})"
        holder.binding.tvOrderTotal.text = String.format("₹ %.2f", order.totalAmount)

        val itemsSummary = orderWithItems.items.joinToString("\n") { "- ${it.itemName} x ${it.quantity.toInt()}" }
        holder.binding.tvOrderDetails.text = itemsSummary
        holder.binding.tvOrderDetails.visibility = if (itemsSummary.isNotEmpty()) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onOrderClick(orderWithItems) }
        holder.binding.btnClearOrder.setOnClickListener { onDeleteClick(orderWithItems) }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderWithItems>() {
        override fun areItemsTheSame(oldItem: OrderWithItems, newItem: OrderWithItems):
            Boolean = oldItem.order.orderId == newItem.order.orderId

        override fun areContentsTheSame(oldItem: OrderWithItems, newItem: OrderWithItems):
            Boolean = oldItem == newItem
    }
}
