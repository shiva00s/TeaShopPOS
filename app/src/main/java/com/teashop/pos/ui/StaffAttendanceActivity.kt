package com.teashop.pos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ActivityStaffAttendanceBinding
import com.teashop.pos.databinding.ItemFinanceRowBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class StaffAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffAttendanceBinding
    private val viewModel: StaffViewModel by viewModels()
    private var employee: Employee? = null
    private var viewState = ViewState.MONTHS // MONTHS or DAYS

    enum class ViewState { MONTHS, DAYS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        employee = intent.getSerializableExtra("EMPLOYEE") as? Employee
        if (employee == null) return finish()

        setupUI()
        setupOnBackPressed()
        showMonths()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = "${employee?.name}'s Attendance"
        binding.toolbar.setNavigationOnClickListener { 
            if (viewState == ViewState.DAYS) showMonths()
            else finish()
        }
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewState == ViewState.DAYS) {
                    showMonths()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showMonths() {
        viewState = ViewState.MONTHS
        binding.tvSelectionTitle.text = "Select Month"
        
        val months = (0..11).map { monthIndex ->
            val cal = Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }.reversed()

        binding.rvAttendance.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                ItemFinanceRowBinding.inflate(LayoutInflater.from(p.context), p, false).root
            ) {}
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val monthName = months[pos]
                val b = ItemFinanceRowBinding.bind(h.itemView)
                b.tvParticulars.text = monthName
                b.tvCredit.text = ">"
                b.root.setOnClickListener {
                    val cal = Calendar.getInstance()
                    val targetMonth = 11 - pos // Since we reversed
                    cal.set(Calendar.MONTH, targetMonth)
                    showDays(cal)
                }
            }
            override fun getItemCount() = months.size
        }
    }

    private fun showDays(cal: Calendar) {
        viewState = ViewState.DAYS
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = cal.timeInMillis

        binding.tvSelectionTitle.text = "Log for ${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)}"

        lifecycleScope.launch {
            viewModel.getAttendanceRecords(employee!!.employeeId, start, end).collect { records ->
                if (viewState != ViewState.DAYS) return@collect
                
                binding.rvAttendance.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                        ItemFinanceRowBinding.inflate(LayoutInflater.from(p.context), p, false).root
                    ) {}
                    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                        val r = records[pos]
                        val b = ItemFinanceRowBinding.bind(h.itemView)
                        val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        val checkOut = r.checkOutTime
                        val outTime = if (checkOut != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkOut) else "IN"
                        b.tvDate.text = df.format(r.checkInTime)
                        b.tvParticulars.text = "%s to %s".format(df.format(r.checkInTime), outTime)
                        val hours = if(checkOut != null) (checkOut - r.checkInTime) / 3600000.0 else 0.0
                        b.tvCredit.text = "%.1fh (%s)".format(hours, r.type)

                        b.root.setOnClickListener {
                            ManualAttendanceDialogFragment.newInstance(employee!!, r)
                                .show(supportFragmentManager, ManualAttendanceDialogFragment.TAG)
                        }
                    }
                    override fun getItemCount() = records.size
                }
            }
        }
    }
}
