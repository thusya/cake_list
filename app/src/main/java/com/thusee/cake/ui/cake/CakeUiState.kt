package com.thusee.cake.ui.cake

import androidx.compose.runtime.Immutable
import com.thusee.cake.domain.model.Cake
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface CakeUiState {
    data object Loading : CakeUiState
    data object Empty : CakeUiState

    data class Content(
        val cakes: ImmutableList<Cake>,
        val isRefreshing: Boolean = false,
        val refreshError: CakeUiError? = null,
    ) : CakeUiState

    data class Failure(val error: CakeUiError) : CakeUiState
}
