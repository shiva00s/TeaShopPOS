package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey val attendanceId: String,
    val employeeId: String,
    val shopId: String,
    val checkInTime: Long,
    val checkOutTime: Long? = null,
    val type: String = "WORK", // WORK or BREAK (GAP)
    val hoursWorked: Double = 0.0,
    val lateDeduction: Double = 0.0,
    val otHours: Double = 0.0,
    val isSynced: Boolean = false
)
