package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.ProductItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel para la gestión reactiva del carrito de compras y cupones de descuento.
 * Cumple con REQ-SEM06-LOG-01 y REQ-SEM06-LOG-02.
 */
class CartViewModel : ViewModel() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<String?>(null)
    val appliedCoupon: StateFlow<String?> = _appliedCoupon.asStateFlow()

    private val _couponDiscountPercent = MutableStateFlow(0.0)
    val couponDiscountPercent: StateFlow<Double> = _couponDiscountPercent.asStateFlow()

    fun addToCart(product: ProductItem) {
        _cartItems.update { current ->
            val index = current.indexOfFirst { it.product.id == product.id }
            if (index != -1) {
                current.mapIndexed { idx, item ->
                    if (idx == index) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                current + CartItem(product = product, quantity = 1)
            }
        }
    }

    fun increaseQuantity(productId: Int) {
        _cartItems.update { current ->
            current.map {
                if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
            }
        }
    }

    fun decreaseQuantity(productId: Int) {
        _cartItems.update { current ->
            current.mapNotNull {
                if (it.product.id == productId) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else it
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _appliedCoupon.value = null
        _couponDiscountPercent.value = 0.0
    }

    fun applyCoupon(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        return when (cleanCode) {
            "FOODJET10" -> {
                _appliedCoupon.value = cleanCode
                _couponDiscountPercent.value = 10.0
                true
            }
            "ESTUDIANTE20" -> {
                _appliedCoupon.value = cleanCode
                _couponDiscountPercent.value = 20.0
                true
            }
            else -> false
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
        _couponDiscountPercent.value = 0.0
    }

    fun getSubtotal(isStudent: Boolean): Double {
        return _cartItems.value.sumOf { it.product.getEffectivePrice(isStudent) * it.quantity }
    }

    fun getCouponDiscountAmount(isStudent: Boolean): Double {
        val subtotal = getSubtotal(isStudent)
        return subtotal * (_couponDiscountPercent.value / 100.0)
    }

    fun getTaxAmount(isStudent: Boolean): Double {
        val base = getSubtotal(isStudent) - getCouponDiscountAmount(isStudent)
        return if (base > 0) base * 0.18 else 0.0
    }

    fun getDeliveryFee(): Double = 5.00

    fun getTotal(isStudent: Boolean): Double {
        if (_cartItems.value.isEmpty()) return 0.0
        val subtotal = getSubtotal(isStudent)
        val discount = getCouponDiscountAmount(isStudent)
        val tax = getTaxAmount(isStudent)
        return (subtotal - discount + tax + getDeliveryFee()).coerceAtLeast(0.0)
    }
}
