package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.Item
import com.teashop.pos.data.entity.ShopItemPrice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ItemMasterViewModel(private val repository: MainRepository) : ViewModel() {

    val allItems: StateFlow<List<Item>> = repository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String, category: String) {
        viewModelScope.launch {
            val item = Item(
                itemId = UUID.randomUUID().toString(),
                name = name,
                category = category
            )
            repository.insertItem(item)
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
