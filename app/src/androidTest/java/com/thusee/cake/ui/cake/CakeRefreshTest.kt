package com.thusee.cake.ui.cake

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import com.thusee.cake.domain.model.Cake
import com.thusee.cake.domain.repository.CakeRepository
import com.thusee.cake.domain.usecase.GetCakesUseCase
import com.thusee.cake.ui.theme.CakeTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test

class CakeRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test fun refreshAfterScrollingAndShrinkingDatasetKeepsListUsable() {
        val requests = AtomicInteger()
        val repository = object : CakeRepository {
            override suspend fun getCakes(): List<Cake> {
                val size = if (requests.incrementAndGet() == 1) 24 else 6
                return (1..size).map { Cake("Cake %02d".format(it), "Description $it", "") }
            }
        }
        val viewModel = CakeViewModel(GetCakesUseCase(repository))
        compose.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val selected by viewModel.selectedCake.collectAsStateWithLifecycle()
            CakeTheme {
                CakeScreen(state, selected, viewModel::refresh, viewModel::retry,
                    viewModel::selectCake, viewModel::dismissCake)
            }
        }
        compose.waitUntil(10_000) { requests.get() == 1 }
        compose.onNodeWithText("Cake 01").assertIsDisplayed()
        // Refresh from a scrolled position into a smaller collection.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Cake 12"))
        compose.waitForIdle()
        compose.onNodeWithText("Refresh").performClick()
        compose.waitUntil(10_000) { requests.get() == 2 }
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Cake 01"))
        compose.onNodeWithText("Cake 01").assertIsDisplayed()
        repeat(3) {
            compose.onNodeWithText("Refresh").performClick()
            compose.waitUntil(10_000) { requests.get() >= it + 3 }
            compose.waitForIdle()
        }
        compose.onNodeWithText("Cake 01").performClick()
        compose.onNodeWithText("Description 1").assertIsDisplayed()
    }
}
