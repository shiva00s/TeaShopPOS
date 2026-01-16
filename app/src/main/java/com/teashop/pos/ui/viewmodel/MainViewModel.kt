package com.teashop.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teashop.pos.data.MainRepository
import com.teashop.pos.data.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository,
    private val sharedViewModel: SharedViewModel
) : ViewModel() {

    val allShops: StateFlow<List<Shop>> = repository.getAllShops().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allFixedExpenses: StateFlow<List<FixedExpense>> = repository.getAllFixedExpenses().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _shopsWithProfit = MutableStateFlow<List<ShopWithProfit>>(emptyList())
    val shopsWithProfit: StateFlow<List<ShopWithProfit>> = _shopsWithProfit.asStateFlow()

    private val _currentPeriod = MutableStateFlow("Up To Date")
    val currentPeriod: StateFlow<String> = _currentPeriod.asStateFlow()

    private val _currentDate = MutableStateFlow(System.currentTimeMillis())

    private val _globalProfit = MutableStateFlow(0.0)
    val globalProfit: StateFlow<Double> = _globalProfit.asStateFlow()

    private val _isSubscribed = MutableStateFlow(true)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    init {
        checkSubscription()
        viewModelScope.launch {
            combine(allShops, sharedViewModel.shopProfits, sharedViewModel.recalculate) { shops, profits, _ ->
                val newShopsWithProfit = shops.map {
                    val profit = profits[it.shopId] ?: 0.0
                    ShopWithProfit(it, profit)
                }
                _shopsWithProfit.value = newShopsWithProfit
                _globalProfit.value = newShopsWithProfit.sumOf { it.profit }
            }.collect()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            sharedViewModel.triggerRecalculate()
        }
    }

    private fun checkSubscription() {
        viewModelScope.launch {
            repository.getProfileFlow().collectLatest { profile ->
                if (profile != null) {
                    val sevenDays = 7 * 24 * 60 * 60 * 1000L
                    val isTrialOver = System.currentTimeMillis() > (profile.joinDate + sevenDays)
                    _isSubscribed.value = profile.isPremium || !isTrialOver
                } else {
                    _isSubscribed.value = true
                }
            }
        }
    }

    fun getShopEmployees(shopId: String): Flow<List<Employee>> {
        return repository.getShopEmployees(shopId)
    }

    fun getAllEmployees(): Flow<List<Employee>> {
        return repository.getAllEmployees()
    }

    fun upgradeToPremium(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun addShop(shop: Shop) {
        viewModelScope.launch { repository.insertShop(shop) }
    }

    fun deleteShop(shop: Shop) {
        viewModelScope.launch { repository.deleteShop(shop) }
    }

    fun addFixedExpense(expense: FixedExpense) {
        viewModelScope.launch { repository.insertFixedExpense(expense) }
    }

    fun updateFixedExpense(expense: FixedExpense, newAmount: Double) {
        viewModelScope.launch {
            val updatedExpense = expense.copy(
                monthlyAmount = newAmount
            )
            repository.insertFixedExpense(updatedExpense)
        }
    }

    fun setFilter(period: String, date: Long = System.currentTimeMillis()) {
        _currentPeriod.value = period
        _currentDate.value = date
    }

    fun addDefaultItems(items: List<Item>) {
        viewModelScope.launch {
            items.forEach { repository.insertItem(it) }
        }
    }

    private fun getTimestampsForPeriod(period: String, date: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        cal.timeInMillis = date

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (period) {
            "Today" -> {}
            "Weekly" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            }
            "Monthly", "Up To Date" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            "Quarterly" -> {
                val quarter = cal.get(Calendar.MONTH) / 3
                cal.set(Calendar.MONTH, quarter * 3)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            "Annually" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return cal.timeInMillis to end
    }

    fun startRealtimeSync() {
        repository.startSync()
    }
}
