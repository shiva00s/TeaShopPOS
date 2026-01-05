package com.teashop.pos.data.dao

import androidx.room.*
import com.teashop.pos.data.entity.*
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
        SELECT i.name as itemName, 
               SUM(sm.quantity) as currentStock,
               sp.weekdayStandard,
               sp.weekendStandard,
               sup.name as supplierName,
               sup.contact as supplierContact
        FROM items i
        JOIN stock_movements sm ON i.itemId = sm.itemId
        JOIN stock_patterns sp ON i.itemId = sp.itemId AND sp.shopId = :shopId
        LEFT JOIN suppliers sup ON sp.supplierId = sup.supplierId
        WHERE sm.shopId = :shopId
        GROUP BY i.itemId
    """)
    fun getSmartStockReminders(shopId: String): Flow<List<SmartStockReminder>>

    @Query("""
        SELECT category, description, SUM(amount) as totalAmount, transactionType
        FROM cashbook
        WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime
        GROUP BY category, description, transactionType
    """)
    fun getShopFinancialBreakdown(shopId: String, startTime: Long, endTime: Long): Flow<List<FinancialCategorySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStockPattern(pattern: StockPattern)

    @Query("""
        SELECT 
            SUM(CASE WHEN transactionType = 'IN' THEN amount ELSE 0 END) - 
            SUM(CASE WHEN transactionType = 'OUT' THEN amount ELSE 0 END)
        FROM cashbook 
        WHERE transactionDate >= :startOfDay
    """)
    fun getGlobalTodaySales(startOfDay: Long): Flow<Double?>
}

data class SmartStockReminder(
    val itemName: String,
    val currentStock: Double,
    val weekdayStandard: Double,
    val weekendStandard: Double,
    val supplierName: String?,
    val supplierContact: String?
)

data class CashFlowSummary(val totalIn: Double, val totalOut: Double)
data class FinancialCategorySummary(val category: String, val description: String?, val totalAmount: Double, val transactionType: String)
