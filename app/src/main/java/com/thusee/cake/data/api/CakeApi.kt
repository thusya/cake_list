package com.thusee.cake.data.api

import com.thusee.cake.data.model.CakeDto
import retrofit2.http.GET

interface CakeApi {
    @GET("Waracle/mobile-coding-test-api/refs/heads/main/cakes")
    suspend fun getCakes(): List<CakeDto>
}
