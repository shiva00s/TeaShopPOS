package com.teashop.pos.data.entity

import com.google.firebase.database.PropertyName

data class FixedExpenseHistory(
    @get:PropertyName("historyId") @set:PropertyName("historyId")
    var historyId: String = "",

    @get:PropertyName("expenseId") @set:PropertyName("expenseId")
    var expenseId: String = "",

    @get:PropertyName("monthlyAmount") @set:PropertyName("monthlyAmount")
    var monthlyAmount: Double = 0.0,

    @get:PropertyName("changeDate") @set:PropertyName("changeDate")
    var changeDate: Long = System.currentTimeMillis()
)
