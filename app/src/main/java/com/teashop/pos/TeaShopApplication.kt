package com.teashop.pos

import android.app.Application
import androidx.room.Room
import com.teashop.pos.api.ApiService
import com.teashop.pos.data.AppDatabase
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.SyncRepository
import com.teashop.pos.ui.viewmodel.ViewModelFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TeaShopApplication : Application() {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "teashop_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://your-api-endpoint.com/api/") // Placeholder
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val mainRepository: MainRepository by lazy {
        MainRepository(
            database.shopDao(),
            database.orderDao(),
            database.staffDao(),
            database.reportDao(),
            database.syncDao(),
            database.purchaseDao()
        )
    }

    val viewModelFactory: ViewModelFactory by lazy {
        ViewModelFactory(mainRepository)
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(apiService, database)
    }
}
