package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salary_payments")
data class SalaryPayment(
    @PrimaryKey val paymentId: String,
    val employeeId: String,
    val shopId: String,
    val amount: Double,
    val lateDeduction: Double = 0.0,
    val netPaid: Double,
    val periodStart: Long,
    val periodEnd: Long,
    val paymentDate: Long = System.currentTimeMillis()
)
