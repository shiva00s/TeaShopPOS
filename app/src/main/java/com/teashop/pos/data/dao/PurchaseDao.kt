package com.teashop.pos.data.dao

import androidx.room.*
import com.teashop.pos.data.entity.Purchase
import com.teashop.pos.data.entity.Supplier
import com.teashop.pos.data.entity.Cashbook
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)

    @Query("SELECT * FROM suppliers WHERE shopId = :shopId")
    fun getShopSuppliers(shopId: String): Flow<List<Supplier>>

    @Insert
    suspend fun insertPurchase(purchase: Purchase)

    @Insert
    suspend fun insertCashbookEntry(entry: Cashbook)

    /**
     * Records a purchase and its initial payment.
     */
    @Transaction
    suspend fun recordPurchase(purchase: Purchase, cashbookEntry: Cashbook?) {
        insertPurchase(purchase)
        cashbookEntry?.let { insertCashbookEntry(it) }
    }

    /**
     * Reminder Logic: Get all purchases with outstanding balances.
     */
    @Query("SELECT * FROM purchases WHERE shopId = :shopId AND balanceAmount > 0")
    fun getPendingPayments(shopId: String): Flow<List<Purchase>>

    @Query("UPDATE purchases SET paidAmount = paidAmount + :amount, balanceAmount = balanceAmount - :amount, isSettled = (balanceAmount - :amount <= 0) WHERE purchaseId = :purchaseId")
    suspend fun payBalance(purchaseId: String, amount: Double)

    @Transaction
    suspend fun settleBalance(purchaseId: String, amount: Double, cashbookEntry: Cashbook) {
        payBalance(purchaseId, amount)
        insertCashbookEntry(cashbookEntry)
    }
}
