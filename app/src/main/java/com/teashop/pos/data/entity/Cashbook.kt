package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.util.UUID

@Keep
@IgnoreExtraProperties
data class Cashbook(
    @get:PropertyName("entryId") @set:PropertyName("entryId")
    var entryId: String = UUID.randomUUID().toString(),
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("transactionType") @set:PropertyName("transactionType")
    var transactionType: String = "IN", // IN, OUT
    
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "OTHER", // SALES, EXPENSE, PURCHASE, SALARY, ADVANCE, CREDIT_SETTLEMENT, OTHER
    
    @get:PropertyName("amount") @set:PropertyName("amount")
    var amount: Double = 0.0,
    
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String? = null,
    
    @get:PropertyName("referenceId") @set:PropertyName("referenceId")
    var referenceId: String? = null, // OrderId, ExpenseId, etc.
    
    @get:PropertyName("transactionDate") @set:PropertyName("transactionDate")
    var transactionDate: Long = System.currentTimeMillis(),
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),

    // Sync Metadata
    @get:PropertyName("lastModified") @set:PropertyName("lastModified")
    var lastModified: Long = System.currentTimeMillis(),
    
    @get:PropertyName("isSynced") @set:PropertyName("isSynced")
    var isSynced: Boolean = false
)
