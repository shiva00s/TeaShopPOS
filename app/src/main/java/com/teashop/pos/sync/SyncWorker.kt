package com.teashop.pos.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.api.ApiService
import com.teashop.pos.data.SyncRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TeaShopApplication
        
        // This would usually be injected or provided by a central Network module
        val retrofit = Retrofit.Builder()
            .baseUrl("https://your-api-endpoint.com/api/") // Placeholder
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val apiService = retrofit.create(ApiService::class.java)
        val syncRepo = SyncRepository(apiService, app.database.syncDao())

        return try {
            syncRepo.performFullSync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
