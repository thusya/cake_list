package com.thusee.cake.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CakeDto(
    @SerialName("title")
    val title: String? = null,
    @SerialName("desc")
    val description: String? = null,
    @SerialName("image")
    val image: String? = null,
)
