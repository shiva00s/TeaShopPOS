package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.R
import com.teashop.pos.data.entity.Attendance
import com.teashop.pos.data.entity.AttendanceType
import com.teashop.pos.databinding.ActivityShopClosedDaysBinding
import com.teashop.pos.databinding.ItemClosedDayBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ShopClosedDaysActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopClosedDaysBinding
    private val viewModel: StaffViewModel by viewModels()
    private var shopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopClosedDaysBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        val shopName = intent.getStringExtra("SHOP_NAME")
        binding.toolbar.title = shopName
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.rvClosedDays.layoutManager = LinearLayoutManager(this)
        binding.fabAddClosedDay.setOnClickListener { showAddClosedDayDialog() }
    }

    private fun observeViewModel() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getAttendanceRecords(shopId!!, start, end).collect { attendances ->
                    val closedDays = attendances.filter { it.attendanceType == AttendanceType.SHOP_CLOSED_PAID || it.attendanceType == AttendanceType.SHOP_CLOSED_UNPAID }.distinctBy { it.checkInTime }
                    binding.rvClosedDays.adapter = ClosedDaysAdapter(closedDays) { day ->
                        viewModel.deleteAttendance(day)
                    }
                }
            }
        }
    }

    private fun showAddClosedDayDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_closed_day, null)
        val etReason = view.findViewById<EditText>(R.id.etReason)
        val cbPaySalary = view.findViewById<CheckBox>(R.id.cbPaySalary)
        val etDate = view.findViewById<EditText>(R.id.etDate)
        
        val selectedDate = Calendar.getInstance()
        val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        etDate.setText(df.format(selectedDate.time))

        etDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d, 0, 0, 0)
                selectedDate.set(Calendar.MILLISECOND, 0)
                etDate.setText(df.format(selectedDate.time))
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Shop Closed Day")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                viewModel.addShopClosedDay(selectedDate.time, cbPaySalary.isChecked)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class ClosedDaysAdapter(
        private val items: List<Attendance>,
        private val onDelete: (Attendance) -> Unit
    ) : RecyclerView.Adapter<ClosedDaysAdapter.ViewHolder>() {
        class ViewHolder(val binding: ItemClosedDayBinding) : RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ItemClosedDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.binding.tvDate.text = df.format(Date(item.checkInTime))
            holder.binding.tvReason.text = "Shop Closed"
            holder.binding.tvSalaryStatus.text = if (item.attendanceType == AttendanceType.SHOP_CLOSED_PAID) "Salary: YES ✅" else "Salary: NO ❌"
            holder.binding.btnDelete.setOnClickListener { onDelete(item) }
        }
        override fun getItemCount() = items.size
    }
}