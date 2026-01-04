package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val reminderId: String,
    val shopId: String,
    val title: String, // Rent, EB Bill, Gas, etc.
    val amount: Double,
    val dueDay: Int, // 1 for 1st of month, etc.
    val category: String, // RENT, EB, GAS, etc.
    val lastPaidMonth: Int? = null, // Month index (0-11)
    val lastPaidYear: Int? = null,
    val isActive: Boolean = true
)
