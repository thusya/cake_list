package com.thusee.cake.domain.usecase

import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.repository.CakeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetCakesUseCaseTests {
    private lateinit var repository: CakeRepository
    private lateinit var useCase: GetCakesUseCase
    private val banana = Cake(
        title = "Banana",
        description = "Soft sponge",
        imageUrl = "https://example.com/banana.jpg"
    )
    private val carrot = Cake(
        title = "Carrot",
        description = "Spiced",
        imageUrl = "https://example.com/carrot.jpg"
    )

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetCakesUseCase(repository)
    }

    @Test
    fun `use case removes full duplicates and sorts independently of source order`() = runTest {
        val differentBanana = banana.copy(description = "Different recipe")
        coEvery { repository.getCakes() } returns listOf(carrot, banana, carrot, differentBanana)

        assertEquals(listOf(differentBanana, banana, carrot), useCase())
        coVerify(exactly = 1) { repository.getCakes() }
    }

    @Test
    fun `cancellation propagates through use case`() = runTest {
        val cancelled = CancellationException("cancelled")
        coEvery { repository.getCakes() } throws cancelled

        try {
            useCase(); fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertEquals(cancelled.message, error.message)
        }
    }
}
