package com.teashop.pos.data.dao

import androidx.room.*
import com.teashop.pos.data.entity.Order
import com.teashop.pos.data.entity.OrderItem
import com.teashop.pos.data.entity.Cashbook
import com.teashop.pos.data.entity.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(orderItem: OrderItem)

    @Transaction
    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    suspend fun getOrderWithItems(orderId: String): OrderWithItems?

    @Query("SELECT * FROM cashbook WHERE shopId = :shopId AND transactionDate >= :startTime AND transactionDate <= :endTime")
    fun getCashbookEntriesForPeriod(shopId: String, startTime: Long, endTime: Long): Flow<List<Cashbook>>

    @Query("SELECT * FROM cashbook WHERE entryId = :entryId")
    suspend fun getCashbookEntry(entryId: String): Cashbook?

    @Update
    suspend fun updateCashbookEntry(entry: Cashbook)

    @Query("SELECT * FROM orders WHERE shopId = :shopId AND status = 'OPEN'")
    fun getOpenOrders(shopId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE shopId = :shopId AND status = 'HOLD'")
    fun getHeldOrders(shopId: String): Flow<List<Order>>

    @Delete
    suspend fun deleteOrder(order: Order)

    @Insert
    suspend fun insertCashbookEntry(entry: Cashbook)

    @Delete
    suspend fun deleteCashbookEntry(entry: Cashbook)

    @Insert
    suspend fun insertStockMovement(movement: StockMovement)

    /**
     * Closes an order, records payment in cashbook, and deducts stock.
     * This ensures the "No sale without cashbook and stock deduction" rule.
     */
    @Transaction
    suspend fun completeOrder(order: Order, items: List<OrderItem>, cashbookEntry: Cashbook, stockMovements: List<StockMovement>) {
        insertOrder(order)
        items.forEach { insertOrderItem(it) }
        insertCashbookEntry(cashbookEntry)
        stockMovements.forEach { insertStockMovement(it) }
    }
}

data class OrderWithItems(
    @Embedded val order: Order,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<OrderItem>
)
