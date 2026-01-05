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

    @Transaction
    suspend fun recordPurchase(purchase: Purchase, cashbookEntry: Cashbook?) {
        insertPurchase(purchase)
        cashbookEntry?.let { insertCashbookEntry(it) }
    }

    @Query("SELECT * FROM purchases WHERE shopId = :shopId AND balanceAmount > 0")
    fun getPendingPayments(shopId: String): Flow<List<Purchase>>

    // Corrected payBalance to be a safe, multi-step transaction
    @Transaction
    suspend fun payBalance(purchaseId: String, amount: Double) {
        // Step 1: Update the amounts
        updatePurchasePayment(purchaseId, amount)
        // Step 2: Mark as settled if balance is now <= 0
        markPurchaseSettled(purchaseId)
    }

    @Query("UPDATE purchases SET paidAmount = paidAmount + :amount, balanceAmount = balanceAmount - :amount WHERE purchaseId = :purchaseId")
    suspend fun updatePurchasePayment(purchaseId: String, amount: Double)

    @Query("UPDATE purchases SET isSettled = 1 WHERE purchaseId = :purchaseId AND balanceAmount <= 0")
    suspend fun markPurchaseSettled(purchaseId: String)

    @Transaction
    suspend fun settleBalance(purchaseId: String, amount: Double, cashbookEntry: Cashbook) {
        payBalance(purchaseId, amount) // This now calls the safe transactional method
        insertCashbookEntry(cashbookEntry)
    }
}
