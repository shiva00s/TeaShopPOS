package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Order
import com.teashop.pos.databinding.DialogHeldOrdersBinding
import com.teashop.pos.databinding.ItemHeldOrderRowBinding
import com.teashop.pos.ui.viewmodel.POSViewModel

class HeldOrdersDialogFragment : DialogFragment() {

    private val viewModel: POSViewModel by activityViewModels {
        (requireActivity().application as TeaShopApplication).viewModelFactory
    }

    companion object {
        const val TAG = "HeldOrdersDialog"
        fun newInstance(): HeldOrdersDialogFragment {
            return HeldOrdersDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogHeldOrdersBinding.inflate(LayoutInflater.from(context))
        binding.rvHeldOrders.layoutManager = LinearLayoutManager(context)

        viewModel.heldOrders.observe(this) { orders ->
            binding.rvHeldOrders.adapter = HeldOrdersAdapter(orders) {
                viewModel.loadHeldOrder(it)
                dismiss()
            }
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle("Held Orders")
            .setView(binding.root)
            .setNegativeButton("Cancel", null)
            .create()
    }
}

class HeldOrdersAdapter(private val orders: List<Order>, private val onOrderClick: (Order) -> Unit) :
    RecyclerView.Adapter<HeldOrdersAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHeldOrderRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHeldOrderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        // Display Table number or Standing/Parcel service type
        val info = if (order.serviceType == "TABLE") "Table: ${order.tableId}" else order.serviceType
        holder.binding.tvOrderId.text = "$info (#${order.orderId.substring(0, 4)})"
        holder.binding.tvOrderTotal.text = String.format("₹ %.2f", order.totalAmount)
        holder.itemView.setOnClickListener { onOrderClick(order) }
    }

    override fun getItemCount() = orders.size
}
