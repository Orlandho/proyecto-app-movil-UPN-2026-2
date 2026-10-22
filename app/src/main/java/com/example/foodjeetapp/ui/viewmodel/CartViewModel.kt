package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel para la gestión reactiva del carrito de compras.
 * Cumple con REQ-SEM06-LOG-01 y REQ-SEM06-LOG-02.
 */
class CartViewModel : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(product: ProductItem) {
        _cartItems.update { current ->
            val index = current.indexOfFirst { it.product.id == product.id }
            if (index != -1) {
                current.mapIndexed { idx, item ->
                    if (idx == index) item.copy(quantity = item.quantity + 1) else item.copy()
                }
            } else {
                current + CartItem(product = product.copy(), quantity = 1)
            }
        }
    }

    fun increaseQuantity(productId: Int) {
        _cartItems.update { current ->
            current.map {
                if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it.copy()
            }
        }
    }

    fun decreaseQuantity(productId: Int) {
        _cartItems.update { current ->
            current.mapNotNull {
                if (it.product.id == productId) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else it.copy()
            }
        }
    }

    fun removeFromCart(productId: Int) {
        _cartItems.update { current ->
            current.filterNot { it.product.id == productId }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getSubtotal(isStudent: Boolean): Double {
        return _cartItems.value.sumOf { it.product.getEffectivePrice(isStudent) * it.quantity }
    }

    fun getTaxAmount(isStudent: Boolean): Double {
        val base = getSubtotal(isStudent)
        return if (base > 0) base * 0.18 else 0.0
    }

    fun getDeliveryFee(): Double = 5.00

    fun getTotal(isStudent: Boolean): Double {
        if (_cartItems.value.isEmpty()) return 0.0
        return (getSubtotal(isStudent) + getTaxAmount(isStudent) + getDeliveryFee()).coerceAtLeast(0.0)
    }
}
