package com.teashop.pos.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.teashop.pos.data.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query("""
        SELECT 
            SUM(CASE WHEN transactionType = 'IN' THEN amount ELSE 0 END) as totalIn,
            SUM(CASE WHEN transactionType = 'OUT' THEN amount ELSE 0 END) as totalOut
        FROM cashbook 
        WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime
    """)
    fun getCashFlowSummary(shopId: String, startTime: Long, endTime: Long): Flow<CashFlowSummary?>

    @Query("""
        SELECT category, SUM(amount) as totalAmount, transactionType
        FROM cashbook
        WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime
        GROUP BY category, transactionType
    """)
    fun getShopFinancialBreakdown(shopId: String, startTime: Long, endTime: Long): Flow<List<FinancialCategorySummary>>

    @Query("SELECT * FROM reminders WHERE shopId = :shopId AND isActive = 1")
    fun getActiveReminders(shopId: String): Flow<List<Reminder>>

    @Query("""
        SELECT i.name as itemName, SUM(sm.quantity) as currentStock
        FROM items i
        LEFT JOIN stock_movements sm ON i.itemId = sm.itemId
        WHERE sm.shopId = :shopId OR sm.shopId IS NULL
        GROUP BY i.itemId
        HAVING currentStock < 10
    """)
    fun getLowStockAlerts(shopId: String): Flow<List<StockStatus>>

    @Query("""
        SELECT i.name as itemName, SUM(sm.quantity) as currentStock
        FROM items i
        JOIN stock_movements sm ON i.itemId = sm.itemId
        WHERE sm.shopId = :shopId
        GROUP BY i.itemId
    """)
    fun getShopStockStatus(shopId: String): Flow<List<StockStatus>>

    @Query("SELECT SUM(payableAmount) FROM orders WHERE status = 'CLOSED' AND createdAt >= :startOfDay")
    fun getGlobalTodaySales(startOfDay: Long): Flow<Double?>
}

data class CashFlowSummary(
    val totalIn: Double,
    val totalOut: Double
)

data class FinancialCategorySummary(
    val category: String,
    val totalAmount: Double,
    val transactionType: String
)

data class StockStatus(
    val itemName: String,
    val currentStock: Double
)
