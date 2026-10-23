package com.thusee.cake.domain.model

sealed class CakeFailure(cause: Throwable? = null) : Exception(cause) {
    class Network(cause: Throwable) : CakeFailure(cause)
    class Server(val status: Int, cause: Throwable) : CakeFailure(cause)
    class InvalidData(cause: Throwable? = null) : CakeFailure(cause)
}
