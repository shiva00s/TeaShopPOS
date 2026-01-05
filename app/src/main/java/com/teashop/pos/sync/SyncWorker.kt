package com.teashop.pos.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teashop.pos.TeaShopApplication

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TeaShopApplication
        val syncRepo = app.syncRepository

        return try {
            syncRepo.performFullSync()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
