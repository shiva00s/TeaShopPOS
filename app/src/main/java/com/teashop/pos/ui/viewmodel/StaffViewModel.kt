package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class StaffViewModel(private val repository: MainRepository) : ViewModel() {

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private var shopId: String? = null

    fun setShop(shopId: String) {
        this.shopId = shopId
        viewModelScope.launch {
            repository.getShopEmployees(shopId).collect {
                _employees.value = it
            }
        }
    }

    fun addEmployee(name: String, salaryRate: Double, type: String, start: String, end: String, otRate: Double) {
        val sId = shopId ?: return
        viewModelScope.launch {
            repository.insertEmployee(Employee(
                employeeId = UUID.randomUUID().toString(),
                shopId = sId,
                name = name,
                phone = "",
                salaryType = type,
                salaryRate = salaryRate,
                shiftStart = start,
                shiftEnd = end,
                otRateMultiplier = otRate
            ))
        }
    }

    fun addManualAttendance(employee: Employee, inTime: Long, outTime: Long, isGap: Boolean) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val totalMillis = outTime - inTime
            var hours = totalMillis / (1000.0 * 60 * 60)
            
            // If it's a gap, the worked hours are negative (deduction)
            if (isGap) {
                hours = -hours
            }

            // Simple late/OT logic for manual entries can be added here if needed
            repository.insertAttendance(Attendance(
                attendanceId = UUID.randomUUID().toString(),
                employeeId = employee.employeeId,
                shopId = sId,
                checkInTime = inTime,
                checkOutTime = outTime,
                hoursWorked = hours,
                type = if (isGap) "GAP" else "WORK"
            ))
        }
    }

    fun toggleAttendance(employee: Employee) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val last = repository.getLastAttendance(employee.employeeId)
            
            if (last == null || last.checkOutTime != null) {
                val now = Calendar.getInstance()
                repository.insertAttendance(Attendance(
                    attendanceId = UUID.randomUUID().toString(),
                    employeeId = employee.employeeId,
                    shopId = sId,
                    checkInTime = now.timeInMillis
                ))
            } else {
                val now = Calendar.getInstance()
                val totalMillis = now.timeInMillis - last.checkInTime
                val hours = totalMillis / (1000.0 * 60 * 60)
                repository.updateAttendance(last.copy(
                    checkOutTime = now.timeInMillis,
                    hoursWorked = hours
                ))
            }
        }
    }

    fun calculateAndPaySalary(employee: Employee, startTime: Long, endTime: Long) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val records = repository.getAttendanceForPeriod(employee.employeeId, startTime, endTime)
            val totalHours = records.sumOf { it.hoursWorked }
            val totalDeductions = records.sumOf { it.lateDeduction }
            
            val hourlyRate = if (employee.salaryType == "PER_HOUR") employee.salaryRate else (employee.salaryRate / 8)
            val netPayable = (totalHours * hourlyRate) - totalDeductions

            val paymentId = UUID.randomUUID().toString()
            repository.processSalary(
                SalaryPayment(paymentId, employee.employeeId, sId, totalHours * hourlyRate, totalDeductions, netPayable, startTime, endTime),
                Cashbook(UUID.randomUUID().toString(), sId, "OUT", "SALARY", netPayable, "Salary: ${employee.name} (${String.format("%.1f", totalHours)}h)", paymentId),
                false
            )
        }
    }

    fun giveAdvance(employeeId: String, amount: Double) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val advanceId = UUID.randomUUID().toString()
            repository.insertAdvance(AdvancePayment(advanceId, employeeId, sId, amount))
            repository.insertCashbookEntry(Cashbook(UUID.randomUUID().toString(), sId, "OUT", "ADVANCE", amount, "Advance to staff", advanceId))
        }
    }
}
