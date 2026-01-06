package com.teashop.pos.data

import com.teashop.pos.data.dao.*
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.Flow

class MainRepository(
    val shopDao: ShopDao,
    val orderDao: OrderDao,
    val staffDao: StaffDao,
    val reportDao: ReportDao,
    val syncDao: SyncDao,
    val purchaseDao: PurchaseDao
) {
    // Shop Operations
    fun getAllShops(): Flow<List<Shop>> = shopDao.getAllShops()
    suspend fun insertShop(shop: Shop) = shopDao.insertShop(shop)
    
    // Menu & Item Operations
    fun getActiveItems(): Flow<List<Item>> = shopDao.getActiveItems()
    fun getShopMenu(shopId: String) = shopDao.getShopMenu(shopId)
    suspend fun getItem(itemId: String) = shopDao.getItem(itemId)
    suspend fun insertItem(item: Item) = shopDao.insertItem(item)
    suspend fun updateItem(item: Item) = shopDao.updateItem(item)
    suspend fun deleteItem(item: Item) = shopDao.deleteItem(item)
    suspend fun updateShopItemPrice(price: ShopItemPrice) = shopDao.updateShopItemPrice(price)

    // Order Operations
    fun getOpenOrders(shopId: String) = orderDao.getOpenOrders(shopId)
    fun getHeldOrders(shopId: String) = orderDao.getHeldOrders(shopId)
    suspend fun getOrderWithItems(orderId: String) = orderDao.getOrderWithItems(orderId)
    suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)
    
    suspend fun completeOrder(
        order: Order, 
        items: List<OrderItem>, 
        cashbookEntry: Cashbook, 
        stockMovements: List<StockMovement>
    ) {
        orderDao.completeOrder(order, items, cashbookEntry, stockMovements)
    }

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun insertOrderItem(item: OrderItem) = orderDao.insertOrderItem(item)
    suspend fun getCashbookEntry(entryId: String) = orderDao.getCashbookEntry(entryId)
    suspend fun updateCashbookEntry(entry: Cashbook) = orderDao.updateCashbookEntry(entry)
    suspend fun insertCashbookEntry(entry: Cashbook) = orderDao.insertCashbookEntry(entry)
    suspend fun deleteCashbookEntry(entry: Cashbook) = orderDao.deleteCashbookEntry(entry)
    fun getCashbookEntriesForPeriod(shopId: String, startTime: Long, endTime: Long) = orderDao.getCashbookEntriesForPeriod(shopId, startTime, endTime)
    suspend fun insertStockMovement(movement: StockMovement) = orderDao.insertStockMovement(movement)

    // Staff Operations
    fun getShopEmployees(shopId: String) = staffDao.getShopEmployees(shopId)
    suspend fun getEmployee(employeeId: String) = staffDao.getEmployee(employeeId)
    suspend fun insertEmployee(employee: Employee) = staffDao.insertEmployee(employee)
    suspend fun updateEmployee(employee: Employee) = staffDao.updateEmployee(employee)
    suspend fun deleteEmployee(employee: Employee) = staffDao.deleteEmployee(employee)
    suspend fun insertAttendance(attendance: Attendance) = staffDao.insertAttendance(attendance)
    suspend fun updateAttendance(attendance: Attendance) = staffDao.updateAttendance(attendance)
    suspend fun getLastAttendance(employeeId: String) = staffDao.getLastAttendance(employeeId)
    suspend fun getAttendanceForPeriod(employeeId: String, start: Long, end: Long) = staffDao.getAttendanceForPeriod(employeeId, start, end)
    suspend fun getPendingAdvance(employeeId: String) = staffDao.getPendingAdvance(employeeId)
    suspend fun insertAdvance(advance: AdvancePayment) = staffDao.insertAdvance(advance)
    suspend fun processSalary(payment: SalaryPayment, cashbookEntry: Cashbook, recoverAdvance: Boolean) = staffDao.processSalary(payment, cashbookEntry, recoverAdvance)

    // Purchase & Supplier Operations
    fun getShopSuppliers(shopId: String) = purchaseDao.getShopSuppliers(shopId)
    suspend fun insertSupplier(supplier: Supplier) = purchaseDao.insertSupplier(supplier)
    suspend fun recordPurchase(purchase: Purchase, cashbookEntry: Cashbook?) = purchaseDao.recordPurchase(purchase, cashbookEntry)
}
