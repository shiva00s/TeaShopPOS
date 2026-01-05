package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class StaffViewModel(private val repository: MainRepository) : ViewModel() {

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private var shopId: String? = null

    // Real-time salary and hours state
    private val _employeeStats = MutableStateFlow<Map<String, EmployeeStats>>(emptyMap())
    val employeeStats = _employeeStats.asStateFlow()

    fun setShop(shopId: String) {
        this.shopId = shopId
        viewModelScope.launch {
            repository.getShopEmployees(shopId).collect { employeeList ->
                _employees.value = employeeList
                employeeList.forEach { loadStats(it) }
            }
        }
    }

    private fun loadStats(employee: Employee) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()

        viewModelScope.launch {
            val attendanceList = repository.getAttendanceForPeriod(employee.employeeId, start, end)
            val totalHours = attendanceList.sumOf { it.hoursWorked }
            val pendingAdvance = repository.getPendingAdvance(employee.employeeId) ?: 0.0
            
            val hourlyRate = if (employee.salaryType == "PER_HOUR") employee.salaryRate else (employee.salaryRate / 8)
            val currentSalary = (totalHours * hourlyRate) - pendingAdvance

            val stats = EmployeeStats(totalHours, currentSalary, pendingAdvance)
            val currentMap = _employeeStats.value.toMutableMap()
            currentMap[employee.employeeId] = stats
            _employeeStats.value = currentMap
        }
    }

    fun addManualAttendance(employee: Employee, inTime: Long, outTime: Long, isGap: Boolean) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val totalMillis = outTime - inTime
            var hours = totalMillis / (1000.0 * 60 * 60)
            if (isGap) hours = -hours

            repository.insertAttendance(Attendance(
                attendanceId = UUID.randomUUID().toString(),
                employeeId = employee.employeeId,
                shopId = sId,
                checkInTime = inTime,
                checkOutTime = outTime,
                hoursWorked = hours,
                type = if (isGap) "GAP" else "WORK"
            ))
            loadStats(employee)
        }
    }

    fun toggleAttendance(employee: Employee) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val last = repository.getLastAttendance(employee.employeeId)
            if (last == null || last.checkOutTime != null) {
                repository.insertAttendance(Attendance(
                    attendanceId = UUID.randomUUID().toString(),
                    employeeId = employee.employeeId,
                    shopId = sId,
                    checkInTime = System.currentTimeMillis()
                ))
            } else {
                val now = System.currentTimeMillis()
                val hours = (now - last.checkInTime) / (1000.0 * 60 * 60)
                repository.updateAttendance(last.copy(checkOutTime = now, hoursWorked = hours))
            }
            loadStats(employee)
        }
    }

    fun giveAdvance(employeeId: String, amount: Double) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val advanceId = UUID.randomUUID().toString()
            repository.insertAdvance(AdvancePayment(advanceId, employeeId, sId, amount))
            repository.insertCashbookEntry(Cashbook(UUID.randomUUID().toString(), sId, "OUT", "ADVANCE", amount, "Advance to staff", advanceId))
            _employees.value.find { it.employeeId == employeeId }?.let { loadStats(it) }
        }
    }

    fun calculateAndPaySalary(employee: Employee, startTime: Long, endTime: Long) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val records = repository.getAttendanceForPeriod(employee.employeeId, startTime, endTime)
            val totalHours = records.sumOf { it.hoursWorked }
            val pendingAdvance = repository.getPendingAdvance(employee.employeeId) ?: 0.0
            
            val hourlyRate = if (employee.salaryType == "PER_HOUR") employee.salaryRate else (employee.salaryRate / 8)
            val netPayable = (totalHours * hourlyRate) - pendingAdvance

            val paymentId = UUID.randomUUID().toString()
            repository.processSalary(
                SalaryPayment(paymentId, employee.employeeId, sId, totalHours * hourlyRate, 0.0, netPayable, startTime, endTime),
                Cashbook(UUID.randomUUID().toString(), sId, "OUT", "SALARY", netPayable, "Salary: ${employee.name}", paymentId),
                pendingAdvance > 0
            )
            loadStats(employee)
        }
    }

    fun addEmployee(name: String, phone: String, rate: Double, type: String, shiftStart: String, shiftEnd: String, otRate: Double) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val newEmployee = Employee(
                employeeId = UUID.randomUUID().toString(),
                shopId = sId,
                name = name,
                phone = phone,
                salaryRate = rate,
                salaryType = type,
                shiftStart = shiftStart,
                shiftEnd = shiftEnd,
                otRateMultiplier = otRate
            )
            repository.insertEmployee(newEmployee)
        }
    }

    fun getAttendanceRecords(employeeId: String, start: Long, end: Long) = repository.getAttendanceForPeriodFlow(employeeId, start, end)

    private fun MainRepository.getAttendanceForPeriodFlow(employeeId: String, start: Long, end: Long) = staffDao.getAttendanceFlow(employeeId, start, end)
}

data class EmployeeStats(
    val totalHours: Double,
    val currentSalary: Double,
    val pendingAdvance: Double
)
