package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.Item
import com.teashop.pos.data.entity.ShopItemPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ItemMasterViewModel @Inject constructor(private val repository: MainRepository) : ViewModel() {

    val allItems: StateFlow<List<Item>> = repository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedItem = MutableStateFlow<Item?>(null)
    val selectedItem: StateFlow<Item?> = _selectedItem.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _selectedItem.value = repository.getItem(itemId).firstOrNull()
        }
    }

    private fun getEmojiForCategory(category: String): String {
        val cat = category.lowercase()
        return when {
            cat.contains("tea") -> "☕"
            cat.contains("coffee") -> "☕"
            cat.contains("milk drink") -> "🥛"
            cat.contains("flavoured milk") -> "🌹"
            cat.contains("juice") -> "🍹"
            cat.contains("shake") -> "🥤"
            cat.contains("cooler") -> "🧊"
            cat.contains("ice cream") -> "🍨"
            cat.contains("falooda") -> "🍧"
            cat.contains("maggie") -> "🍜"
            cat.contains("fries") -> "🍟"
            cat.contains("bread") -> "🍞"
            cat.contains("sandwich") -> "🥪"
            cat.contains("chicken") -> "🍗"
            cat.contains("burger") -> "🍔"
            cat.contains("snack") -> "🍘"
            cat.contains("health") -> "💪"
            else -> "🍽️"
        }
    }

    private fun formatNameWithEmoji(name: String, category: String): String {
        val emoji = getEmojiForCategory(category)
        // Only add emoji if name doesn't already contain a symbol/emoji
        return if (name.any { Character.isSurrogate(it) || it.code > 127 }) name else "$emoji $name"
    }

    private fun formatCategoryWithEmoji(category: String): String {
        val emoji = getEmojiForCategory(category)
        // Only add emoji if category doesn't already contain a symbol/emoji
        return if (category.any { Character.isSurrogate(it) || it.code > 127 }) category else "$category $emoji"
    }

    fun addItemWithParcel(name: String, category: String, hasParcel: Boolean, parcelAmt: Double) {
        viewModelScope.launch {
            val finalCategory = formatCategoryWithEmoji(category)
            val finalName = formatNameWithEmoji(name, finalCategory)
            
            val item = Item(
                itemId = UUID.randomUUID().toString(),
                name = finalName,
                category = finalCategory,
                hasParcelCharge = hasParcel,
                defaultParcelCharge = parcelAmt
            )
            repository.insertItem(item)
        }
    }

    // New method for AI Menu Scan
    fun addItemFromScan(name: String, price: Double, shopId: String?) {
        viewModelScope.launch {
            val itemId = UUID.randomUUID().toString()
            val item = Item(
                itemId = itemId,
                name = "🔍 $name",
                category = "Scanned 🔍",
                isActive = true,
                globalPrice = price
            )
            repository.insertItem(item)
            
            if (shopId != null) {
                val shopPrice = ShopItemPrice(
                    shopId = shopId,
                    itemId = itemId,
                    sellingPrice = price
                )
                repository.updateShopItemPrice(shopPrice)
            }
        }
    }

    fun updateItem(item: Item, name: String, category: String, hasParcel: Boolean, parcelAmt: Double) {
        viewModelScope.launch {
            val finalCategory = formatCategoryWithEmoji(category)
            val finalName = formatNameWithEmoji(name, finalCategory)
            
            val updatedItem = item.copy(
                name = finalName,
                category = finalCategory,
                hasParcelCharge = hasParcel,
                defaultParcelCharge = parcelAmt
            )
            repository.updateItem(updatedItem)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun setPriceForShop(itemId: String, shopId: String, price: Double) {
        viewModelScope.launch {
            val shopPrice = ShopItemPrice(
                shopId = shopId,
                itemId = itemId,
                sellingPrice = price
            )
            repository.updateShopItemPrice(shopPrice)
        }
    }
}
