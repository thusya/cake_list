package com.thusee.cake.data.api

import com.thusee.cake.data.repository.CakeRepositoryImpl
import com.thusee.cake.domain.model.CakeFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import com.thusee.cake.di.CakeModule
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class CakeApiTests {
    @Test
    fun `API decodes desc field and requests the exercise path`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""[{"title":"Cake","desc":"A treat","image":"https://example.com/cake.jpg"}]"""))
            val cake = repository(server).getCakes().single()
            assertEquals("A treat", cake.description)
            assertEquals(
                "/Waracle/mobile-coding-test-api/refs/heads/main/cakes",
                server.takeRequest().path
            )
        }
    }

    @Test
    fun `HTTP status is retained as a server failure`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            try {
                repository(server).getCakes(); fail("Expected server failure")
            } catch (error: CakeFailure.Server) {
                assertEquals(503, error.status)
            }
        }
    }

    @Test
    fun `malformed JSON is invalid data`() = runTest {
        MockWebServer().use { server ->
            val repo = repository(server)
            for (body in listOf("not-json", "[null]", "[{", "[{\"title\":{}}]")) {
                server.enqueue(MockResponse().setBody(body))
                try {
                    repo.getCakes(); fail("Expected invalid data for $body")
                } catch (_: CakeFailure.InvalidData) {
                }
            }
        }
    }

    @Test
    fun `unknown fields do not break decoding`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""[{"title":"Cake","desc":"A treat","image":"https://example.com/cake.jpg","newField":42}]"""))
            assertEquals("Cake", repository(server).getCakes().single().title)
        }
    }

    @Test
    fun `missing and null fields use empty defaults`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""[{}, {"title":null,"desc":null,"image":null}]"""))
            val cakes = repository(server).getCakes()
            assertEquals(2, cakes.size)
            assertTrue(cakes.all { it.title.isEmpty() && it.description.isEmpty() && it.imageUrl.isEmpty() })
        }
    }

    private fun repository(server: MockWebServer): CakeRepositoryImpl {
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(CakeModule.provideJson().asConverterFactory("application/json".toMediaType())).build().create(CakeApi::class.java)
        return CakeRepositoryImpl(api, Dispatchers.IO)
    }
}
