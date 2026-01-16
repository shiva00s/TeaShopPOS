package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class Reminder(
    @get:PropertyName("reminderId") @set:PropertyName("reminderId")
    var reminderId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    
    @get:PropertyName("dueDay") @set:PropertyName("dueDay")
    var dueDay: Int = 1,
    
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "",
    
    @get:PropertyName("lastPaidMonth") @set:PropertyName("lastPaidMonth")
    var lastPaidMonth: Int? = null,
    
    @get:PropertyName("lastPaidYear") @set:PropertyName("lastPaidYear")
    var lastPaidYear: Int? = null,
    
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true
)
