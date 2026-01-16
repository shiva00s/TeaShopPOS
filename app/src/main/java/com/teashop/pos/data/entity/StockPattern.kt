package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class StockPattern(
    @get:PropertyName("itemId") @set:PropertyName("itemId")
    var itemId: String = "",
    
    @get:PropertyName("shopId") @set:PropertyName("shopId")
    var shopId: String = "",
    
    @get:PropertyName("weekdayStandard") @set:PropertyName("weekdayStandard")
    var weekdayStandard: Double = 0.0,
    
    @get:PropertyName("weekendStandard") @set:PropertyName("weekendStandard")
    var weekendStandard: Double = 0.0,
    
    @get:PropertyName("supplierId") @set:PropertyName("supplierId")
    var supplierId: String? = null
)
