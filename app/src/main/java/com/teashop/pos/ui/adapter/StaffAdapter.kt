package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ItemStaffCardBinding
import com.teashop.pos.ui.viewmodel.EmployeeStats

class StaffAdapter(
    private val onAttendanceClick: (Employee) -> Unit,
    private val onAdvanceClick: (Employee) -> Unit,
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
        fun bind(employee: Employee, stats: EmployeeStats?) {
            binding.tvStaffName.text = employee.name
            binding.tvStaffRole.text = "${employee.salaryType} - ₹${employee.salaryRate}"
            
            // Bind Real-time Stats
            binding.tvTotalHours.text = "%.1f hrs".format(stats?.totalHours ?: 0.0)
            binding.tvPendingAdvance.text = "₹ %.2f".format(stats?.pendingAdvance ?: 0.0)
            binding.tvCurrentSalary.text = "₹ %.2f".format(stats?.currentSalary ?: 0.0)

            binding.btnAttendance.setOnClickListener { onAttendanceClick(employee) }
            binding.btnAdvance.setOnClickListener { onAdvanceClick(employee) }
            binding.btnPaySalary.setOnClickListener { onSalaryClick(employee) }
            binding.btnViewRecords.setOnClickListener { onViewLogsClick(employee) }
            binding.btnEdit.setOnClickListener { onEditClick(employee) }
            binding.btnDelete.setOnClickListener { onDeleteClick(employee) }
        }
    }

    class EmployeeDiffCallback : DiffUtil.ItemCallback<Employee>() {
        override fun areItemsTheSame(oldItem: Employee, newItem: Employee) = oldItem.employeeId == newItem.employeeId
        override fun areContentsTheSame(oldItem: Employee, newItem: Employee) = oldItem == newItem
    }
}
