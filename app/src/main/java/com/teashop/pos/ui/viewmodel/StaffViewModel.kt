package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class EmployeeStats(
    val totalHoursMonth: Double,
    val monthlySalary: Double,
    val todayHours: Double,
    val todaySalary: Double,
    val pendingAdvance: Double,
    val isCheckedIn: Boolean = false,
    val bonusAmount: Double = 0.0,
    val daysPresent: Int = 0,
    val versionCount: Int = 0
)

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val repository: MainRepository,
    private val sharedViewModel: SharedViewModel
) : ViewModel() {

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private var shopId: String? = null

    private val _employeeStats = MutableStateFlow<Map<String, EmployeeStats>>(emptyMap())
    val employeeStats = _employeeStats.asStateFlow()

    private val _selectedEmployee = MutableStateFlow<Employee?>(null)
    val selectedEmployee = _selectedEmployee.asStateFlow()

    init {
        startRealTimeTicker()
    }

    fun setShop(shopId: String) {
        this.shopId = shopId
        viewModelScope.launch {
            val employeeFlow = if (shopId.isNotBlank()) {
                repository.getShopEmployees(shopId)
            } else {
                repository.getAllEmployees()
            }
            employeeFlow.collect { employeeList ->
                _employees.value = employeeList
                employeeList.forEach { loadStats(it) }
            }
        }
    }

    private fun startRealTimeTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // Updates every second for real-time tracking
                val updatedEmployees = _employees.value.mapNotNull { employee ->
                    val stats = _employeeStats.value[employee.employeeId]
                    if (stats?.isCheckedIn == true) employee else null
                }
                if (updatedEmployees.isNotEmpty()) {
                    updatedEmployees.forEach { loadStats(it) }
                }
            }
        }
    }


    private fun loadStats(employee: Employee) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { time = Date(now) }

        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val normalizedHireDate = Calendar.getInstance().apply {
            timeInMillis = employee.hireDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModelScope.launch {
            val calcStart = maxOf(monthStart, normalizedHireDate)
            val calcEnd = employee.terminateDate?.let { minOf(now, it) } ?: now

            if ((employee.terminateDate != null && employee.terminateDate!! < monthStart) || calcStart > calcEnd) {
                repository.getPendingAdvance(employee.employeeId).collect { pendingAdvance ->
                    updateStats(employee.employeeId, EmployeeStats(0.0, 0.0, 0.0, 0.0, pendingAdvance))
                }
                return@launch
            }

            combine(
                repository.getAttendanceFlow(employee.employeeId, 0, Long.MAX_VALUE), // Query all and filter locally
                repository.getPendingAdvance(employee.employeeId),
                repository.getHistoryCount(employee.employeeId)
            ) { allAttendance, pendingAdvance, versionCount ->

                val recordsInWindow = allAttendance.filter { it.checkInTime in calcStart..calcEnd }

                var totalHoursMonth = 0.0
                var todayHours = 0.0
                var isCurrentlyIn = false

                val daysWorkedSet = mutableSetOf<String>()
                val dailyShiftHours = calculateShiftDurationInHours(employee.shiftStart, employee.shiftEnd)

                recordsInWindow.forEach { att ->
                    val attendanceDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(att.checkInTime))
                    @Suppress("DEPRECATION")
                    val attendanceType = att.attendanceType 

                    when (attendanceType) {
                        AttendanceType.WORK -> {
                            daysWorkedSet.add(attendanceDate)
                            val checkOutTime = att.checkOutTime
                            val duration = when {
                                checkOutTime != null -> (checkOutTime - att.checkInTime) / 3600000.0
                                else -> {
                                    isCurrentlyIn = true
                                    (now - att.checkInTime) / 3600000.0
                                }
                            }
                            totalHoursMonth += duration
                            if (att.checkInTime >= todayStart) {
                                todayHours += duration
                            }
                        }
                        AttendanceType.SHOP_CLOSED_PAID -> {
                            daysWorkedSet.add(attendanceDate)
                            val shiftHours = dailyShiftHours
                            totalHoursMonth += shiftHours
                            if (att.checkInTime >= todayStart) {
                                todayHours += shiftHours
                            }
                        }
                        AttendanceType.SHOP_CLOSED_UNPAID, AttendanceType.GAP -> {
                            // Do nothing for unpaid leave or gaps
                        }
                    }
                }
                
                val totalDeductedBreakHours = daysWorkedSet.size * employee.breakHours
                totalHoursMonth -= totalDeductedBreakHours

                val todayWorkedOrPaidLeave = recordsInWindow.any {
                    @Suppress("DEPRECATION")
                    it.checkInTime >= todayStart && (it.attendanceType == AttendanceType.WORK || it.attendanceType == AttendanceType.SHOP_CLOSED_PAID)
                }
                if (todayWorkedOrPaidLeave) {
                    todayHours -= employee.breakHours
                }

                val (totalSalarySum, todaySalary) = calculateSalaryFromHours(
                    employee, totalHoursMonth, todayHours, dailyShiftHours
                )

                val daysPresentCount = daysWorkedSet.size
                var bonus = 0.0
                val calendar = Calendar.getInstance()
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                if (daysPresentCount >= currentDay && employee.isActive) {
                    bonus = if (employee.salaryType == "MONTHLY_FIXED") employee.salaryRate / 30.0 else (employee.salaryRate * 8.0)
                }

                EmployeeStats(
                    totalHoursMonth = totalHoursMonth.coerceAtLeast(0.0),
                    monthlySalary = (totalSalarySum + bonus) - pendingAdvance,
                    todayHours = todayHours.coerceAtLeast(0.0),
                    todaySalary = todaySalary,
                    pendingAdvance = pendingAdvance,
                    isCheckedIn = isCurrentlyIn,
                    bonusAmount = bonus,
                    daysPresent = daysPresentCount,
                    versionCount = versionCount
                )
            }.collect { stats ->
                updateStats(employee.employeeId, stats)
            }
        }
    }

    private fun calculateShiftDurationInHours(shiftStart: String, shiftEnd: String): Double {
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
            durationInMinutes / 60.0
        } catch (e: Exception) {
            0.0 // Return 0 if parsing fails
        }
    }

    private fun calculateSalaryFromHours(employee: Employee, totalHours: Double, todayHours: Double, dailyShiftHours: Double): Pair<Double, Double> {
        val effectiveShiftHours = dailyShiftHours - employee.breakHours
        val hourlyRate = when (employee.salaryType) {
            "MONTHLY_FIXED" -> {
                val perDay = employee.salaryRate / 30
                if (effectiveShiftHours > 0) perDay / effectiveShiftHours else 0.0
            }
            "DAILY_FIXED" -> {
                if (effectiveShiftHours > 0) employee.salaryRate / effectiveShiftHours else 0.0
            }
            "HOURLY" -> employee.salaryRate
            else -> 0.0
        }

        val totalSalary = totalHours * hourlyRate
        val todaySalary = todayHours * hourlyRate

        return Pair(totalSalary.coerceAtLeast(0.0), todaySalary.coerceAtLeast(0.0))
    }


    private fun updateStats(empId: String, stats: EmployeeStats) {
        val currentMap = _employeeStats.value.toMutableMap()
        currentMap[empId] = stats
        _employeeStats.value = currentMap
    }

    fun addShopClosedDay(date: Date, isPaid: Boolean) {
        val sId = shopId ?: return
        viewModelScope.launch {
            _employees.value.forEach { employee ->
                if (employee.isActive) {
                    val attendanceType = if (isPaid) AttendanceType.SHOP_CLOSED_PAID else AttendanceType.SHOP_CLOSED_UNPAID
                    val calendar = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val checkInTime = calendar.timeInMillis

                    val attendance = Attendance(
                        attendanceId = UUID.randomUUID().toString(),
                        employeeId = employee.employeeId,
                        shopId = sId,
                        checkInTime = checkInTime,
                        checkOutTime = null,
                        attendanceType = attendanceType,
                        type = attendanceType.name,
                        shiftStart = employee.shiftStart,
                        shiftEnd = employee.shiftEnd,
                        breakHours = employee.breakHours,
                        salaryType = employee.salaryType,
                        salaryRate = employee.salaryRate
                    )
                    repository.insertAttendance(attendance)
                }
            }
            recalculateShopProfit(sId) // Recalculate profit after adding attendance
        }
    }

    fun deleteClosedDay(day: ShopClosedDay) {
        viewModelScope.launch {
            repository.deleteClosedDay(day)
        }
    }

    fun getClosedDays(shopId: String, start: Long, end: Long) = repository.getClosedDays(shopId, start, end)

    fun addEmployee(name: String, phone: String, salaryRate: Double, type: String, start: String, end: String, otRate: Double, breakHours: Double = 0.0, hireDate: Long = System.currentTimeMillis()) {
        val sId = shopId ?: return
        viewModelScope.launch {
            repository.insertEmployee(Employee(
                employeeId = UUID.randomUUID().toString(),
                shopId = sId,
                name = name,
                phone = phone,
                salaryType = type,
                salaryRate = salaryRate,
                shiftStart = start,
                shiftEnd = end,
                breakHours = breakHours,
                otRateMultiplier = otRate,
                hireDate = hireDate
            ))
        }
    }

    fun loadEmployee(id: String) {
        viewModelScope.launch {
            repository.getEmployee(id).collect {
                _selectedEmployee.value = it
            }
        }
    }

    fun updateEmployee(employee: Employee, name: String, phone: String, salaryRate: Double, type: String, start: String, end: String, otRate: Double, breakHours: Double, hireDate: Long, terminateDate: Long?) {
        viewModelScope.launch {
            if (employee.salaryRate != salaryRate || employee.salaryType != type ||
                employee.shiftStart != start || employee.shiftEnd != end || employee.breakHours != breakHours) {

                repository.getHistoryCount(employee.employeeId).firstOrNull()?.let { count ->
                    repository.insertHistory(EmployeeHistory(
                        historyId = UUID.randomUUID().toString(),
                        employeeId = employee.employeeId,
                        version = count + 1,
                        salaryType = employee.salaryType,
                        salaryRate = employee.salaryRate,
                        shiftStart = employee.shiftStart,
                        shiftEnd = employee.shiftEnd,
                        breakHours = employee.breakHours,
                        changeDate = System.currentTimeMillis()
                    ))
                }
            }

            repository.updateEmployee(employee.copy(
                name = name,
                phone = phone,
                salaryRate = salaryRate,
                salaryType = type,
                shiftStart = start,
                shiftEnd = end,
                breakHours = breakHours,
                otRateMultiplier = otRate,
                hireDate = hireDate,
                terminateDate = terminateDate,
                isActive = terminateDate == null
            ))
        }
    }

    fun deleteEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
        }
    }

    fun markAttendance(employee: Employee, date: Date) {
        viewModelScope.launch {
            val todayStart = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val records = repository.getAttendanceFlow(employee.employeeId, todayStart, todayStart + 86400000).first()
            @Suppress("DEPRECATION")
            val lastRecord = records.filter { it.attendanceType == AttendanceType.WORK }.maxByOrNull { it.checkInTime }

            if (lastRecord != null && lastRecord.checkOutTime == null) {
                val updatedRecord = lastRecord.copy(checkOutTime = date.time)
                repository.updateAttendance(updatedRecord)
            } else {
                repository.insertAttendance(Attendance(
                    attendanceId = UUID.randomUUID().toString(),
                    employeeId = employee.employeeId,
                    shopId = employee.shopId,
                    checkInTime = date.time,
                    attendanceType = AttendanceType.WORK,
                    type = "WORK",
                    shiftStart = employee.shiftStart,
                    shiftEnd = employee.shiftEnd,
                    breakHours = employee.breakHours,
                    salaryType = employee.salaryType,
                    salaryRate = employee.salaryRate
                ))
            }
            recalculateShopProfit(employee.shopId)
        }
    }

    fun addManualAttendanceEntry(employee: Employee, inTime: Long, outTime: Long, isGap: Boolean) {
        val sId = shopId ?: return
        if (inTime < employee.hireDate || (employee.terminateDate != null && inTime > employee.terminateDate!!)) return

        viewModelScope.launch {
            val attendanceType = if (isGap) AttendanceType.GAP else AttendanceType.WORK
            repository.insertAttendance(Attendance(
                attendanceId = UUID.randomUUID().toString(),
                employeeId = employee.employeeId,
                shopId = sId,
                checkInTime = inTime,
                checkOutTime = if (outTime > 0) outTime else null,
                attendanceType = attendanceType,
                type = attendanceType.name,
                shiftStart = employee.shiftStart,
                shiftEnd = employee.shiftEnd,
                breakHours = employee.breakHours,
                salaryType = employee.salaryType,
                salaryRate = employee.salaryRate
            ))
            recalculateShopProfit(sId)
        }
    }

    fun updateAttendance(attendance: Attendance) {
        viewModelScope.launch {
            @Suppress("DEPRECATION")
            repository.updateAttendance(attendance.copy(type = attendance.attendanceType.name))
            recalculateShopProfit(attendance.shopId)
        }
    }

    fun deleteAttendance(attendance: Attendance) {
        viewModelScope.launch {
            repository.deleteAttendance(attendance)
            recalculateShopProfit(attendance.shopId)
        }
    }

    fun getAdvanceRecords(employeeId: String, start: Long, end: Long): Flow<List<AdvancePayment>> {
        return repository.getAdvanceRecords(employeeId, start, end)
    }

    fun giveAdvance(employeeId: String, amount: Double, date: Date = Date()) {
        val sId = shopId ?: return
        viewModelScope.launch {
            val advanceId = UUID.randomUUID().toString()
            repository.insertAdvance(AdvancePayment(advanceId, employeeId, sId, amount, date.time))
            repository.insertCashbookEntry(Cashbook(UUID.randomUUID().toString(), sId, "OUT", "ADVANCE", amount, "Advance to staff", advanceId))
            recalculateShopProfit(sId)
        }
    }

    fun updateAdvance(advance: AdvancePayment) {
        viewModelScope.launch {
            repository.updateAdvance(advance)
            recalculateShopProfit(advance.shopId)
        }
    }

    fun deleteAdvance(advance: AdvancePayment) {
        viewModelScope.launch {
            repository.deleteAdvance(advance)
            repository.deleteCashbookEntryByReference(advance.advanceId)
            recalculateShopProfit(advance.shopId)
        }
    }


    fun calculateAndPaySalary(employee: Employee, startTime: Long, endTime: Long) {
        val sId = shopId ?: return
        val payStart = maxOf(startTime, employee.hireDate)
        val payEnd = if (employee.terminateDate != null) minOf(endTime, employee.terminateDate!!) else endTime
        if (payStart > payEnd) return

        viewModelScope.launch {
            combine(
                repository.getAttendanceFlow(employee.employeeId, payStart, payEnd),
                repository.getPendingAdvance(employee.employeeId)
            ) { records, pendingAdvance ->
                var totalHours = 0.0
                val daysWorkedSet = mutableSetOf<String>()

                records.forEach { att ->
                    @Suppress("DEPRECATION")
                    if (att.attendanceType == AttendanceType.WORK) {
                        daysWorkedSet.add(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(att.checkInTime)))
                        val duration = (att.checkOutTime ?: payEnd) - att.checkInTime
                        totalHours += duration / 3600000.0
                    }
                }

                val dailyShiftHours = calculateShiftDurationInHours(employee.shiftStart, employee.shiftEnd)
                val totalDeductedBreakHours = daysWorkedSet.size * employee.breakHours
                totalHours -= totalDeductedBreakHours

                val (totalPay, _) = calculateSalaryFromHours(employee, totalHours, 0.0, dailyShiftHours)

                Pair(totalPay, pendingAdvance)
            }.first().let { (totalPay, pendingAdvance) ->
                val netPayable = totalPay - pendingAdvance
                val paymentId = UUID.randomUUID().toString()
                repository.insertSalaryPayment(SalaryPayment(paymentId, employee.employeeId, sId, totalPay, 0.0, netPayable, payStart, payEnd))
                repository.insertCashbookEntry(Cashbook(UUID.randomUUID().toString(), sId, "OUT", "SALARY", netPayable, "Salary: ${employee.name}", paymentId))
                recalculateShopProfit(sId)
            }
        }
    }


    fun getAttendanceRecords(employeeId: String, start: Long, end: Long) = repository.getAttendanceFlow(employeeId, start, end)

    private fun recalculateShopProfit(shopId: String) {
        viewModelScope.launch {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            repository.getCashFlowSummary(shopId, start, end).collect { summary ->
                if (summary != null) {
                    val profit = summary.totalIn - summary.totalOut
                    sharedViewModel.updateShopProfit(shopId, profit)
                }
            }
        }
    }
}
