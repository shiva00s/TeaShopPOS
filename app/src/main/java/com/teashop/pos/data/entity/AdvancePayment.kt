package com.teashop.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "advance_payments")
data class AdvancePayment(
    @PrimaryKey val advanceId: String,
    val employeeId: String,
    val shopId: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val isRecovered: Boolean = false,
    val recoveryPaymentId: String? = null // Linked to SalaryPayment
)
