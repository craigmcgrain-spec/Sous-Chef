package com.souschef.app.data

import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface RecipeExtractor {
    suspend fun extract(html: String, url: String): ExtractedRecipe
    suspend fun temperatures(texts: List<String>): List<Temperature>
}

/** [code] is the Python `EXTRACTION_ERROR` code, or `PARSE_FAILED` for anything unexpected. */
class ExtractionException(val code: String, cause: Throwable? = null) : Exception(code, cause)

fun decodeTemperatures(json: String): List<Temperature> = SousChefJson.decodeFromString(json)

private val ERROR_CODE = Regex("EXTRACTION_ERROR:([A-Z_]+)")

/** Chaquopy is single-threaded, so every Python call is serialized behind one mutex. */
class RecipeParser : RecipeExtractor {
    private val mutex = Mutex()

    override suspend fun extract(html: String, url: String): ExtractedRecipe = call {
        decodeExtractedRecipe(it.callAttr("extract", html, url).toString())
    }

    override suspend fun temperatures(texts: List<String>): List<Temperature> = call {
        decodeTemperatures(it.callAttr("temperatures_from_texts", texts.toTypedArray()).toString())
    }

    private suspend fun <T> call(block: (PyObject) -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                block(Python.getInstance().getModule("souschef.extract"))
            } catch (e: PyException) {
                val code = ERROR_CODE.find(e.message.orEmpty())?.groupValues?.get(1) ?: "PARSE_FAILED"
                throw ExtractionException(code, e)
            }
        }
    }
}
