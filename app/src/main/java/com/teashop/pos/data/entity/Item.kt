package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val itemId: String,
    val name: String,
    val category: String,
    val isActive: Boolean = true,
    val hasParcelCharge: Boolean = false,
    val defaultParcelCharge: Double = 5.0,
    val globalPrice: Double = 0.0, // Added: Set once, used by all shops by default
    val createdAt: Long = System.currentTimeMillis()
)
