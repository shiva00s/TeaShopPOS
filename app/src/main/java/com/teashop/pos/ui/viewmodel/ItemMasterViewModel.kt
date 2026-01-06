package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.Item
import com.teashop.pos.data.entity.ShopItemPrice
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ItemMasterViewModel(private val repository: MainRepository) : ViewModel() {

    val allItems: StateFlow<List<Item>> = repository.getActiveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedItem = MutableStateFlow<Item?>(null)
    val selectedItem: StateFlow<Item?> = _selectedItem.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _selectedItem.value = repository.getItem(itemId)
        }
    }

    fun addItemWithParcel(name: String, category: String, hasParcel: Boolean, parcelAmt: Double) {
        viewModelScope.launch {
            val item = Item(
                itemId = UUID.randomUUID().toString(),
                name = name,
                category = category,
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
                name = name,
                category = "Scanned",
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
            val updatedItem = item.copy(
                name = name,
                category = category,
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
