package com.teashop.pos.data

import android.util.Log
import com.teashop.pos.api.ApiService
import com.teashop.pos.data.dao.SyncDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles the "Offline-first" sync logic.
 * Periodically or manually pushes local data to the PostgreSQL cloud.
 */
class SyncRepository(
    private val apiService: ApiService,
    private val syncDao: SyncDao
) {

    suspend fun performFullSync() = withContext(Dispatchers.IO) {
        syncOrders()
        syncCashbook()
    }

    private suspend fun syncOrders() {
        try {
            val unsynced = syncDao.getUnsyncedOrders()
            if (unsynced.isEmpty()) return

            val response = apiService.pushOrders(unsynced)
            if (response.isSuccessful) {
                syncDao.markOrdersSynced(unsynced.map { it.orderId })
                Log.d("Sync", "Successfully synced ${unsynced.size} orders")
            }
        } catch (e: Exception) {
            Log.e("Sync", "Order sync failed: ${e.message}")
        }
    }

    private suspend fun syncCashbook() {
        try {
            val unsynced = syncDao.getUnsyncedCashbook()
            if (unsynced.isEmpty()) return

            val response = apiService.pushCashbook(unsynced)
            if (response.isSuccessful) {
                syncDao.markCashbookSynced(unsynced.map { it.entryId })
                Log.d("Sync", "Successfully synced ${unsynced.size} cashbook entries")
            }
        } catch (e: Exception) {
            Log.e("Sync", "Cashbook sync failed: ${e.message}")
        }
    }
}
