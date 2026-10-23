package com.thusee.cake.ui.cake

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.model.CakeFailure
import com.thusee.cake.domain.usecase.GetCakesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CakeViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val testDispatcher = UnconfinedTestDispatcher(mainDispatcher.scheduler)
    private lateinit var getCakesUseCase: GetCakesUseCase
    private lateinit var viewModel: CakeViewModel
    private val viewModelStore = ViewModelStore()
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
        Dispatchers.setMain(mainDispatcher)
        getCakesUseCase = mockk()
        coEvery { getCakesUseCase() } returns emptyList()
        viewModel = CakeViewModel(getCakesUseCase)
        viewModelStore.put("cake", viewModel)
    }

    @AfterEach
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `startup StateFlow presents cakes returned by the use case`() = runTest(testDispatcher) {
        coEvery { getCakesUseCase() } returns listOf(banana, carrot)
        viewModel.uiState.test {
            assertEquals(CakeUiState.Loading, awaitItem())
            val loaded = assertInstanceOf(CakeUiState.Content::class.java, awaitItem())
            assertFalse(loaded.isRefreshing)
            assertEquals(listOf(banana, carrot), loaded.cakes)
            assertNull(loaded.refreshError)
        }
        coVerify(exactly = 1) { getCakesUseCase() }
    }

    @Test
    fun `startup failure is exposed and retry clears the error`() = runTest(testDispatcher) {
        val failure = CakeFailure.Network(IOException("offline"))
        coEvery { getCakesUseCase() } throws failure andThen listOf(banana)
        viewModel.uiState.test {
            assertEquals(CakeUiState.Loading, awaitItem())
            val failed = assertInstanceOf(CakeUiState.Failure::class.java, awaitItem())
            assertEquals(CakeUiError.Network, failed.error)

            withContext(mainDispatcher) { viewModel.retry() }
            assertEquals(CakeUiState.Loading, awaitItem())
            assertEquals(listOf(banana), assertInstanceOf(CakeUiState.Content::class.java, awaitItem()).cakes)
        }
        coVerify(exactly = 2) { getCakesUseCase() }
    }

    @Test
    fun `server and invalid data failures map to presentation values`() = runTest(testDispatcher) {
        for ((failure, expected) in listOf(
            CakeFailure.Server(503, IOException()) to CakeUiError.Server,
            CakeFailure.InvalidData() to CakeUiError.InvalidData,
        )) {
            coEvery { getCakesUseCase() } throws failure
            if (viewModel.uiState.value != CakeUiState.Loading) viewModel.retry()
            advanceUntilIdle()
            assertEquals(CakeUiState.Failure(expected), viewModel.uiState.value)
        }
    }

    @Test
    fun `refresh retains cakes while loading and on failure`() = runTest(testDispatcher) {
        val response = CompletableDeferred<List<Cake>>()
        coEvery { getCakesUseCase() } returns listOf(banana)
        advanceUntilIdle()
        coEvery { getCakesUseCase() } coAnswers { response.await() }
        viewModel.uiState.test {
            assertEquals(listOf(banana), assertInstanceOf(CakeUiState.Content::class.java, awaitItem()).cakes)
            viewModel.refresh()
            val refreshing = assertInstanceOf(CakeUiState.Content::class.java, awaitItem())
            assertTrue(refreshing.isRefreshing)
            assertEquals(listOf(banana), refreshing.cakes)
            runCurrent()
            response.completeExceptionally(CakeFailure.Network(IOException()))
            val failed = assertInstanceOf(CakeUiState.Content::class.java, awaitItem())
            assertEquals(listOf(banana), failed.cakes)
            assertFalse(failed.isRefreshing)
            assertEquals(CakeUiError.Network, failed.refreshError)
        }
    }

    @Test
    fun `rapid refresh taps make only one request`() = runTest(testDispatcher) {
        val response = CompletableDeferred<List<Cake>>()
        coEvery { getCakesUseCase() } coAnswers { response.await() }
        repeat(5) { viewModel.refresh() }
        runCurrent()
        coVerify(exactly = 1) { getCakesUseCase() }
        response.complete(listOf(banana))
        advanceUntilIdle()
        val content = assertInstanceOf(CakeUiState.Content::class.java, viewModel.uiState.value)
        assertFalse(content.isRefreshing)
        assertEquals(listOf(banana), content.cakes)
    }

    @Test
    fun `successful refresh replaces the collection`() = runTest(testDispatcher) {
        coEvery { getCakesUseCase() } returns listOf(banana, carrot) andThen listOf(carrot)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        val content = assertInstanceOf(CakeUiState.Content::class.java, viewModel.uiState.value)
        assertEquals(listOf(carrot), content.cakes)
        assertNull(content.refreshError)
    }

    @Test
    fun `refreshing content with empty response transitions to Empty`() = runTest(testDispatcher) {
        coEvery { getCakesUseCase() } returns listOf(banana) andThen emptyList()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(CakeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `selection StateFlow emits selection and dismissal`() = runTest(testDispatcher) {
        coEvery { getCakesUseCase() } returns emptyList()
        viewModel.selectedCake.test {
            assertNull(awaitItem())
            viewModel.selectCake(banana)
            assertEquals(banana, awaitItem())
            viewModel.dismissCake()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `empty dataset produces a successful empty state`() = runTest(testDispatcher) {
        coEvery { getCakesUseCase() } returns emptyList()
        advanceUntilIdle()
        assertEquals(CakeUiState.Empty, viewModel.uiState.value)
    }
}
