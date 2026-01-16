package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.teashop.pos.R
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.Cashbook
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ActivityDetailedReportBinding
import com.teashop.pos.ui.viewmodel.MainViewModel
import com.teashop.pos.ui.viewmodel.ReportsViewModel
import com.teashop.pos.ui.viewmodel.SharedViewModel
import com.teashop.pos.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DetailedReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailedReportBinding
    private val reportsViewModel: ReportsViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val staffViewModel: StaffViewModel by viewModels()
    @Inject lateinit var sharedViewModel: SharedViewModel
    @Inject lateinit var repository: MainRepository
    private lateinit var ledgerAdapter: LedgerAdapter
    private var selectedDate: Calendar = Calendar.getInstance()
    private var shopId: String? = null
    private var currentFilter = "Up To Date"
    private var employeeMap = mapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailedReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID")
        val shopName = intent.getStringExtra("SHOP_NAME")
        binding.toolbar.title = shopName

        shopId?.let { staffViewModel.setShop(it) }
        setupUI()
        observeViewModel()
        refreshReport()
    }

    private fun setupUI() {
        ledgerAdapter = LedgerAdapter(::updateFilteredSum)
        binding.rvDetailedBreakdown.layoutManager = LinearLayoutManager(this)
        binding.rvDetailedBreakdown.adapter = ledgerAdapter

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

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ledgerAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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
                    format = SimpleDateFormat("'1st to' dd MMM, yyyy", Locale.getDefault())
                }
                "Weekly" -> {
                    start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                    end.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek + 6)
                    format = SimpleDateFormat("'Week' w, yyyy", Locale.getDefault())
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
                    format = SimpleDateFormat("'Q'${quarter + 1} yyyy", Locale.getDefault())
                }
                "Half Yearly" -> {
                    val half = if (start.get(Calendar.MONTH) < 6) 0 else 1
                    start.set(Calendar.MONTH, half * 6)
                    start.set(Calendar.DAY_OF_MONTH, 1)
                    end.set(Calendar.MONTH, (half * 6) + 5)
                    end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                    format = SimpleDateFormat(if (half == 0) "'H1' yyyy" else "'H2' yyyy", Locale.getDefault())
                }
                "Annually" -> {
                    start.set(Calendar.DAY_OF_YEAR, 1)
                    end.set(Calendar.MONTH, 11)
                    end.set(Calendar.DAY_OF_MONTH, 31)
                    format = SimpleDateFormat("yyyy", Locale.getDefault())
                }
                else -> format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            }

            binding.btnReportDate.text = format.format(selectedDate.time)
            binding.btnFilterType.text = currentFilter

            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            end.set(Calendar.MILLISECOND, 999)

            val salary = staffViewModel.employeeStats.value.values.sumOf { it.monthlySalary }
            reportsViewModel.loadReportsForPeriod(shopId, start.timeInMillis, end.timeInMillis, salary)
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

                if (!shopId.isNullOrEmpty()) {
                    launch {
                        mainViewModel.getShopEmployees(shopId!!).collectLatest { employees: List<Employee> ->
                            employeeMap = employees.associate { emp -> emp.employeeId to emp.name }
                            ledgerAdapter.setEmployeeMap(employeeMap)
                        }
                    }
                } else {
                    launch {
                        mainViewModel.getAllEmployees().collectLatest { employees: List<Employee> ->
                            employeeMap = employees.associate { emp -> emp.employeeId to emp.name }
                            ledgerAdapter.setEmployeeMap(employeeMap)
                        }
                    }
                }

                launch {
                    reportsViewModel.cashbookEntries.collectLatest {
                        ledgerAdapter.updateList(it)
                    }
                }

                launch {
                    reportsViewModel.cashFlow.collectLatest {
                        updateSummary()
                    }
                }

                launch {
                    reportsViewModel.totalSalary.collectLatest {
                        updateSummary()
                    }
                }

                launch {
                    reportsViewModel.totalFixedExpenses.collectLatest {
                        updateSummary()
                    }
                }
            }
        }
    }

    private fun updateSummary() {
        val totalIn = reportsViewModel.cashFlow.value?.totalIn ?: 0.0
        val totalOut = reportsViewModel.cashFlow.value?.totalOut ?: 0.0
        val salary = reportsViewModel.totalSalary.value
        val fixedExpenses = reportsViewModel.totalFixedExpenses.value
        val result = totalIn - totalOut - salary - fixedExpenses
        
        binding.tvTotalIn.text = String.format(Locale.getDefault(), "₹ %.2f", totalIn)
        binding.tvTotalOut.text = String.format(Locale.getDefault(), "₹ %.2f", totalOut)
        binding.tvTotalSalary.text = String.format(Locale.getDefault(), "₹ %.2f", salary)
        binding.tvFixedExpenses.text = String.format(Locale.getDefault(), "₹ %.2f", fixedExpenses)
        binding.tvNetResult.text = String.format(Locale.getDefault(), "₹ %.2f", result)
        binding.tvNetResult.setTextColor(if (result >= 0) 0xFF2E7D32.toInt() else 0xFFC62828.toInt())

        shopId?.let { sharedViewModel.updateShopProfit(it, result) }
    }

    private fun updateFilteredSum(sum: Double, isFilterActive: Boolean) {
        if (isFilterActive) {
            binding.tvFilteredSum.visibility = View.VISIBLE
            binding.tvFilteredSum.text = String.format(Locale.getDefault(), "Filtered Total: ₹%.2f", sum)
        } else {
            binding.tvFilteredSum.visibility = View.GONE
        }
    }

    class LedgerAdapter(private val onFilter: (Double, Boolean) -> Unit) :
        RecyclerView.Adapter<LedgerAdapter.ViewHolder>() {
        private var items: List<Cashbook> = listOf()
        private var filteredItems: List<Cashbook> = listOf()
        private var employeeMap: Map<String, String> = mapOf()

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
            val tvParticulars: TextView = itemView.findViewById(R.id.tvParticulars)
            val tvDebit: TextView = itemView.findViewById(R.id.tvDebit)
            val tvCredit: TextView = itemView.findViewById(R.id.tvCredit)
        }

        fun updateList(newItems: List<Cashbook>) {
            items = newItems
            filter("")
        }

        fun setEmployeeMap(map: Map<String, String>){
            employeeMap = map
            notifyDataSetChanged()
        }

        fun filter(query: String) {
            filteredItems = if (query.isEmpty()) {
                items
            } else {
                items.filter { 
                    val particulars = getParticulars(it)
                    particulars.contains(query, ignoreCase = true)
                }
            }
            val sum = filteredItems.sumOf { if(it.transactionType == "IN") it.amount else -it.amount }
            onFilter(sum, query.isNotEmpty())
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ledger_row, parent, false)
            return ViewHolder(view)
        }

        private fun getEmojiForEntry(category: String, description: String?): String {
            val searchText = "$category ${description.orEmpty()}"
            return when {
                searchText.contains("MILK", ignoreCase = true) -> "🥛"
                searchText.contains("BREAD", ignoreCase = true) -> "🍞"
                searchText.contains("FOOD", ignoreCase = true) -> "🍔"
                category.equals("DAILY SALES (CASH)", ignoreCase = true) -> "💵"
                category.equals("DAILY SALES (QR)", ignoreCase = true) -> "📱"
                category.equals("OPENING CASH", ignoreCase = true) -> "💰"
                category.equals("PURCHASE", ignoreCase = true) -> "🛒"
                category.equals("SALARY", ignoreCase = true) -> "🧑‍💼"
                category.equals("ADVANCE", ignoreCase = true) -> "💸"
                category.equals("RENT", ignoreCase = true) -> "🏠"
                category.equals("EB/GAS", ignoreCase = true) -> "💡"
                category.equals("MAINTENANCE", ignoreCase = true) -> "🔧"
                else -> "💲"
            }
        }

        private fun getParticulars(item: Cashbook) : String {
            val description = item.description
            var particulars = if (description.isNullOrEmpty()) {
                item.category
            } else {
                if (description.startsWith(item.category, ignoreCase = true)) {
                    description
                } else {
                    "${item.category} - $description"
                }
            }
            if (item.category.equals("ADVANCE", ignoreCase = true) || item.category.equals("SALARY", ignoreCase = true)) {
                item.referenceId?.let { refId ->
                    employeeMap[refId]?.let {
                        particulars = "${item.category} to $it"
                    } 
                }
            }
            return particulars
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredItems[position]
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            holder.tvDate.text = dateFormat.format(Date(item.transactionDate))
            holder.tvEmoji.text = getEmojiForEntry(item.category, item.description)
            holder.tvParticulars.text = getParticulars(item)

            if (item.transactionType == "IN") {
                holder.tvCredit.text = String.format(Locale.getDefault(), "₹%.2f", item.amount)
                holder.tvCredit.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
                holder.tvDebit.text = ""
            } else {
                holder.tvDebit.text = String.format(Locale.getDefault(), "₹%.2f", item.amount)
                holder.tvDebit.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
                holder.tvCredit.text = ""
            }
        }
        override fun getItemCount() = filteredItems.size
    }
}