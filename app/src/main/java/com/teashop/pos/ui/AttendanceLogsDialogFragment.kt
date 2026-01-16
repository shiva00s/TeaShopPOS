package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.R
import com.teashop.pos.data.entity.Attendance
import com.teashop.pos.databinding.DialogAttendanceLogsBinding
import com.teashop.pos.databinding.ItemFinanceRowBinding
import com.teashop.pos.databinding.ItemMonthCardBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AttendanceLogsDialogFragment : DialogFragment() {

    private lateinit var viewModel: StaffViewModel
    private var _binding: DialogAttendanceLogsBinding? = null
    private val binding get() = _binding!!

    private var currentView = "MONTHS"
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private val today = Calendar.getInstance()
    private var isFirstOpen = true

    data class DaySummary(
        val dateLabel: String,
        val dateKey: String,
        val totalWork: Double,
        val totalGap: Double,
        val breakHrs: Double,
        val netHours: Double,
        val status: String, // WORK, ABSENT, CLOSED_PAID, CLOSED_UNPAID, BEFORE_HIRE, AFTER_TERMINATE
        val sessions: List<Attendance>
    )

    companion object {
        const val TAG = "AttendanceLogsDialog"
        private const val ARG_EMPLOYEE_ID = "employeeId"
        private const val ARG_EMPLOYEE_NAME = "employeeName"

        fun newInstance(employeeId: String, employeeName: String): AttendanceLogsDialogFragment {
            val args = Bundle().apply {
                putString(ARG_EMPLOYEE_ID, employeeId)
                putString(ARG_EMPLOYEE_NAME, employeeName)
            }
            return AttendanceLogsDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[StaffViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAttendanceLogsBinding.inflate(LayoutInflater.from(context))
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)!!
        val employeeName = requireArguments().getString(ARG_EMPLOYEE_NAME)!!

        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.btnYearToggle.text = selectedYear.toString()

        binding.btnYearToggle.setOnClickListener {
            showYearPickerDialog()
        }

        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle("$employeeName's Attendance Logs")
            .setView(binding.root)
            .setPositiveButton("Close", null)
            .setNegativeButton("Back", null)
            .create()

        dialog.setOnShowListener {
            val backBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            backBtn.setOnClickListener {
                if (currentView == "DAYS") {
                    showMonthsView(employeeId)
                }
            }
            
            showMonthsView(employeeId)
            
            if (isFirstOpen && selectedYear == today.get(Calendar.YEAR)) {
                isFirstOpen = false
                showDaysView(employeeId, today)
            }
        }

        return dialog
    }

    private fun showYearPickerDialog() {
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)!!
        val years = arrayOf("2024", "2025", "2026")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Year")
            .setItems(years) { _, which ->
                selectedYear = years[which].toInt()
                binding.btnYearToggle.text = selectedYear.toString()
                showMonthsView(employeeId)
            }
            .show()
    }

    private fun showMonthsView(employeeId: String) {
        currentView = "MONTHS"
        binding.tvCurrentViewLabel.text = "Select Month"
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
        
        val monthsList = mutableListOf<Calendar>()
        val maxMonth = if (selectedYear == today.get(Calendar.YEAR)) today.get(Calendar.MONTH) else 11
        
        for (i in 0..maxMonth) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, selectedYear)
            cal.set(Calendar.MONTH, i)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            monthsList.add(cal)
        }
        monthsList.reverse()

        binding.rvLogs.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                ItemMonthCardBinding.inflate(LayoutInflater.from(p.context), p, false).root
            ) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                val cal = monthsList[pos]
                val itemBinding = ItemMonthCardBinding.bind(h.itemView)
                val df = SimpleDateFormat("MMMM", Locale.getDefault())
                itemBinding.tvMonthName.text = df.format(cal.time)
                itemBinding.root.setOnClickListener { showDaysView(employeeId, cal) }
            }
            override fun getItemCount() = monthsList.size
        }
    }

    private fun showDaysView(employeeId: String, monthCal: Calendar) {
        currentView = "DAYS"
        binding.tvCurrentViewLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.VISIBLE
        
        val employee = viewModel.employees.value.find { it.employeeId == employeeId } ?: return
        val shopId = employee.shopId
        
        val start = monthCal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0)
        
        val end = monthCal.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59)

        lifecycleScope.launch {
            combine(
                viewModel.getAttendanceRecords(employeeId, start.timeInMillis, end.timeInMillis),
                viewModel.getClosedDays(shopId, start.timeInMillis, end.timeInMillis)
            ) { records, closedDays ->
                records to closedDays
            }.collect { (records, closedDays) ->
                if (currentView != "DAYS") return@collect
                
                val daySummaries = mutableListOf<DaySummary>()
                
                val groupedRecords = records.groupBy { 
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.checkInTime))
                }
                val groupedClosed = closedDays.associateBy {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
                }

                val maxDay = if (monthCal.get(Calendar.MONTH) == today.get(Calendar.MONTH) && 
                                 monthCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                    today.get(Calendar.DAY_OF_MONTH)
                } else {
                    monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                }

                for (day in 1..maxDay) {
                    val cal = monthCal.clone() as Calendar
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                    val dateLabel = SimpleDateFormat("dd/MM", Locale.getDefault()).format(cal.time)
                    
                    val dayRecords = groupedRecords[dateKey] ?: emptyList()
                    val closedDay = groupedClosed[dateKey]
                    
                    var status: String
                    var net: Double
                    var work = 0.0
                    var gap = 0.0
                    var dayBreak = employee.breakHours // Default from profile

                    val hireCal = Calendar.getInstance().apply { timeInMillis = employee.hireDate }
                    hireCal.set(Calendar.HOUR_OF_DAY, 0); hireCal.set(Calendar.MINUTE, 0); hireCal.set(Calendar.SECOND, 0); hireCal.set(Calendar.MILLISECOND, 0)
                    
                    val terminateCal = employee.terminateDate?.let {
                        Calendar.getInstance().apply { timeInMillis = it }
                    }
                    if (terminateCal != null) {
                        terminateCal.set(Calendar.HOUR_OF_DAY, 0); terminateCal.set(Calendar.MINUTE, 0); terminateCal.set(Calendar.SECOND, 0); terminateCal.set(Calendar.MILLISECOND, 0)
                    }

                    if (cal.timeInMillis < hireCal.timeInMillis) {
                        status = "BEFORE_HIRE"
                        net = 0.0
                    } else if (terminateCal != null && cal.timeInMillis > terminateCal.timeInMillis) {
                        status = "AFTER_TERMINATE"
                        net = 0.0
                    } else if (dayRecords.any { it.type == "WORK" }) {
                        dayRecords.filter { it.type == "WORK" }.forEach { 
                            val checkOut = it.checkOutTime
                            work += if (checkOut == null) (System.currentTimeMillis() - it.checkInTime) / 3600000.0 else (checkOut - it.checkInTime) / 3600000.0
                            dayBreak = it.breakHours
                        }
                        gap = dayRecords.filter { it.type == "GAP" }.sumOf {
                            val checkOut = it.checkOutTime
                            if (checkOut != null) (checkOut - it.checkInTime) / 3600000.0 else 0.0
                        }
                        status = "WORK"
                        net = work - gap - if (work > 0) dayBreak else 0.0
                    } else if (closedDay != null) {
                        if (closedDay.paySalary) { status = "CLOSED_PAID"; net = 8.0 }
                        else { status = "CLOSED_UNPAID"; net = 0.0 }
                    } else {
                        status = "ABSENT"
                        net = 0.0
                    }
                    
                    if (status != "AFTER_TERMINATE") {
                        daySummaries.add(DaySummary(dateLabel, dateKey, work, gap, dayBreak, net, status, dayRecords))
                    }
                }
                daySummaries.reverse()

                binding.rvLogs.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                        ItemFinanceRowBinding.inflate(LayoutInflater.from(p.context), p, false).root
                    ) {}
                    
                    override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                        val summary = daySummaries[pos]
                        val itemBinding = ItemFinanceRowBinding.bind(h.itemView)
                        
                        itemBinding.tvDate.text = summary.dateLabel

                        when (summary.status) {
                            "BEFORE_HIRE" -> {
                                itemBinding.tvParticulars.text = "Not yet hired"
                                itemBinding.tvDebit.text = "-"
                                itemBinding.tvCredit.text = "-"
                            }
                            "WORK" -> {
                                itemBinding.tvParticulars.text = String.format(Locale.getDefault(), "Work: %.1fh | Brk: %.1fh | Gap: %.1fh", summary.totalWork, summary.breakHrs, summary.totalGap)
                                itemBinding.tvCredit.text = "%.2f hrs".format(summary.netHours)
                                itemBinding.tvCredit.setTextColor(ContextCompat.getColor(h.itemView.context, R.color.green))
                                itemBinding.tvDebit.text = ""
                            }
                            "CLOSED_PAID" -> {
                                itemBinding.tvParticulars.text = "Shop Closed (Paid)"
                                itemBinding.tvCredit.text = "8.00 hrs"
                                itemBinding.tvCredit.setTextColor(ContextCompat.getColor(h.itemView.context, R.color.green))
                                itemBinding.tvDebit.text = ""
                            }
                            "CLOSED_UNPAID" -> {
                                itemBinding.tvParticulars.text = "Shop Closed (Unpaid)"
                                itemBinding.tvCredit.text = "0.00 hrs"
                                itemBinding.tvDebit.text = ""
                            }
                            "ABSENT" -> {
                                itemBinding.tvParticulars.text = "Absent"
                                itemBinding.tvDebit.text = "ABSENT"
                                itemBinding.tvDebit.setTextColor(ContextCompat.getColor(h.itemView.context, R.color.red))
                                itemBinding.tvCredit.text = ""
                            }
                        }
                        
                        itemBinding.root.setOnClickListener {
                            if (summary.status == "WORK") {
                                showDayDetailsDialog(summary)
                            }
                        }
                    }
                    override fun getItemCount() = daySummaries.size
                }
            }
        }
    }

    private fun showDayDetailsDialog(summary: DaySummary) {
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)
        val employee = viewModel.employees.value.find { it.employeeId == employeeId } ?: return
        
        val options = summary.sessions.map { 
            val df = SimpleDateFormat("HH:mm", Locale.getDefault())
            val checkOut = it.checkOutTime
            val out = if (checkOut != null) df.format(Date(checkOut)) else "RUNNING"
            val hours = if (checkOut != null) {
                (checkOut - it.checkInTime) / 3600000.0
            } else {
                (System.currentTimeMillis() - it.checkInTime) / 3600000.0
            }
            "${df.format(Date(it.checkInTime))} - $out (${it.type}: %.2fh)".format(hours)
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Sessions for ${summary.dateLabel}")
            .setItems(options) { _, which ->
                val session = summary.sessions[which]
                ManualAttendanceDialogFragment.newInstance(employee, session)
                    .show(parentFragmentManager, ManualAttendanceDialogFragment.TAG)
            }
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
