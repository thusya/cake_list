package com.thusee.cake.domain.usecase

import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.repository.CakeRepository
import java.util.Locale

class GetCakesUseCase(private val repository: CakeRepository) {
    suspend operator fun invoke(): List<Cake> = repository.getCakes()
        .distinct()
        .sortedWith(compareBy<Cake> { it.title.lowercase(Locale.ROOT) }
            .thenBy { it.title }
            .thenBy { it.description }
            .thenBy { it.imageUrl })
}