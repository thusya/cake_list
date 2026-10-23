package com.thusee.cake.data.repository

import com.thusee.cake.data.api.CakeApi
import com.thusee.cake.data.model.CakeDto
import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.model.CakeFailure
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CakeRepositoryTests {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: CakeApi
    private lateinit var repository: CakeRepositoryImpl
    private val banana = Cake("Banana", "Soft sponge", "https://example.com/banana.jpg")

    @BeforeEach
    fun setUp() {
        api = mockk()
        repository = CakeRepositoryImpl(api, dispatcher)
    }

    @Test
    fun `repository maps DTO and classifies network failures`() = runTest(dispatcher) {
        coEvery { api.getCakes() } returns listOf(CakeDto(" Banana ", "Soft sponge", banana.imageUrl))
        assertEquals(listOf(banana), repository.getCakes())
        coEvery { api.getCakes() } throws IOException("offline")
        try { repository.getCakes(); fail<Unit>("Expected network failure") }
        catch (error: CakeFailure.Network) { assertInstanceOf(IOException::class.java, generateSequence<Throwable>(error) { it.cause }.last()) }
    }

    @Test
    fun `nullable DTO fields map directly with empty defaults`() = runTest(dispatcher) {
        coEvery { api.getCakes() } returns listOf(CakeDto(null, " description ", null))
        assertEquals(listOf(Cake("", "description", "")), repository.getCakes())
    }

    @Test
    fun `cancellation propagates through repository`() = runTest(dispatcher) {
        val cancelled = CancellationException("cancelled")
        coEvery { api.getCakes() } throws cancelled
        try { repository.getCakes(); fail("Expected cancellation") }
        catch (error: CancellationException) { assertEquals(cancelled.message, error.message) }
    }
}
