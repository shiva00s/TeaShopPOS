package com.teashop.pos

import android.app.Application
import androidx.room.Room
import com.teashop.pos.data.AppDatabase
import com.teashop.pos.data.MainRepository

class TeaShopApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MainRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "teashop_database"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = MainRepository(
            database.shopDao(), 
            database.orderDao(),
            database.staffDao(),
            database.reportDao(),
            database.syncDao(),
            database.purchaseDao()
        )
    }
}
