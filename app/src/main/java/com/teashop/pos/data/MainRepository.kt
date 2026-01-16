package com.teashop.pos.data

import com.teashop.pos.data.entity.*
import com.teashop.pos.sync.FirebaseSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainRepository(
    private val firebaseSync: FirebaseSyncManager
) {

    // ---------------- USER PROFILE ----------------
    fun getProfileFlow(): Flow<UserProfile?> = firebaseSync.getItemFlow("user_profiles", "current_user") // Simplified
    fun updateProfile(profile: UserProfile) = firebaseSync.pushProfile(profile)

    // ---------------- SHOP ----------------
    fun getAllShops(): Flow<List<Shop>> = firebaseSync.getDataFlow("shops")
    fun insertShop(shop: Shop) = firebaseSync.pushShop(shop)
    fun updateShop(shop: Shop) = firebaseSync.pushShop(shop)
    fun deleteShop(shop: Shop) = firebaseSync.deleteShop(shop.shopId)

    // ---------------- MENU & ITEMS ----------------
    fun getActiveItems(): Flow<List<Item>> = firebaseSync.getDataFlow<Item>("items")
        .map { items -> items.filter { it.isActive } }

    fun getItem(itemId: String): Flow<Item?> = firebaseSync.getItemFlow("items", itemId)

    fun getShopMenu(shopId: String): Flow<List<ShopMenuItem>> {
        return combine(
            firebaseSync.getDataFlow<Item>("items"),
            firebaseSync.getDataFlow<ShopItemPrice>("prices"),
            firebaseSync.getDataFlow<OrderItem>("order_items")
        ) { items, prices, orderItems ->
            items.filter { it.isActive }.map { item ->
                val price = prices.find { it.shopId == shopId && it.itemId == item.itemId }?.sellingPrice ?: item.globalPrice
                val sales = orderItems.count { it.itemId == item.itemId }
                ShopMenuItem(item, price, sales)
            }.sortedByDescending { it.salesCount }
        }
    }

    fun insertItem(item: Item) = firebaseSync.pushItem(item)
    fun updateItem(item: Item) = firebaseSync.pushItem(item)
    fun deleteItem(item: Item) = firebaseSync.deleteItem(item.itemId)
    fun updateShopItemPrice(price: ShopItemPrice) = firebaseSync.pushPrice(price)

    // ---------------- ORDERS ----------------
    fun getHeldOrders(shopId: String): Flow<List<Order>> = firebaseSync.getDataFlow<Order>("orders")
        .map { orders -> orders.filter { it.shopId == shopId && it.status == "HOLD" } }

    fun getOrderWithItems(orderId: String): Flow<OrderWithItems> {
        return combine(
            firebaseSync.getItemFlow<Order>("orders", orderId),
            firebaseSync.getDataFlow<OrderItem>("order_items")
        ) { order, items ->
            OrderWithItems(order ?: Order(orderId = orderId), items.filter { it.orderId == orderId })
        }
    }

    fun getHeldOrdersWithItems(shopId: String): Flow<List<OrderWithItems>> {
        return getHeldOrders(shopId).flatMapLatest { orders ->
            val orderFlows = orders.map { order ->
                getOrderWithItems(order.orderId)
            }
            if (orderFlows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(orderFlows) { it.toList() }
            }
        }
    }

    fun getBilledOrdersWithItems(shopId: String, startTime: Long, endTime: Long): Flow<List<OrderWithItems>> {
        return firebaseSync.getDataFlow<Order>("orders")
            .map { orders ->
                orders.filter { it.shopId == shopId && it.status == "CLOSED" && it.closedAt in startTime..endTime }
                    .sortedByDescending { it.closedAt }
            }
            .flatMapLatest { orders ->
                val orderFlows = orders.map { order ->
                    getOrderWithItems(order.orderId)
                }
                if (orderFlows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(orderFlows) { it.toList() }
                }
            }
    }

    fun getOrdersWithItems(orderIds: List<String>): Flow<List<OrderWithItems>> {
        return firebaseSync.getDataFlow<Order>("orders").map { orders ->
            orders.filter { it.orderId in orderIds }
        }.flatMapLatest { orders ->
            val orderFlows = orders.map { order ->
                getOrderWithItems(order.orderId)
            }
            combine(orderFlows) { it.toList() }
        }
    }

    fun getOrdersWithItemsForPeriod(shopId: String, start: Long, end: Long): Flow<List<OrderWithItems>> {
        return firebaseSync.getDataFlow<Order>("orders").map { orders ->
            orders.filter { it.shopId == shopId && it.closedAt in start..end }
        }.flatMapLatest { orders ->
            val orderFlows = orders.map { order ->
                getOrderWithItems(order.orderId)
            }
            combine(orderFlows) { it.toList() }
        }
    }

    fun insertOrder(order: Order) = firebaseSync.pushOrder(order)
    fun updateOrder(order: Order) = firebaseSync.pushOrder(order)
    suspend fun deleteOrder(order: Order) {
        getOrderWithItems(order.orderId).first().items.forEach {
            firebaseSync.deleteOrderItem(it.orderItemId)
        }
        firebaseSync.deleteOrder(order.orderId)
    }
    fun insertOrderItem(item: OrderItem) = firebaseSync.pushOrderItem(item)

    // ---------------- CASHBOOK ----------------
    fun insertCashbookEntry(entry: Cashbook) = firebaseSync.pushCashbookEntry(entry)
    fun deleteCashbookEntry(entry: Cashbook) = firebaseSync.deleteCashbookEntry(entry.entryId)
    fun deleteCashbookEntryByReference(referenceId: String) = firebaseSync.deleteCashbookEntryByReference(referenceId)
    fun getCashbookEntriesForPeriod(shopId: String?, start: Long, end: Long): Flow<List<Cashbook>> =
        firebaseSync.getDataFlow<Cashbook>("cashbook").map { entries ->
            entries.filter { (shopId.isNullOrEmpty() || it.shopId == shopId) && it.transactionDate in start..end }
                .sortedByDescending { it.transactionDate }
        }

    // ---------------- STAFF ----------------
    fun getShopEmployees(shopId: String): Flow<List<Employee>> =
        combine(
            firebaseSync.getDataFlow<Employee>("employees"),
            firebaseSync.getDataFlow<Attendance>("attendance")
        ) { employees, attendance ->
            val lastAttendance = attendance.groupBy { it.employeeId }
                .mapValues { (_, records) ->
                    records.maxOfOrNull { it.checkInTime }
                }

            employees.filter { it.shopId == shopId }
                .map { it.apply { this.lastActive = lastAttendance[this.employeeId] } }
                .sortedByDescending { it.lastActive }
        }


    fun getAllEmployees(): Flow<List<Employee>> =
        combine(
            firebaseSync.getDataFlow<Employee>("employees"),
            firebaseSync.getDataFlow<Attendance>("attendance")
        ) { employees, attendance ->
            val lastAttendance = attendance.groupBy { it.employeeId }
                .mapValues { (_, records) ->
                    records.maxOfOrNull { it.checkInTime }
                }

            employees.map { it.apply { this.lastActive = lastAttendance[this.employeeId] } }
                .sortedByDescending { it.lastActive }
        }

    fun getEmployee(employeeId: String): Flow<Employee?> = firebaseSync.getItemFlow("employees", employeeId)
    fun insertEmployee(employee: Employee) = firebaseSync.pushEmployee(employee)
    fun updateEmployee(employee: Employee) = firebaseSync.pushEmployee(employee)
    fun deleteEmployee(employee: Employee) = firebaseSync.deleteEmployee(employee.employeeId)

    fun getHistoryCount(employeeId: String): Flow<Int> = firebaseSync.getDataFlow<EmployeeHistory>("employee_history")
        .map { history -> history.count { it.employeeId == employeeId } }

    fun insertHistory(history: EmployeeHistory) = firebaseSync.pushHistory(history)

    // ---------------- ATTENDANCE & PAYMENTS ----------------
    fun getAttendanceFlow(employeeId: String, start: Long, end: Long): Flow<List<Attendance>> =
        firebaseSync.getDataFlow<Attendance>("attendance").map { records ->
            records.filter { it.employeeId == employeeId && it.checkInTime in start..end }
                .sortedByDescending { it.checkInTime }
        }

    fun insertAttendance(att: Attendance) = firebaseSync.pushAttendance(att)
    fun updateAttendance(att: Attendance) = firebaseSync.pushAttendance(att)
    fun deleteAttendance(att: Attendance) = firebaseSync.deleteAttendance(att.attendanceId)

    fun getPendingAdvance(employeeId: String): Flow<Double> = firebaseSync.getDataFlow<AdvancePayment>("advance_payments")
        .map { advances -> advances.filter { it.employeeId == employeeId && !it.isRecovered }.sumOf { it.amount } }

    fun getAdvanceRecords(employeeId: String, start: Long, end: Long): Flow<List<AdvancePayment>> =
        firebaseSync.getDataFlow<AdvancePayment>("advance_payments").map { advances ->
            advances.filter { it.employeeId == employeeId && it.date in start..end }
                .sortedByDescending { it.date }
        }

    fun insertAdvance(adv: AdvancePayment) = firebaseSync.pushAdvance(adv)
    fun updateAdvance(adv: AdvancePayment) = firebaseSync.pushAdvance(adv)
    fun deleteAdvance(adv: AdvancePayment) = firebaseSync.deleteAdvance(adv.advanceId)

    fun insertSalaryPayment(payment: SalaryPayment) = firebaseSync.pushSalaryPayment(payment)
    fun getClosedDays(shopId: String?, start: Long, end: Long): Flow<List<ShopClosedDay>> =
        firebaseSync.getDataFlow<ShopClosedDay>("shop_closed_days").map { days ->
            days.filter { (shopId.isNullOrEmpty() || it.shopId == shopId) && it.date in start..end }
        }

    fun insertClosedDay(day: ShopClosedDay) = firebaseSync.pushClosedDay(day)
    fun deleteClosedDay(day: ShopClosedDay) = firebaseSync.deleteClosedDay(day.id)

    // ---------------- FIXED EXPENSES ----------------
    fun getFixedExpensesForShop(shopId: String): Flow<List<FixedExpense>> = firebaseSync.getDataFlow<FixedExpense>("fixed_expenses")
        .map { expenses -> expenses.filter { it.shopId == shopId } }

    fun getAllFixedExpenses(): Flow<List<FixedExpense>> = firebaseSync.getDataFlow("fixed_expenses")

    fun insertFixedExpense(expense: FixedExpense) = firebaseSync.pushFixedExpense(expense)

    // ---------------- REPORTS ----------------
    suspend fun calculateProjectedSalary(shopId: String?, start: Long, end: Long): Double {
        val employees = if (shopId != null) getShopEmployees(shopId).firstOrNull() else getAllEmployees().firstOrNull()
        if (employees.isNullOrEmpty()) return 0.0

        val closedDays = getClosedDays(shopId, start, end).firstOrNull() ?: emptyList()
        var totalSalarySum = 0.0
        val now = System.currentTimeMillis()

        employees.forEach { employee ->
            val normalizedHireDate = Calendar.getInstance().apply {
                timeInMillis = employee.hireDate
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val terminateDate = employee.terminateDate
            if (start > (terminateDate ?: Long.MAX_VALUE) || end < normalizedHireDate) return@forEach

            val calcStart = maxOf(start, normalizedHireDate)
            val calcEnd = if (terminateDate != null) minOf(end, terminateDate) else end

            if (calcStart > calcEnd) return@forEach

            val recordsInWindow = getAttendanceFlow(employee.employeeId, calcStart, calcEnd).firstOrNull() ?: emptyList()

            var employeeSalary = 0.0
            val daysWorkedSet = mutableSetOf<String>()

            recordsInWindow.forEach { att ->
                val checkOutTime = att.checkOutTime
                val duration: Double = if (checkOutTime != null && checkOutTime > 0) {
                    (checkOutTime - att.checkInTime) / 3600000.0
                } else {
                    val sessionDay = Calendar.getInstance().apply { timeInMillis = att.checkInTime }
                    val currentDay = Calendar.getInstance()
                    if (sessionDay.get(Calendar.DAY_OF_YEAR) == currentDay.get(Calendar.DAY_OF_YEAR) &&
                        sessionDay.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR)) {
                        (now - att.checkInTime).toDouble() / 3600000.0
                    } else {
                        val shiftEndCal = sessionDay.clone() as Calendar
                        val endParts = att.shiftEnd.split(":")
                        shiftEndCal.set(Calendar.HOUR_OF_DAY, endParts[0].toInt())
                        shiftEndCal.set(Calendar.MINUTE, endParts[1].toInt())
                        val autoOutTime = shiftEndCal.timeInMillis
                        (autoOutTime - att.checkInTime).toDouble() / 3600000.0
                    }
                }

                val hourlyRate: Double = if (att.salaryType == "MONTHLY_FIXED") {
                    val dailyChunk = att.salaryRate / 30.0
                    val sParts = att.shiftStart.split(":")
                    val eParts = att.shiftEnd.split(":")
                    val scheduledHrs = (eParts[0].toInt() + eParts[1].toInt() / 60.0) - (sParts[0].toInt() + sParts[1].toInt() / 60.0)
                    val netScheduledHrs = scheduledHrs - att.breakHours
                    if (netScheduledHrs > 0) dailyChunk / netScheduledHrs else 0.0
                } else {
                    att.salaryRate
                }

                employeeSalary += duration * hourlyRate
                if (att.type == "WORK") {
                    daysWorkedSet.add(SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(att.checkInTime)))
                }
            }

            daysWorkedSet.forEach { dayStr ->
                val recordsForDay = recordsInWindow.filter { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.checkInTime)) == dayStr }
                if (recordsForDay.isNotEmpty()) {
                    val snapshot = recordsForDay.first()
                    if(snapshot.salaryType != "MONTHLY_FIXED") {
                        val breakDed = snapshot.breakHours
                        employeeSalary -= (breakDed * snapshot.salaryRate)
                    }
                }
            }
            totalSalarySum += employeeSalary
        }

        closedDays.forEach { closed ->
            if (closed.paySalary) {
                employees.forEach { emp ->
                    val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(closed.date))
                    val hasWorked = getAttendanceFlow(emp.employeeId, closed.date, closed.date + 86400000).firstOrNull()
                        ?.any { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.checkInTime)) == dateKey && it.type == "WORK" } ?: false

                    if (!hasWorked) {
                        val dailyChunk = if (emp.salaryType == "MONTHLY_FIXED") emp.salaryRate / 30.0 else (emp.salaryRate * 8.0)
                        totalSalarySum += dailyChunk
                    }
                }
            }
        }

        val pendingAdvance = employees.sumOf { getPendingAdvance(it.employeeId).firstOrNull() ?: 0.0 }
        return totalSalarySum - pendingAdvance
    }

    internal suspend fun calculateProratedExpenses(
        expenses: List<FixedExpense>,
        start: Long,
        end: Long
    ): Double {
        if (start > end) return 0.0

        var totalExpense = 0.0
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (tempCal.timeInMillis <= end) {
            val currentDay = tempCal.timeInMillis
            val relevantExpenses = expenses.filter { expense ->
                currentDay >= expense.createdAt
            }

            relevantExpenses.forEach { expense ->
                val expenseCal = Calendar.getInstance().apply { timeInMillis = currentDay }
                val daysInMonth = expenseCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (daysInMonth > 0) {
                    val dailyRate = expense.monthlyAmount / daysInMonth
                    totalExpense += dailyRate
                }
            }
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return totalExpense
    }

    fun getCashFlowSummary(shopId: String?, start: Long, end: Long, salary: Double = 0.0): Flow<CashFlowSummary?> {
        return getCashbookEntriesForPeriod(shopId, start, end).map { entries ->
            val totalIn = entries.filter { it.transactionType == "IN" }.sumOf { it.amount }
            val totalOut = entries.filter { it.transactionType == "OUT" }.sumOf { it.amount }
            val expenses = if (shopId != null) getFixedExpensesForShop(shopId).first() else getAllFixedExpenses().first()
            val fixedExpenses = calculateProratedExpenses(expenses, start, end)
            CashFlowSummary(totalIn, totalOut, salary, fixedExpenses)
        }
    }

    fun getFinancialBreakdown(shopId: String?, start: Long, end: Long): Flow<List<FinancialCategorySummary>> {
        return getCashbookEntriesForPeriod(shopId, start, end).map { entries ->
            entries.groupBy { Triple(it.category, it.description ?: "", it.transactionType) }
                .map { (key, list) -> FinancialCategorySummary(key.first, key.second, list.sumOf { it.amount }, key.third) }
        }
    }

    // ---------------- PURCHASES ----------------
    fun getShopSuppliers(shopId: String): Flow<List<Supplier>> = firebaseSync.getDataFlow<Supplier>("suppliers")
        .map { list -> if (shopId.isEmpty()) list else list.filter { it.shopId == shopId } }

    fun insertSupplier(supplier: Supplier) = firebaseSync.pushSupplier(supplier)

    fun recordPurchase(purchase: Purchase, cashbookEntry: Cashbook?) {
        firebaseSync.pushPurchase(purchase)
        cashbookEntry?.let { firebaseSync.pushCashbookEntry(it) }
    }

    // ---------------- STOCK ----------------
    fun insertStockMovement(movement: StockMovement) = firebaseSync.pushStockMovement(movement)

    fun startSync() { }
}

data class OrderWithItems(val order: Order, val items: List<OrderItem>)
data class CashFlowSummary(val totalIn: Double, val totalOut: Double, val salary: Double = 0.0, val fixedExpenses: Double = 0.0)
data class FinancialCategorySummary(val category: String, val description: String?, val totalAmount: Double, val transactionType: String)
