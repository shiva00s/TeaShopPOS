package com.teashop.pos.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teashop.pos.data.dao.*
import com.teashop.pos.data.entity.*

@Database(
    entities = [
        Shop::class,
        Item::class,
        ShopItemPrice::class,
        ShopTable::class,
        Order::class,
        OrderItem::class,
        Cashbook::class,
        StockMovement::class,
        Employee::class,
        Attendance::class,
        SalaryPayment::class,
        AdvancePayment::class,
        Supplier::class,
        Purchase::class,
        Reminder::class,
        StockPattern::class
    ],
    version = 10, // Incremented after fixing StockPattern schema
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    abstract fun orderDao(): OrderDao
    abstract fun reportDao(): ReportDao
    abstract fun staffDao(): StaffDao
    abstract fun syncDao(): SyncDao
    abstract fun purchaseDao(): PurchaseDao
}
