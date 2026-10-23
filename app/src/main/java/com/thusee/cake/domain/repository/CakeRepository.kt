package com.thusee.cake.domain.repository

import com.thusee.cake.domain.model.Cake

interface CakeRepository {
    suspend fun getCakes(): List<Cake>
}