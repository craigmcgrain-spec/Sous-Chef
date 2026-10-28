package com.souschef.app.data

import java.net.URI

private val URL_PATTERN = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
private const val TRAILING_PUNCTUATION = ".,;:!?\"')]}>"

/** First http(s) link in shared text, minus the punctuation that ends a sentence. */
fun extractSharedUrl(text: String?): String? = text
    ?.let { URL_PATTERN.find(it)?.value }
    ?.trimEnd { it in TRAILING_PUNCTUATION }
    ?.takeIf { it.isNotEmpty() }

/** Dedup key: scheme and host lowercased, fragment dropped, path and query kept. */
fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return trimmed.substringBefore('#')
    val scheme = uri.scheme ?: return trimmed.substringBefore('#')
    val host = uri.host ?: return trimmed.substringBefore('#')
    val port = if (uri.port == -1) "" else ":${uri.port}"
    val query = uri.rawQuery?.let { "?$it" } ?: ""
    return "${scheme.lowercase()}://${host.lowercase()}$port${uri.rawPath ?: ""}$query"
}

fun domainOf(url: String): String {
    val trimmed = url.trim()
    val host = runCatching { URI(trimmed).host }.getOrNull()
    return host?.lowercase() ?: trimmed.substringAfter("://").substringBefore('/')
}
