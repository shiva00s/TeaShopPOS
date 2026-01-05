package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.dao.ShopMenuItem
import com.teashop.pos.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class POSViewModel(private val repository: MainRepository) : ViewModel() {

    private val _currentShopId = MutableStateFlow<String?>(null)
    val currentShopId: StateFlow<String?> = _currentShopId.asStateFlow()

    private val _menuItems = MutableStateFlow<List<ShopMenuItem>>(emptyList())
    val menuItems: StateFlow<List<ShopMenuItem>> = _menuItems.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _serviceType = MutableStateFlow("STANDING")
    val serviceType: StateFlow<String> = _serviceType.asStateFlow()

    private val _tableId = MutableStateFlow<String?>(null)
    val tableId: StateFlow<String?> = _tableId.asStateFlow()

    private val _transactionComplete = MutableStateFlow(false)
    val transactionComplete: StateFlow<Boolean> = _transactionComplete.asStateFlow()

    fun setShop(shopId: String) {
        _currentShopId.value = shopId
        viewModelScope.launch {
            repository.getShopMenu(shopId).collect {
                _menuItems.value = it
            }
        }
    }

    fun setServiceType(type: String, table: String? = null) {
        _serviceType.value = type
        _tableId.value = table
        
        val currentList = _cart.value.map { item ->
            if (type == "PARCEL" && item.item.hasParcelCharge) {
                item.copy(parcelCharge = item.item.defaultParcelCharge)
            } else {
                item.copy(parcelCharge = 0.0)
            }
        }
        _cart.value = currentList
    }

    fun addToCart(menuItem: ShopMenuItem) {
        val currentList = _cart.value.toMutableList()
        val existing = currentList.find { it.item.itemId == menuItem.item.itemId }
        
        if (existing != null) {
            val index = currentList.indexOf(existing)
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            val initialParcelCharge = if (_serviceType.value == "PARCEL" && menuItem.item.hasParcelCharge) {
                menuItem.item.defaultParcelCharge
            } else 0.0
            currentList.add(CartItem(menuItem.item, menuItem.finalPrice, 1.0, initialParcelCharge))
        }
        _cart.value = currentList
    }

    fun updateParcelCharge(cartItem: CartItem, newCharge: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.item.itemId == cartItem.item.itemId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(parcelCharge = newCharge)
            _cart.value = currentList
        }
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

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun checkoutSplit(payments: Map<String, Double>, serviceType: String, tableId: String?) {
        val shopId = _currentShopId.value ?: return
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            val orderId = UUID.randomUUID().toString()
            val totalAmount = cartItems.sumOf { (it.price * it.quantity) + it.parcelCharge }
            
            val order = Order(
                orderId = orderId,
                shopId = shopId,
                tableId = tableId,
                serviceType = serviceType,
                totalAmount = totalAmount,
                payableAmount = totalAmount,
                paymentStatus = "PAID",
                paymentMethod = "SPLIT",
                status = "CLOSED",
                closedAt = System.currentTimeMillis()
            )

            repository.insertOrder(order)

            cartItems.forEach {
                repository.insertOrderItem(OrderItem(
                    orderItemId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    itemId = it.item.itemId,
                    itemName = it.item.name,
                    quantity = it.quantity,
                    unitPrice = it.price,
                    parcelCharge = it.parcelCharge,
                    subTotal = (it.price * it.quantity) + it.parcelCharge
                ))
            }

            payments.filter { it.value > 0 }.forEach { (mode, amount) ->
                repository.insertCashbookEntry(Cashbook(
                    entryId = UUID.randomUUID().toString(),
                    shopId = shopId,
                    transactionType = "IN",
                    category = if (mode == "CASH") "DAILY SALES (CASH)" else "DAILY SALES (QR)",
                    amount = amount,
                    referenceId = orderId,
                    description = "Split Sale ($mode)",
                    transactionDate = System.currentTimeMillis()
                ))
            }

            cartItems.forEach {
                repository.insertStockMovement(StockMovement(
                    movementId = UUID.randomUUID().toString(),
                    shopId = shopId,
                    itemId = it.item.itemId,
                    quantity = -it.quantity,
                    movementType = "SALE",
                    referenceId = orderId
                ))
            }
            
            _cart.value = emptyList()
            _transactionComplete.value = true
        }
    }

    fun onTransactionComplete() {
        _transactionComplete.value = false
    }
}

data class CartItem(
    val item: Item,
    val price: Double,
    val quantity: Double,
    val parcelCharge: Double = 0.0
)
