package com.teashop.pos.data.entity

import com.google.firebase.database.PropertyName

data class FixedExpense(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("monthlyAmount") @set:PropertyName("monthlyAmount")
    var monthlyAmount: Double = 0.0,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
)
