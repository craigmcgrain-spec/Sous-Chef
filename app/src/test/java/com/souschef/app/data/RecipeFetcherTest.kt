package com.souschef.app.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecipeFetcherTest {

    private lateinit var server: MockWebServer
    private lateinit var fetcher: RecipeFetcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        fetcher = RecipeFetcher()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns html`() = runBlocking {
        server.enqueue(MockResponse().setBody("<html></html>").setHeader("Content-Type", "text/html"))
        assertEquals("<html></html>", fetcher.fetch(server.url("/").toString()))
    }

    @Test
    fun `404 maps to HTTP with status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertKind(FetchError.HTTP, status = 404)
    }

    @Test
    fun `redirect to 404 maps to HTTP`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/missing"))
        server.enqueue(MockResponse().setResponseCode(404))
        assertKind(FetchError.HTTP, status = 404)
    }

    @Test
    fun `pdf maps to NOT_HTML`() = runBlocking {
        server.enqueue(MockResponse().setBody("%PDF").setHeader("Content-Type", "application/pdf"))
        assertKind(FetchError.NOT_HTML)
    }

    @Test
    fun `body over 8 MB maps to TOO_LARGE`() = runBlocking {
        // OkHttp 4.12's MockResponse has no setBody(ByteArray); the Buffer overload is equivalent.
        server.enqueue(MockResponse().setBody(Buffer().write(ByteArray(8 * 1024 * 1024 + 1))))
        assertKind(FetchError.TOO_LARGE)
    }

    @Test
    fun `sends a browser user agent`() = runBlocking {
        server.enqueue(MockResponse().setBody("<html></html>").setHeader("Content-Type", "text/html"))
        fetcher.fetch(server.url("/").toString())
        val ua = server.takeRequest().getHeader("User-Agent")!!
        assertTrue(ua.contains("Mozilla/5.0") && ua.contains("Chrome"))
    }

    private suspend fun assertKind(kind: FetchError, status: Int? = null) {
        val error = try {
            fetcher.fetch(server.url("/").toString())
            null
        } catch (e: FetchException) {
            e
        }
        assertNotNull("expected FetchException", error)
        assertEquals(kind, error!!.kind)
        assertEquals(status, error.status)
    }
}
