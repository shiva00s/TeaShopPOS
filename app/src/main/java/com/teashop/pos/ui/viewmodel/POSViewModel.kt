package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.dao.ShopMenuItem
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class POSViewModel(private val repository: MainRepository) : ViewModel() {

    private val _currentShopId = MutableStateFlow<String?>(null)
    val currentShopId: StateFlow<String?> = _currentShopId.asStateFlow()

    private val _menuItems = MutableStateFlow<List<ShopMenuItem>>(emptyList())
    val menuItems: StateFlow<List<ShopMenuItem>> = _menuItems.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    fun setShop(shopId: String) {
        _currentShopId.value = shopId
        viewModelScope.launch {
            repository.getShopMenu(shopId).collect {
                _menuItems.value = it
            }
        }
    }

    fun addToCart(menuItem: ShopMenuItem) {
        val currentList = _cart.value.toMutableList()
        val existing = currentList.find { it.item.itemId == menuItem.item.itemId }
        if (existing != null) {
            val index = currentList.indexOf(existing)
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(CartItem(menuItem.item, menuItem.sellingPrice, 1.0))
        }
        _cart.value = currentList
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentList = _cart.value.toMutableList()
        val existing = currentList.find { it.item.itemId == cartItem.item.itemId }
        if (existing != null) {
            if (existing.quantity > 1) {
                val index = currentList.indexOf(existing)
                currentList[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                currentList.remove(existing)
            }
        }
        _cart.value = currentList
    }

    fun checkout(paymentMethod: String, serviceType: String, tableId: String? = null) {
        val shopId = _currentShopId.value ?: return
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            val orderId = UUID.randomUUID().toString()
            val totalAmount = cartItems.sumOf { it.price * it.quantity }
            
            val order = Order(
                orderId = orderId,
                shopId = shopId,
                tableId = tableId,
                serviceType = serviceType,
                totalAmount = totalAmount,
                payableAmount = totalAmount,
                paymentStatus = "PAID",
                paymentMethod = paymentMethod,
                status = "CLOSED",
                closedAt = System.currentTimeMillis()
            )

            val orderItems = cartItems.map {
                OrderItem(
                    orderItemId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    itemId = it.item.itemId,
                    itemName = it.item.name,
                    quantity = it.quantity,
                    unitPrice = it.price,
                    subTotal = it.price * it.quantity
                )
            }

            val cashbookEntry = Cashbook(
                entryId = UUID.randomUUID().toString(),
                shopId = shopId,
                transactionType = "IN",
                category = "SALES",
                amount = totalAmount,
                referenceId = orderId,
                description = "Sale from POS - $serviceType ($paymentMethod)"
            )

            val stockMovements = cartItems.map {
                StockMovement(
                    movementId = UUID.randomUUID().toString(),
                    shopId = shopId,
                    itemId = it.item.itemId,
                    quantity = -it.quantity,
                    movementType = "SALE",
                    referenceId = orderId
                )
            }

            repository.completeOrder(order, orderItems, cashbookEntry, stockMovements)
            _cart.value = emptyList()
        }
    }
}

data class CartItem(
    val item: Item,
    val price: Double,
    val quantity: Double
)
