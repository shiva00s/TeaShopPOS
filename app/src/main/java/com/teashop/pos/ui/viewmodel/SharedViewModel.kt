package com.teashop.pos.ui.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedViewModel @Inject constructor() {

    private val _shopProfits = MutableStateFlow<Map<String, Double>>(emptyMap())
    val shopProfits: StateFlow<Map<String, Double>> = _shopProfits

    private val _recalculate = MutableSharedFlow<Unit>()
    val recalculate: SharedFlow<Unit> = _recalculate

    suspend fun triggerRecalculate() {
        _recalculate.emit(Unit)
    }

    fun updateShopProfit(shopId: String, profit: Double) {
        val updatedProfits = _shopProfits.value.toMutableMap()
        updatedProfits[shopId] = profit
        _shopProfits.value = updatedProfits
    }
}
