package com.thusee.cake.ui.cake

import com.thusee.cake.domain.model.CakeFailure

enum class CakeUiError {
    Network,
    Server,
    InvalidData,
}

internal fun CakeFailure.toUiError(): CakeUiError = when (this) {
    is CakeFailure.Network -> CakeUiError.Network
    is CakeFailure.Server -> CakeUiError.Server
    is CakeFailure.InvalidData -> CakeUiError.InvalidData
}
