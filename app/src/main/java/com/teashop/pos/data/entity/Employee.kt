package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey val employeeId: String,
    val shopId: String,
    val name: String,
    val phone: String,
    val salaryType: String, // PER_HOUR, MONTHLY_FIXED
    val salaryRate: Double,
    val shiftStart: String = "10:00", // 24h format HH:mm
    val shiftEnd: String = "22:00",
    val graceMinutes: Int = 5,
    val otRateMultiplier: Double = 1.0, // 1.0x, 1.5x, 2.0x
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
