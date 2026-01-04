package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val itemId: String,
    val name: String,
    val category: String, // Tea, Coffee, Snack, etc.
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
