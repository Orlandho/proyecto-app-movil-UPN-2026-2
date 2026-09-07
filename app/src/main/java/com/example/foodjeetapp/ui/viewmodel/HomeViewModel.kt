package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.PromotionSlide
import com.example.foodjeetapp.data.repository.ProductRepository
import com.example.foodjeetapp.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla principal (Home / Catálogo de Productos).
 * Cumple con REQ-SEM06-LOG-01, REQ-SEM06-LOG-02 y REQ-SEM06-LOG-04.
 */
class HomeViewModel(
    private val productRepository: ProductRepository = ProductRepositoryImpl()
) : ViewModel() {

    private val _productsState = MutableStateFlow<UiState<List<ProductItem>>>(UiState.Loading)
    val productsState: StateFlow<UiState<List<ProductItem>>> = _productsState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _maxPrice = MutableStateFlow(25f)
    val maxPrice: StateFlow<Float> = _maxPrice.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Int>>(emptySet())
    val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    // Lista de productos filtrada de manera reactiva según búsqueda, categoría y rango de precio
    val filteredProducts: StateFlow<List<ProductItem>> = combine(
        _productsState,
        _selectedCategory,
        _searchQuery,
        _maxPrice
    ) { state, category, query, priceLimit ->
        if (state is UiState.Success) {
            state.data.filter { item ->
                val matchesCategory = if (category == "Todos") true else item.tipoComida.equals(category, ignoreCase = true)
                val matchesSearch = query.isBlank() ||
                        item.nombre.contains(query, ignoreCase = true) ||
                        item.descripcion.contains(query, ignoreCase = true)
                val matchesPrice = item.precio <= priceLimit
                matchesCategory && matchesSearch && matchesPrice
            }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _productsState.value = UiState.Loading
            val result = productRepository.getProducts()
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _productsState.value = UiState.Empty
                } else {
                    _productsState.value = UiState.Success(list)
                }
            }.onFailure { error ->
                _productsState.value = UiState.Error(
                    message = error.message ?: "Error al cargar el catálogo de productos",
                    cause = error
                )
            }
        }
    }

    fun retry() {
        loadProducts()
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMaxPrice(price: Float) {
        _maxPrice.value = price
    }

    fun toggleFavorite(productId: Int) {
        _favorites.update { current ->
            if (current.contains(productId)) current - productId else current + productId
        }
    }

    fun getPromotions(): List<PromotionSlide> {
        return productRepository.getPromotions()
    }
}
