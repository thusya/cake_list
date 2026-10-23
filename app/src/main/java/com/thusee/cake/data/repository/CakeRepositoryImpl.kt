package com.thusee.cake.data.repository

import com.thusee.cake.data.api.CakeApi
import com.thusee.cake.di.IoDispatcher
import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.model.CakeFailure
import com.thusee.cake.domain.repository.CakeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class CakeRepositoryImpl @Inject constructor(
    private val api: CakeApi,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : CakeRepository {
    override suspend fun getCakes(): List<Cake> = withContext(dispatcher) {
        try {
            api.getCakes().map { dto ->
                Cake(
                    title = dto.title.orEmpty().trim(),
                    description = dto.description.orEmpty().trim(),
                    imageUrl = dto.image.orEmpty().trim(),
                )
            }
        } catch (error: HttpException) {
            throw CakeFailure.Server(error.code(), error)
        } catch (error: SerializationException) {
            throw CakeFailure.InvalidData(error)
        } catch (error: IOException) {
            throw CakeFailure.Network(error)
        }
    }
}