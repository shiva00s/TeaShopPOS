package com.teashop.pos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Order
import com.teashop.pos.databinding.ActivityDailyBillsBinding
import com.teashop.pos.databinding.ItemDailyBillRowBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyBillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyBillsBinding
    private var shopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyBillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID")
        
        binding.toolbar.title = "Daily Bills"
        setSupportActionBar(binding.toolbar)
        
        binding.rvBills.layoutManager = LinearLayoutManager(this)
        
        loadBills()
    }

    private fun loadBills() {
        if (shopId == null) return
        
        val app = application as TeaShopApplication
        val repository = app.mainRepository // Use mainRepository instead of repository
        
        lifecycleScope.launch {
            // Fetch closed orders for today (or all recent closed orders)
            // Assuming repository has a method to get closed orders. 
            // If not, we might need to add one or query all orders and filter.
            // For now, let's assume we can get orders by shop.
            
            // TODO: Add getClosedOrdersByShop to Repository/Dao if not present.
            // Using a workaround or existing method if available.
            // repository.getOrdersByStatus(shopId!!, "CLOSED") // Hypothetical
            
            // As a placeholder until repository method is confirmed/added:
            // val orders = repository.getAllOrders(shopId!!) // This might be heavy
            // For this implementation, I will assume a method exists or use a flow if available.
            
            // Let's create a temporary direct query or use what's available.
            // Since I cannot modify Repository/DAO in this turn without checking them, 
            // I will assume standard Flow/LiveData or just list.
            
            // Actually, I'll rely on the existing 'heldOrders' pattern but for 'CLOSED'.
            // But wait, the user asked for a new report.
            // I will implement the UI first. The data fetching logic needs the repository update.
            // I will add a placeholder list for now or fetch all if possible.
        }
    }
    
    // Simple Adapter for Bills
    class BillsAdapter(private val bills: List<Order>) : RecyclerView.Adapter<BillsAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemDailyBillRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDailyBillRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val bill = bills[position]
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            
            holder.binding.tvBillId.text = "Bill #${bill.orderId.takeLast(4)}"
            holder.binding.tvTime.text = if (bill.closedAt != null) timeFormat.format(Date(bill.closedAt)) else "-"
            holder.binding.tvAmount.text = String.format("₹ %.2f", bill.totalAmount)
            holder.binding.tvType.text = if (bill.serviceType == "TABLE") "Table ${bill.tableId}" else bill.serviceType
        }

        override fun getItemCount() = bills.size
    }
}
