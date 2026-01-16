package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ItemStaffCardBinding
import com.teashop.pos.ui.viewmodel.EmployeeStats
import java.util.Calendar

class StaffAdapter(
    private val onAttendanceClick: (Employee) -> Unit,
    private val onAdvanceClick: (Employee) -> Unit,
    private val onAdvanceLogsClick: (Employee) -> Unit, // Added this
    private val onSalaryClick: (Employee) -> Unit,
    private val onViewLogsClick: (Employee) -> Unit,
    private val onEditClick: (Employee) -> Unit,
    private val onDeleteClick: (Employee) -> Unit,
    private var statsMap: Map<String, EmployeeStats> = emptyMap()
) : ListAdapter<Employee, StaffAdapter.StaffViewHolder>(EmployeeDiffCallback()) {

    fun updateStats(newStats: Map<String, EmployeeStats>) {
        statsMap = newStats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemStaffCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val employee = getItem(position)
        holder.bind(employee, statsMap[employee.employeeId])
    }

    inner class StaffViewHolder(private val binding: ItemStaffCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private fun calculateShiftDurationInHours(shiftStart: String, shiftEnd: String, breakHours: Double): Double {
            return try {
                val startParts = shiftStart.split(":").map { it.toInt() }
                val endParts = shiftEnd.split(":").map { it.toInt() }
                val startHour = startParts[0]
                val startMinute = startParts[1]
                val endHour = endParts[0]
                val endMinute = endParts[1]

                var durationInMinutes = (endHour - startHour) * 60 + (endMinute - startMinute)
                if (durationInMinutes < 0) { // Handles overnight shifts
                    durationInMinutes += 24 * 60
                }

                val durationInHours = durationInMinutes / 60.0
                (durationInHours - breakHours).coerceAtLeast(0.0)
            } catch (e: Exception) {
                0.0 // Return 0 if parsing fails
            }
        }

        fun bind(employee: Employee, stats: EmployeeStats?) {
            if (!employee.isActive) {
                binding.tvStaffName.text = "${employee.name} (Terminated)"
            } else {
                binding.tvStaffName.text = employee.name
            }
            binding.tvStaffRole.text = "${employee.salaryType} - ₹${employee.salaryRate}"

            val dailyShiftHours = calculateShiftDurationInHours(employee.shiftStart, employee.shiftEnd, employee.breakHours)

            val (perDaySalary, perHourSalary) = when (employee.salaryType) {
                "MONTHLY_FIXED" -> {
                    val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
                    val perDay = employee.salaryRate / daysInMonth
                    val perHour = if (dailyShiftHours > 0) perDay / dailyShiftHours else 0.0
                    perDay to perHour
                }
                "DAILY_FIXED" -> {
                    val perDay = employee.salaryRate
                    val perHour = if (dailyShiftHours > 0) perDay / dailyShiftHours else 0.0
                    perDay to perHour
                }
                "HOURLY" -> {
                    0.0 to employee.salaryRate
                }
                else -> 0.0 to 0.0
            }

            if (employee.salaryType == "HOURLY") {
                binding.tvTodayStats.text = "₹%.0f/hr".format(perHourSalary)
            } else {
                binding.tvTodayStats.text = "₹%.0f/day | ₹%.0f/hr".format(perDaySalary, perHourSalary)
            }

            binding.tvTotalHours.text = "%.1f hrs".format(stats?.totalHoursMonth ?: 0.0)
            binding.tvPendingAdvance.text = "₹ %.2f".format(stats?.pendingAdvance ?: 0.0)
            binding.tvCurrentSalary.text = "₹ %.2f".format(stats?.monthlySalary ?: 0.0)

            binding.btnAttendance.text = if (stats?.isCheckedIn == true) "CHECK OUT" else "ATTENDANCE"

            val daysPresent = stats?.daysPresent ?: 0
            val calendar = Calendar.getInstance()
            val hireDate = Calendar.getInstance().apply { timeInMillis = employee.hireDate }
            val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
            val hireDay = hireDate.get(Calendar.DAY_OF_YEAR)
            val daysSinceHire = if (calendar.get(Calendar.YEAR) == hireDate.get(Calendar.YEAR)) {
                currentDay - hireDay + 1
            } else {
                // This logic can be more complex if you need to handle multiple years
                currentDay
            }
            val absentDays = if (daysSinceHire > daysPresent) daysSinceHire - daysPresent else 0

            binding.tvDaysPresent.text = "$daysPresent"
            binding.tvDaysAbsent.text = "${if(absentDays < 0) 0 else absentDays}"
            binding.tvBonus.text = "₹ %.2f".format(stats?.bonusAmount ?: 0.0)

            binding.btnAttendance.setOnClickListener { onAttendanceClick(employee) }
            binding.btnAdvance.setOnClickListener { onAdvanceClick(employee) }

            binding.tvPendingAdvance.setOnClickListener { onAdvanceLogsClick(employee) }

            binding.btnPaySalary.setOnClickListener { onSalaryClick(employee) }
            binding.btnViewRecords.setOnClickListener { onViewLogsClick(employee) }

            binding.root.setOnClickListener { onEditClick(employee) }
            binding.root.setOnLongClickListener {
                onDeleteClick(employee)
                true
            }
        }
    }

    class EmployeeDiffCallback : DiffUtil.ItemCallback<Employee>() {
        override fun areItemsTheSame(oldItem: Employee, newItem: Employee) = oldItem.employeeId == newItem.employeeId
        override fun areContentsTheSame(oldItem: Employee, newItem: Employee) = oldItem == newItem
    }
}
