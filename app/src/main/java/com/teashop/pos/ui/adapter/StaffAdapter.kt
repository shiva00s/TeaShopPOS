package com.teashop.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ItemStaffCardBinding

class StaffAdapter(
    private val onAttendanceClick: (Employee) -> Unit,
    private val onAdvanceClick: (Employee) -> Unit,
    private val onSalaryClick: (Employee) -> Unit
) : ListAdapter<Employee, StaffAdapter.StaffViewHolder>(EmployeeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemStaffCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StaffViewHolder(private val binding: ItemStaffCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(employee: Employee) {
            binding.tvStaffName.text = employee.name
            binding.tvStaffRole.text = "${employee.salaryType} • ₹${employee.salaryRate}"
            
            binding.btnAttendance.setOnClickListener { onAttendanceClick(employee) }
            binding.btnAdvance.setOnClickListener { onAdvanceClick(employee) }
            binding.btnPaySalary.setOnClickListener { onSalaryClick(employee) }
        }
    }

    class EmployeeDiffCallback : DiffUtil.ItemCallback<Employee>() {
        override fun areItemsTheSame(oldItem: Employee, newItem: Employee) = oldItem.employeeId == newItem.employeeId
        override fun areContentsTheSame(oldItem: Employee, newItem: Employee) = oldItem == newItem
    }
}
