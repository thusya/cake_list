package com.thusee.cake.ui.cake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.model.CakeFailure
import com.thusee.cake.domain.usecase.GetCakesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class CakeViewModel @Inject constructor(private val getCakes: GetCakesUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow<CakeUiState>(CakeUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var requestInFlight = false

    private val _selectedCake = MutableStateFlow<Cake?>(null)
    val selectedCake = _selectedCake.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (requestInFlight) return
        requestInFlight = true
        _uiState.update { state ->
            when (state) {
                is CakeUiState.Content -> state.copy(isRefreshing = true, refreshError = null)
                else -> CakeUiState.Loading
            }
        }
        viewModelScope.launch {
            try {
                val cakes = getCakes().toPersistentList()
                _uiState.value = if (cakes.isEmpty()) CakeUiState.Empty else CakeUiState.Content(cakes)
            } catch (failure: CakeFailure) {
                _uiState.update { state ->
                    when (state) {
                        is CakeUiState.Content -> state.copy(
                            isRefreshing = false, refreshError = failure.toUiError(),
                        )
                        else -> CakeUiState.Failure(failure.toUiError())
                    }
                }
            } finally {
                requestInFlight = false
            }
        }
    }

    fun retry() = refresh()
    fun selectCake(cake: Cake) { _selectedCake.value = cake }
    fun dismissCake() { _selectedCake.value = null }
}
