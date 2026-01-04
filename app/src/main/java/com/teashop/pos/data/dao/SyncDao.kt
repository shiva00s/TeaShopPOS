package com.teashop.pos.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.teashop.pos.data.entity.Order
import com.teashop.pos.data.entity.Cashbook
import com.teashop.pos.data.entity.StockMovement

@Dao
interface SyncDao {

    @Query("SELECT * FROM orders WHERE isSynced = 0")
    suspend fun getUnsyncedOrders(): List<Order>

    @Query("UPDATE orders SET isSynced = 1 WHERE orderId IN (:orderIds)")
    suspend fun markOrdersSynced(orderIds: List<String>)

    @Query("SELECT * FROM cashbook WHERE isSynced = 0")
    suspend fun getUnsyncedCashbook(): List<Cashbook>

    @Query("UPDATE cashbook SET isSynced = 1 WHERE entryId IN (:entryIds)")
    suspend fun markCashbookSynced(entryIds: List<String>)
    
    // Add similar methods for StockMovements if needed
}
