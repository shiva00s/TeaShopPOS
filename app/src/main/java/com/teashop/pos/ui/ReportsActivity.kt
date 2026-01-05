package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.dao.FinancialCategorySummary
import com.teashop.pos.data.dao.SmartStockReminder
import com.teashop.pos.databinding.ActivityReportsBinding
import com.teashop.pos.databinding.ItemFinanceRowBinding
import com.teashop.pos.ui.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var viewModel: ReportsViewModel
    private var selectedDate: Calendar = Calendar.getInstance()
    private var shopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(this, factory)[ReportsViewModel::class.java]

        shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        
        setupUI()
        observeViewModel()
        refreshReport()
    }

    private fun setupUI() {
        binding.rvFinanceBreakdown.layoutManager = LinearLayoutManager(this)
        binding.rvLowStock.layoutManager = LinearLayoutManager(this)

        binding.btnReportDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.btnReportDate.text = "Summary Date: ${format.format(selectedDate.time)}"
                refreshReport()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun refreshReport() {
        val start = selectedDate.clone() as Calendar
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        
        val end = selectedDate.clone() as Calendar
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        
        shopId?.let { viewModel.loadReportsForPeriod(it, start.timeInMillis, end.timeInMillis) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.smartStock.collect { items ->
                        if (items.isNotEmpty()) {
                            binding.cardLowStock.visibility = View.VISIBLE
                            binding.rvLowStock.adapter = SmartStockAdapter(items)
                        } else {
                            binding.cardLowStock.visibility = View.GONE
                        }
                    }
                }
                launch {
                    viewModel.cashFlow.collect { summary ->
                        val totalIn = summary?.totalIn ?: 0.0
                        val totalOut = summary?.totalOut ?: 0.0
                        val profit = totalIn - totalOut
                        
                        binding.tvTotalCashIn.text = String.format("₹ %.2f", totalIn)
                        binding.tvTotalCashOut.text = String.format("₹ %.2f", totalOut)
                        binding.tvFinalProfit.text = String.format("₹ %.2f", profit)
                        
                        if (profit >= 0) {
                            binding.tvFinalProfit.setTextColor(0xFF2E7D32.toInt())
                            binding.tvSummaryTitle.text = "NET DAILY PROFIT"
                        } else {
                            binding.tvFinalProfit.setTextColor(0xFFC62828.toInt())
                            binding.tvSummaryTitle.text = "NET DAILY LOSS"
                        }
                    }
                }
                launch {
                    viewModel.financeBreakdown.collect { binding.rvFinanceBreakdown.adapter = FinanceAdapter(it) }
                }
            }
        }
    }

    class FinanceAdapter(private val items: List<FinancialCategorySummary>) :
        RecyclerView.Adapter<FinanceAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemFinanceRowBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemFinanceRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val label = if (item.description.isNullOrBlank()) item.category else "${item.category} ${item.description}"
            holder.binding.tvLabel.text = label
            holder.binding.tvValue.text = String.format("₹ %.2f", item.totalAmount)
            holder.binding.tvValue.setTextColor(if (item.transactionType == "IN") 0xFF2E7D32.toInt() else 0xFFC62828.toInt())
        }
        override fun getItemCount() = items.size
    }

    class SmartStockAdapter(private val items: List<SmartStockReminder>) :
        RecyclerView.Adapter<SmartStockAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemFinanceRowBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemFinanceRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val isWeekend = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in arrayOf(Calendar.SATURDAY, Calendar.SUNDAY)
            val standard = if (isWeekend) item.weekendStandard else item.weekdayStandard
            val required = standard - item.currentStock
            
            holder.binding.tvLabel.text = "${item.itemName} (Need: ${required.toInt()} from ${item.supplierName ?: "Unknown"})"
            holder.binding.tvValue.text = "Stock: ${item.currentStock.toInt()}"
            holder.binding.tvValue.setTextColor(if (required > 0) 0xFFC62828.toInt() else 0xFF2E7D32.toInt())
        }
        override fun getItemCount() = items.size
    }
}
