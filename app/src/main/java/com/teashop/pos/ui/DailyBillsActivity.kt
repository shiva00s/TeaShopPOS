package com.teashop.pos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.OrderWithItems
import com.teashop.pos.databinding.ActivityDailyBillsBinding
import com.teashop.pos.databinding.ItemDailyBillRowBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DailyBillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyBillsBinding
    private var shopId: String? = null

    @Inject
    lateinit var repository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyBillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID")
        
        binding.toolbar.title = "Daily Bills"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.rvBills.layoutManager = LinearLayoutManager(this)
        
        loadBills()
    }

    private fun loadBills() {
        val sId = shopId ?: return
        
        lifecycleScope.launch {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            repository.getOrdersWithItemsForPeriod(sId, todayStart, todayEnd).collectLatest { ordersWithItems ->
                binding.rvBills.adapter = BillsAdapter(ordersWithItems)
            }
        }
    }
    
    class BillsAdapter(private val bills: List<OrderWithItems>) : RecyclerView.Adapter<BillsAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemDailyBillRowBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDailyBillRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val bill = bills[position].order
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            
            holder.binding.tvBillId.text = "Bill #${bill.orderId.takeLast(4)}"
            holder.binding.tvTime.text = bill.closedAt?.let { timeFormat.format(Date(it)) } ?: "-"
            holder.binding.tvAmount.text = String.format("₹ %.2f", bill.totalAmount)
            holder.binding.tvType.text = if (bill.serviceType == "TABLE") "Table ${bill.tableId}" else bill.serviceType
        }

        override fun getItemCount() = bills.size
    }
}
