package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey val supplierId: String,
    val shopId: String,
    val name: String,
    val contact: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
