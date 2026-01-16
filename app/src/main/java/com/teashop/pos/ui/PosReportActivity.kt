package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.R
import com.teashop.pos.data.OrderWithItems
import com.teashop.pos.databinding.ActivityPosReportBinding
import com.teashop.pos.databinding.ItemPosBillDetailBinding
import com.teashop.pos.ui.viewmodel.ReportsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PosReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPosReportBinding
    private val viewModel: ReportsViewModel by viewModels()
    private var shopId: String? = null
    private var selectedCalendar: Calendar = Calendar.getInstance()
    
    enum class ReportPeriod { DAY, WEEK, MONTH, QUARTER, HALF_YEAR, ANNUAL }
    private var currentPeriod = ReportPeriod.DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        binding.toolbar.title = intent.getStringExtra("SHOP_NAME")

        setupUI()
        observeViewModel()
        refreshData()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvBillDetails.layoutManager = LinearLayoutManager(this)

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            currentPeriod = when (checkedId) {
                R.id.chipDay -> ReportPeriod.DAY
                R.id.chipWeek -> ReportPeriod.WEEK
                R.id.chipMonth -> ReportPeriod.MONTH
                R.id.chipQuarter -> ReportPeriod.QUARTER
                R.id.chipHalfYear -> ReportPeriod.HALF_YEAR
                R.id.chipYear -> ReportPeriod.ANNUAL
                else -> ReportPeriod.DAY
            }
            updateFilterText()
            refreshData()
        }

        binding.btnPreviousDate.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, -1)
            updateFilterText()
            refreshData()
        }

        binding.btnNextDate.setOnClickListener {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
            updateFilterText()
            refreshData()
        }
        
        binding.llTopSellingHeader.setOnClickListener {
            toggleVisibility(binding.tvTopSellingItems, binding.ivTopSellingArrow)
        }

        binding.llLowSellingHeader.setOnClickListener {
            toggleVisibility(binding.tvLowSellingItems, binding.ivLowSellingArrow)
        }

        updateFilterText()
    }

    private fun toggleVisibility(view: View, arrow: View) {
        val isVisible = view.visibility == View.VISIBLE
        view.visibility = if (isVisible) View.GONE else View.VISIBLE
        arrow.rotation = if (isVisible) 90f else 270f
    }

    private fun updateFilterText() {
        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val sdfDay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val year = selectedCalendar.get(Calendar.YEAR)

        val displayText = when (currentPeriod) {
            ReportPeriod.DAY -> sdfDay.format(selectedCalendar.time)
            ReportPeriod.WEEK -> {
                val weekStart = selectedCalendar.clone() as Calendar
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
                val weekEnd = weekStart.clone() as Calendar
                weekEnd.add(Calendar.DAY_OF_WEEK, 6)
                "${sdfDay.format(weekStart.time)} - ${sdfDay.format(weekEnd.time)}"
            }
            ReportPeriod.MONTH -> sdfMonth.format(selectedCalendar.time)
            ReportPeriod.QUARTER -> {
                val quarter = (selectedCalendar.get(Calendar.MONTH) / 3) + 1
                "Quarter $quarter, $year"
            }
            ReportPeriod.HALF_YEAR -> {
                val half = (selectedCalendar.get(Calendar.MONTH) / 6) + 1
                "Half Year $half, $year"
            }
            ReportPeriod.ANNUAL -> "Year $year"
        }
        
        binding.tvSelectedDate.text = displayText
        binding.tvCurrentFilter.text = String.format(Locale.getDefault(), "Viewing: %s", displayText)
    }

    private fun refreshData() {
        val start = selectedCalendar.clone() as Calendar
        val end = selectedCalendar.clone() as Calendar

        when (currentPeriod) {
            ReportPeriod.DAY -> {
                // The selected day is the start and end
            }
            ReportPeriod.WEEK -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek + 6)
            }
            ReportPeriod.MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            ReportPeriod.QUARTER -> {
                val quarter = start.get(Calendar.MONTH) / 3
                start.set(Calendar.MONTH, quarter * 3)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, (quarter * 3) + 2)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            ReportPeriod.HALF_YEAR -> {
                val half = if (start.get(Calendar.MONTH) < 6) 0 else 1
                start.set(Calendar.MONTH, half * 6)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, (half * 6) + 5)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            ReportPeriod.ANNUAL -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                end.set(Calendar.YEAR, start.get(Calendar.YEAR))
                end.set(Calendar.MONTH, 11)
                end.set(Calendar.DAY_OF_MONTH, 31)
            }
        }

        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)

        shopId?.let {
            viewModel.loadPosReports(it, start.timeInMillis, end.timeInMillis)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.posBillReports.collect { billList ->
                        binding.rvBillDetails.adapter = BillDetailsAdapter(billList, 
                            onEdit = { showEditDialog(it) },
                            onDelete = { showDeleteDialog(it) }
                        )
                    }
                }

                launch {
                    viewModel.posReportSummary.collect { summary ->
                        summary?.let {
                            binding.tvTotalSales.text = String.format(Locale.getDefault(), "Total Sales: ₹ %.2f", it.totalSales)
                            binding.tvCashSales.text = String.format(Locale.getDefault(), "Cash: ₹ %.2f", it.cashSales)
                            binding.tvQrSales.text = String.format(Locale.getDefault(), "QR: ₹ %.2f", it.qrSales)
                        }
                    }
                }

                launch {
                    viewModel.topSellingItems.collect { items ->
                        binding.tvTopSellingItems.text = items.joinToString("\n") { "- ${it.first}: ${it.second}" }
                    }
                }

                launch {
                    viewModel.lowSellingItems.collect { items ->
                        binding.tvLowSellingItems.text = items.joinToString("\n") { "- ${it.first}: ${it.second}" }
                    }
                }
            }
        }
    }

    private fun showEditDialog(orderWithItems: OrderWithItems) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_bill_amount, null)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvBillInfo)
        val etAmount = dialogView.findViewById<EditText>(R.id.etPayableAmount)
        
        tvInfo.text = String.format(Locale.getDefault(), "Bill #%s - Current Total: ₹ %.2f", 
            orderWithItems.order.orderId.takeLast(6), orderWithItems.order.payableAmount)
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", orderWithItems.order.payableAmount))

        AlertDialog.Builder(this)
            .setTitle("Quick Edit Bill Amount")
            .setView(dialogView)
            .setPositiveButton("Update Amount") { _, _ ->
                val newAmount = etAmount.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                val updatedOrder = orderWithItems.order.copy(
                    totalAmount = newAmount,
                    payableAmount = newAmount,
                    lastModified = System.currentTimeMillis(),
                    isSynced = false
                )
                viewModel.updateOrderAndItems(updatedOrder, orderWithItems.items)
                Toast.makeText(this, "Bill updated", Toast.LENGTH_SHORT).show()
                refreshData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(orderWithItems: OrderWithItems) {
        AlertDialog.Builder(this)
            .setTitle("Delete Bill?")
            .setMessage(String.format(Locale.getDefault(), "Are you sure you want to delete Bill #%s?", orderWithItems.order.orderId.takeLast(6)))
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteOrder(orderWithItems.order)
                Toast.makeText(this, "Bill deleted", Toast.LENGTH_SHORT).show()
                refreshData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class BillDetailsAdapter(
        private val items: List<OrderWithItems>,
        private val onEdit: (OrderWithItems) -> Unit,
        private val onDelete: (OrderWithItems) -> Unit
    ) : RecyclerView.Adapter<BillDetailsAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemPosBillDetailBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemPosBillDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val closedAt = item.order.closedAt
            val timeStr = if (closedAt != null) timeFormat.format(Date(closedAt)) else "N/A"
            
            holder.binding.tvBillHeader.text = String.format(Locale.getDefault(), "Bill #%s - %s", item.order.orderId.takeLast(6), timeStr)
            
            val methodText = when(item.order.paymentMethod) {
                "SPLIT" -> String.format(Locale.getDefault(), "SPLIT (C:₹%.2f, Q:₹%.2f)", item.order.cashAmount, item.order.onlineAmount)
                "ONLINE" -> "QR/ONLINE"
                else -> item.order.paymentMethod ?: "N/A"
            }
            holder.binding.tvPaymentMethod.text = methodText

            val serviceText = if (item.order.serviceType == "TABLE") {
                String.format(Locale.getDefault(), "Table %s", item.order.tableId ?: "N/A")
            } else {
                item.order.serviceType
            }
            holder.binding.tvServiceType.text = serviceText
            
            val itemsSummary = item.items.joinToString(", ") { String.format(Locale.getDefault(), "%s x %d", it.itemName, it.quantity.toInt()) }
            holder.binding.tvItemsSummary.text = itemsSummary
            holder.binding.tvBillTotal.text = String.format(Locale.getDefault(), "Total: ₹ %.2f", item.order.payableAmount)

            holder.itemView.setOnClickListener { onEdit(item) }
            holder.itemView.setOnLongClickListener { 
                onDelete(item)
                true
            }
        }
        override fun getItemCount() = items.size
    }
}
