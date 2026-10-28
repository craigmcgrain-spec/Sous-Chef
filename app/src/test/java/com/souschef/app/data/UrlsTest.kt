package com.souschef.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlsTest {

    @Test
    fun `extractSharedUrl takes first link and strips punctuation`() {
        assertEquals("https://x.com/a", extractSharedUrl("look https://x.com/a. and https://y.com"))
    }

    @Test
    fun `extractSharedUrl returns null without a link`() {
        assertNull(extractSharedUrl("just words"))
    }

    @Test
    fun `normalizeUrl lowercases host and drops fragment`() {
        assertEquals("https://example.com/r?x=1", normalizeUrl("HTTPS://Example.COM/r?x=1#top"))
    }

    @Test
    fun `domainOf returns the host`() {
        assertEquals("example.com", domainOf("https://Example.COM/r"))
    }
}
