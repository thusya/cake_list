package com.thusee.cake.domain.model

data class Cake(
    val title: String,
    val description: String,
    val imageUrl: String
) {
    val id: String get() = "$title\u0000$description\u0000$imageUrl"
}
