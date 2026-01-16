package com.teashop.pos.data.entity

import androidx.annotation.Keep
import com.google.firebase.database.PropertyName

@Keep
data class UserProfile(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    
    @get:PropertyName("phone") @set:PropertyName("phone")
    var phone: String = "",
    
    @get:PropertyName("joinDate") @set:PropertyName("joinDate")
    var joinDate: Long = System.currentTimeMillis(),
    
    @get:PropertyName("isPremium") @set:PropertyName("isPremium")
    var isPremium: Boolean = false,
    
    @get:PropertyName("subscriptionExpiry") @set:PropertyName("subscriptionExpiry")
    var subscriptionExpiry: Long? = null,
    
    @get:PropertyName("lastCheckTimestamp") @set:PropertyName("lastCheckTimestamp")
    var lastCheckTimestamp: Long = System.currentTimeMillis()
)
