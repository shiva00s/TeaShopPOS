package com.teashop.pos.data

import android.util.Log
import com.teashop.pos.api.ApiService
import com.teashop.pos.data.dao.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRepository(
    private val apiService: ApiService,
    private val db: AppDatabase
) {

    suspend fun performFullSync() = withContext(Dispatchers.IO) {
        // 1. PUSH local changes to Cloud
        pushOrders()
        pushCashbook()
        
        // 2. PULL new data from Cloud (Multi-Mobile Sync)
        pullShops()
        pullGlobalSales()
    }

    private suspend fun pushOrders() {
        try {
            val unsynced = db.syncDao().getUnsyncedOrders()
            if (unsynced.isEmpty()) return
            val response = apiService.pushOrders(unsynced)
            if (response.isSuccessful) db.syncDao().markOrdersSynced(unsynced.map { it.orderId })
        } catch (e: Exception) { Log.e("Sync", "Order Push failed", e) }
    }

    private suspend fun pushCashbook() {
        try {
            val unsynced = db.syncDao().getUnsyncedCashbook()
            if (unsynced.isEmpty()) return
            val response = apiService.pushCashbook(unsynced)
            if (response.isSuccessful) db.syncDao().markCashbookSynced(unsynced.map { it.entryId })
        } catch (e: Exception) { Log.e("Sync", "Cashbook Push failed", e) }
    }

    private suspend fun pullShops() {
        try {
            // Fetch all shops from cloud that this owner owns
            val response = apiService.getGlobalSummary() // Example endpoint
            if (response.isSuccessful) {
                // Update local DB with new shops/data from other mobiles
                // response.body()?.shopSummaries?.forEach { ... }
            }
        } catch (e: Exception) { Log.e("Sync", "Pull failed", e) }
    }

    private suspend fun pullGlobalSales() {
        // Implementation to fetch total profit from all mobiles
    }
}
