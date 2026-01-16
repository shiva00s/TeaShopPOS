package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.Cashbook
import com.teashop.pos.databinding.ActivityReportsBinding
import com.teashop.pos.databinding.ItemCashbookRowBinding
import com.teashop.pos.ui.viewmodel.MainViewModel
import com.teashop.pos.ui.viewmodel.ReportsViewModel
import com.teashop.pos.ui.viewmodel.SharedViewModel
import com.teashop.pos.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val reportsViewModel: ReportsViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val staffViewModel: StaffViewModel by viewModels()
    @Inject lateinit var sharedViewModel: SharedViewModel
    @Inject lateinit var repository: MainRepository
    private var selectedDate: Calendar = Calendar.getInstance()
    private var shopId: String? = null
    private var currentFilter = "Up To Date"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        val shopName = intent.getStringExtra("SHOP_NAME")
        binding.toolbar.title = shopName

        shopId?.let { staffViewModel.setShop(it) }
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.rvFinanceBreakdown.layoutManager = LinearLayoutManager(this)

        binding.btnReportDate.setOnClickListener {
            DatePickerDialog(
                this, { _, year, month, day ->
                    selectedDate.set(year, month, day)
                    refreshReport()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnPrevDate.setOnClickListener { adjustDate(-1) }
        binding.btnNextDate.setOnClickListener { adjustDate(1) }

        binding.btnFilterType.setOnClickListener {
            val items = arrayOf("Daily", "Up To Date", "Weekly", "Monthly", "Quarterly", "Half Yearly", "Annually")
            MaterialAlertDialogBuilder(this)
                .setTitle("Filter Type")
                .setItems(items) { _, which ->
                    currentFilter = items[which]
                    refreshReport()
                }
                .show()
        }
    }

    private fun adjustDate(amount: Int) {
        when (currentFilter) {
            "Daily", "Up To Date" -> selectedDate.add(Calendar.DAY_OF_MONTH, amount)
            "Weekly" -> selectedDate.add(Calendar.WEEK_OF_YEAR, amount)
            "Monthly" -> selectedDate.add(Calendar.MONTH, amount)
            "Quarterly" -> selectedDate.add(Calendar.MONTH, amount * 3)
            "Half Yearly" -> selectedDate.add(Calendar.MONTH, amount * 6)
            "Annually" -> selectedDate.add(Calendar.YEAR, amount)
        }
        refreshReport()
    }

    private fun refreshReport() {
        lifecycleScope.launch {
            val start = selectedDate.clone() as Calendar
            val end = selectedDate.clone() as Calendar
            val format: SimpleDateFormat

            when (currentFilter) {
                "Daily" -> format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                "Up To Date" -> {
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    format = SimpleDateFormat("\'1st to\' dd MMM, yyyy", Locale.getDefault())
                }
                "Weekly" -> {
                    start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                    end.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek + 6)
                    format = SimpleDateFormat("\'Week\' w, yyyy", Locale.getDefault())
                }
                "Monthly" -> {
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                }
                "Quarterly" -> {
                    val quarter = (start.get(Calendar.MONTH) / 3)
                    start.set(Calendar.MONTH, quarter * 3)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.MONTH, (quarter * 3) + 2)
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    format = SimpleDateFormat("\'Q\'${quarter + 1} yyyy", Locale.getDefault())
                }
                "Half Yearly" -> {
                    val half = if (start.get(Calendar.MONTH) < 6) 0 else 1
                    start.set(Calendar.MONTH, half * 6)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.MONTH, (half * 6) + 5)
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    format = SimpleDateFormat(if (half == 0) "\'H1\' yyyy" else "\'H2\' yyyy", Locale.getDefault())
                }
                "Annually" -> {
                    start.set(Calendar.DAY_OF_YEAR, 1)
                    end.set(Calendar.YEAR, start.get(Calendar.YEAR))
                    end.set(Calendar.MONTH, 11)
                    end.set(Calendar.DAY_OF_MONTH, 31)
                    format = SimpleDateFormat("yyyy", Locale.getDefault())
                }
                else -> format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            }

            binding.btnReportDate.text = format.format(selectedDate.time)
            binding.btnFilterType.text = currentFilter

            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)

            val salary = staffViewModel.employeeStats.value.values.sumOf { it.monthlySalary }
            shopId?.let { reportsViewModel.loadReportsForPeriod(it, start.timeInMillis, end.timeInMillis, salary) }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    staffViewModel.employeeStats.collectLatest {
                        refreshReport()
                    }
                }

                launch {
                    combine(
                        reportsViewModel.cashFlow,
                        reportsViewModel.totalSalary,
                        reportsViewModel.totalFixedExpenses
                    ) { summary, salary, expenses ->
                        updateSummary(summary?.totalIn ?: 0.0, summary?.totalOut ?: 0.0, salary, expenses)
                    }.collect()
                }
                launch {
                    reportsViewModel.cashbookEntries.collect {
                        binding.rvFinanceBreakdown.adapter = CashbookAdapter(it,
                            onDelete = { entry -> reportsViewModel.deleteCashbookEntry(entry) },
                            onEdit = { entry ->
                                val intent = Intent(this@ReportsActivity, FinanceEntryActivity::class.java)
                                intent.putExtra("ENTRY_ID", entry.entryId)
                                intent.putExtra("SHOP_ID", shopId)
                                startActivity(intent)
                            })
                    }
                }
            }
        }
    }

    private fun updateSummary(totalIn: Double, totalOut: Double, salary: Double, fixedExpenses: Double) {
        val profit = totalIn - totalOut - salary - fixedExpenses
        binding.tvTotalCashIn.text = String.format("₹ %.2f", totalIn)
        binding.tvTotalCashOut.text = String.format("₹ %.2f", totalOut)
        binding.tvTotalSalary.text = String.format("₹ %.2f", salary)
        binding.tvFixedExpenses.text = String.format("₹ %.2f", fixedExpenses)
        binding.tvFinalProfit.text = String.format("₹ %.2f", profit)

        if (profit >= 0) {
            binding.tvFinalProfit.setTextColor(0xFFFFFFFF.toInt())
            binding.tvSummaryTitle.text = "NET ${currentFilter.uppercase()} PROFIT"
        } else {
            binding.tvFinalProfit.setTextColor(0xFFFCA5A5.toInt())
            binding.tvSummaryTitle.text = "NET ${currentFilter.uppercase()} LOSS"
        }

        shopId?.let { sharedViewModel.updateShopProfit(it, profit) }
    }

    class CashbookAdapter(
        private val items: List<Cashbook>,
        private val onDelete: (Cashbook) -> Unit,
        private val onEdit: (Cashbook) -> Unit
    ) : RecyclerView.Adapter<CashbookAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemCashbookRowBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemCashbookRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val format = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault())
            val timeStr = format.format(Date(item.transactionDate))
            val label = if (item.description.isNullOrBlank()) "${item.category} ($timeStr)" else "${item.category} ${item.description} ($timeStr)"
            holder.binding.tvLabel.text = label
            holder.binding.tvValue.text = String.format("₹ %.2f", item.amount)
            holder.binding.tvValue.setTextColor(if (item.transactionType == "IN") 0xFF2E7D32.toInt() else 0xFFC62828.toInt())
            holder.binding.btnDelete.setOnClickListener { onDelete(item) }
            holder.binding.btnEdit.setOnClickListener { onEdit(item) }
        }
        override fun getItemCount() = items.size
    }
}
