package com.teashop.pos.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncManager @Inject constructor() {

    companion object {
        private const val DB_URL = "https://teashoppos-3b3bd-default-rtdb.asia-southeast1.firebasedatabase.app/"
    }

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(DB_URL).apply {
        setPersistenceEnabled(true)
    }.reference

    fun getOwnerRef(): DatabaseReference? {
        val uid = auth.currentUser?.uid ?: return null
        return database.child("owners").child(uid)
    }

    // Generic Flow for any table
    inline fun <reified T : Any> getDataFlow(table: String): Flow<List<T>> = callbackFlow {
        val ref = getOwnerRef()?.child(table) ?: run {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<T>()
                for (childSnapshot in snapshot.children) {
                    try {
                        if (childSnapshot.hasChildren()) {
                            val item = childSnapshot.getValue(T::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        } else {
                            Log.w("FirebaseSyncManager", "Skipping primitive value in table '$table': ${childSnapshot.value}")
                        }
                    } catch (e: DatabaseException) {
                        Log.e("FirebaseSyncManager", "Failed to convert to ${T::class.java.name} in table '$table' for key ${childSnapshot.key}", e)
                    }
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Single item flow
    inline fun <reified T : Any> getItemFlow(table: String, itemId: String): Flow<T?> = callbackFlow {
        val ref = getOwnerRef()?.child(table)?.child(itemId) ?: run {
            trySend(null)
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(T::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Write Methods
    fun pushShop(shop: Shop) = getOwnerRef()?.child("shops")?.child(shop.shopId)?.setValue(shop)
    fun pushItem(item: Item) = getOwnerRef()?.child("items")?.child(item.itemId)?.setValue(item)
    fun pushPrice(price: ShopItemPrice) = getOwnerRef()?.child("prices")?.child("${price.shopId}_${price.itemId}")?.setValue(price)
    fun pushOrder(order: Order) = getOwnerRef()?.child("orders")?.child(order.orderId)?.setValue(order)
    fun pushOrderItem(item: OrderItem) = getOwnerRef()?.child("order_items")?.child(item.orderItemId)?.setValue(item)
    fun pushCashbookEntry(entry: Cashbook) = getOwnerRef()?.child("cashbook")?.child(entry.entryId)?.setValue(entry)
    fun pushEmployee(emp: Employee) = getOwnerRef()?.child("employees")?.child(emp.employeeId)?.setValue(emp)
    fun pushAttendance(att: Attendance) = getOwnerRef()?.child("attendance")?.child(att.attendanceId)?.setValue(att)
    fun pushStockMovement(sm: StockMovement) = getOwnerRef()?.child("stock_movements")?.child(sm.movementId)?.setValue(sm)
    fun pushAdvance(adv: AdvancePayment) = getOwnerRef()?.child("advance_payments")?.child(adv.advanceId)?.setValue(adv)
    fun pushSalaryPayment(p: SalaryPayment) = getOwnerRef()?.child("salary_payments")?.child(p.paymentId)?.setValue(p)
    fun pushSupplier(s: Supplier) = getOwnerRef()?.child("suppliers")?.child(s.supplierId)?.setValue(s)
    fun pushPurchase(p: Purchase) = getOwnerRef()?.child("purchases")?.child(p.purchaseId)?.setValue(p)
    fun pushHistory(hist: EmployeeHistory) = getOwnerRef()?.child("employee_history")?.child(hist.historyId)?.setValue(hist)
    fun pushClosedDay(day: ShopClosedDay) = getOwnerRef()?.child("shop_closed_days")?.child(day.id)?.setValue(day)
    fun pushReminder(reminder: Reminder) = getOwnerRef()?.child("reminders")?.child(reminder.reminderId)?.setValue(reminder)
    fun pushStockPattern(pattern: StockPattern) = getOwnerRef()?.child("stock_patterns")?.child("${pattern.shopId}_${pattern.itemId}")?.setValue(pattern)
    fun pushShopTable(table: ShopTable) = getOwnerRef()?.child("shop_tables")?.child("${table.shopId}_${table.tableId}")?.setValue(table)
    fun pushProfile(profile: UserProfile) = getOwnerRef()?.child("user_profiles")?.child(profile.uid)?.setValue(profile)
    fun pushFixedExpense(expense: FixedExpense) = getOwnerRef()?.child("fixed_expenses")?.child(expense.id)?.setValue(expense)

    // Delete Methods
    fun deleteShop(shopId: String) = getOwnerRef()?.child("shops")?.child(shopId)?.removeValue()
    fun deleteItem(itemId: String) = getOwnerRef()?.child("items")?.child(itemId)?.removeValue()
    fun deleteOrder(orderId: String) = getOwnerRef()?.child("orders")?.child(orderId)?.removeValue()
    fun deleteOrderItem(orderItemId: String) = getOwnerRef()?.child("order_items")?.child(orderItemId)?.removeValue()
    fun deleteCashbookEntry(entryId: String) = getOwnerRef()?.child("cashbook")?.child(entryId)?.removeValue()
    fun deleteEmployee(employeeId: String) = getOwnerRef()?.child("employees")?.child(employeeId)?.removeValue()
    fun deleteAttendance(attendanceId: String) = getOwnerRef()?.child("attendance")?.child(attendanceId)?.removeValue()
    fun deleteAdvance(advanceId: String) = getOwnerRef()?.child("advance_payments")?.child(advanceId)?.removeValue()
    fun deleteClosedDay(dayId: String) = getOwnerRef()?.child("shop_closed_days")?.child(dayId)?.removeValue()
    fun deleteReminder(reminderId: String) = getOwnerRef()?.child("reminders")?.child(reminderId)?.removeValue()

    fun deleteCashbookEntryByReference(referenceId: String) {
        getOwnerRef()?.child("cashbook")?.orderByChild("referenceId")?.equalTo(referenceId)?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { it.ref.removeValue() }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
