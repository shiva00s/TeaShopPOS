package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey val shopId: String, // UUID for sync compatibility
    val name: String,
    val location: String,
    val openingDate: Long,
    val openingCashBalance: Double,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
