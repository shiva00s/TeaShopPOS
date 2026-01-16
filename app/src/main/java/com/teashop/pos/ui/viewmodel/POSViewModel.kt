package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.OrderWithItems
import com.teashop.pos.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CartItem(
    val item: Item,
    val price: Double,
    val quantity: Double,
    val parcelCharge: Double = 0.0
)

@HiltViewModel
class POSViewModel @Inject constructor(private val repository: MainRepository) : ViewModel() {

    private val _currentShopId = MutableStateFlow<String?>(null)
    val currentShopId: StateFlow<String?> = _currentShopId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val heldOrders: LiveData<List<Order>> = _currentShopId.flatMapLatest { shopId ->
        repository.getHeldOrders(shopId ?: "")
    }.asLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val heldOrdersWithItems: LiveData<List<OrderWithItems>> = _currentShopId.flatMapLatest { shopId ->
        repository.getHeldOrdersWithItems(shopId ?: "")
    }.asLiveData()

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
        _tableId.value = table
        _serviceType.value = type
        
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

    fun holdOrder() {
        val shopId = _currentShopId.value ?: return
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            val orderId = UUID.randomUUID().toString()
            val totalAmount = cartItems.sumOf { (it.price * it.quantity) + it.parcelCharge }
            
            val order = Order(
                orderId = orderId,
                shopId = shopId,
                tableId = _tableId.value,
                serviceType = _serviceType.value,
                totalAmount = totalAmount,
                payableAmount = totalAmount,
                paymentStatus = "PENDING",
                paymentMethod = "",
                status = "HOLD",
                closedAt = null
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
            
            _cart.value = emptyList()
            _transactionComplete.value = true
        }
    }

    fun deleteHeldOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    fun clearAllHeldOrders() {
        viewModelScope.launch {
            heldOrdersWithItems.value?.forEach { orderWithItems ->
                repository.deleteOrder(orderWithItems.order)
            }
        }
    }

    fun loadHeldOrder(order: Order) {
        viewModelScope.launch {
            repository.getOrderWithItems(order.orderId).collect { orderWithItems ->
                val cartItems = orderWithItems.items.mapNotNull { item ->
                    val shopMenuItem = _menuItems.value.find { menuItem -> menuItem.item.itemId == item.itemId }
                    shopMenuItem?.let { CartItem(it.item, item.unitPrice, item.quantity, item.parcelCharge) }
                }
                _cart.value = cartItems
                _tableId.value = order.tableId
                _serviceType.value = order.serviceType
            }
        }
    }

    fun checkoutSplit(payments: Map<String, Double>, serviceType: String, tableId: String?) {
        val shopId = _currentShopId.value ?: return
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            val orderId = UUID.randomUUID().toString()
            val totalAmount = cartItems.sumOf { (it.price * it.quantity) + it.parcelCharge }
            val cashAmt = payments["CASH"] ?: 0.0
            val qrAmt = payments["QR"] ?: 0.0
            
            val method = when {
                cashAmt > 0 && qrAmt > 0 -> "SPLIT"
                cashAmt > 0 -> "CASH"
                qrAmt > 0 -> "ONLINE"
                else -> "CASH"
            }
            
            val order = Order(
                orderId = orderId,
                shopId = shopId,
                tableId = tableId,
                serviceType = serviceType,
                totalAmount = totalAmount,
                payableAmount = totalAmount,
                paymentStatus = "PAID",
                paymentMethod = method,
                cashAmount = cashAmt,
                onlineAmount = qrAmt,
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

            _cart.value = emptyList()
            _transactionComplete.value = true
        }
    }

    fun onTransactionComplete() {
        _transactionComplete.value = false
    }
}
