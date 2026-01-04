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

    @Query("SELECT * FROM orders WHERE shopId = :shopId AND status = 'OPEN'")
    fun getOpenOrders(shopId: String): Flow<List<Order>>

    @Insert
    suspend fun insertCashbookEntry(entry: Cashbook)

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
