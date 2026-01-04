package com.teashop.pos.data.dao

import androidx.room.*
import com.teashop.pos.data.entity.Shop
import com.teashop.pos.data.entity.Item
import com.teashop.pos.data.entity.ShopItemPrice
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: Shop)

    @Query("SELECT * FROM shops")
    fun getAllShops(): Flow<List<Shop>>

    @Query("SELECT * FROM shops WHERE shopId = :shopId")
    suspend fun getShopById(shopId: String): Shop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Query("SELECT * FROM items WHERE isActive = 1")
    fun getActiveItems(): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateShopItemPrice(price: ShopItemPrice)

    @Query("""
        SELECT i.*, sip.sellingPrice 
        FROM items i 
        JOIN shop_item_prices sip ON i.itemId = sip.itemId 
        WHERE sip.shopId = :shopId AND i.isActive = 1
    """)
    fun getShopMenu(shopId: String): Flow<List<ShopMenuItem>>
}

data class ShopMenuItem(
    @Embedded val item: Item,
    val sellingPrice: Double
)
