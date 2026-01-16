package com.teashop.pos

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.teashop.pos.data.MainRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TeaShopApplication : Application() {

    override fun onCreate() {
        // 1. Enable persistence BEFORE Hilt injection (super.onCreate)
        try {
            val database = FirebaseDatabase.getInstance("https://teashoppos-3b3bd-default-rtdb.asia-southeast1.firebasedatabase.app/")
            database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Log error but don't crash
        }

        super.onCreate()

        val sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val themeMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)

        // 2. Initial sync will be managed by the repository/viewmodels
    }
}
