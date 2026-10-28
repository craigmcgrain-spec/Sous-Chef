package com.souschef.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

fun interface HtmlFetcher {
    suspend fun fetch(url: String): String
}

enum class FetchError { OFFLINE, TIMEOUT, HTTP, NOT_HTML, TOO_LARGE }

class FetchException(
    val kind: FetchError,
    val status: Int? = null,
    cause: Throwable? = null,
) : IOException(kind.name, cause)

private const val MAX_BYTES = 8 * 1024 * 1024
private const val TIMEOUT_SECONDS = 15L
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/126.0.0.0 Mobile Safari/537.36"
private val HTML_TYPES = setOf("text/html", "application/xhtml+xml")

private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

class RecipeFetcher(private val client: OkHttpClient = defaultClient()) : HtmlFetcher {

    override suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw FetchException(FetchError.HTTP, status = response.code)
                }
                // Size before type: a declared oversize body is rejected without reading it.
                val declared = response.header("Content-Length")?.toLongOrNull()
                if (declared != null && declared > MAX_BYTES) {
                    throw FetchException(FetchError.TOO_LARGE)
                }
                val contentType = response.header("Content-Type")
                val mediaType = contentType?.toMediaTypeOrNull()
                if (mediaType != null && mediaType.type + "/" + mediaType.subtype !in HTML_TYPES) {
                    throw FetchException(FetchError.NOT_HTML)
                }
                val stream = response.body?.byteStream() ?: return@withContext ""
                val bytes = readCapped(stream)
                val text = bytes.toString(mediaType?.charset(UTF_8) ?: UTF_8)
                // No Content-Type: accept only when the body starts with a tag.
                if (mediaType == null && text.firstOrNull { !it.isWhitespace() } != '<') {
                    throw FetchException(FetchError.NOT_HTML)
                }
                text
            }
        } catch (e: FetchException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw FetchException(FetchError.TIMEOUT, cause = e)
        } catch (e: InterruptedIOException) {
            // OkHttp's whole-call timeout; SocketTimeoutException is caught above first.
            throw FetchException(FetchError.TIMEOUT, cause = e)
        } catch (e: IOException) {
            throw FetchException(FetchError.OFFLINE, cause = e)
        }
    }

    private fun readCapped(stream: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BYTES) throw FetchException(FetchError.TOO_LARGE)
        }
        return out.toByteArray()
    }
}
