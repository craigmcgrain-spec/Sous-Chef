package com.souschef.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souschef.app.data.ExtractionException
import com.souschef.app.data.FetchError
import com.souschef.app.data.FetchException
import com.souschef.app.data.HtmlFetcher
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.domainOf
import com.souschef.app.data.extractSharedUrl
import com.souschef.app.data.toRecipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ShareUiState {
    data object Idle : ShareUiState
    data class Loading(val domain: String) : ShareUiState
    data class Failure(val url: String, val message: String, val canRetry: Boolean) : ShareUiState
    data class Done(val recipeId: String) : ShareUiState
}

private const val NO_LINK = "That share didn't include a link."
private const val OFFLINE = "No internet connection. Connect and try again."
private const val NO_RESPONSE = "The site didn't respond. Try again in a moment."
private const val NOT_A_RECIPE = "That link isn't a recipe page."
private const val NO_RECIPE_MESSAGE = "No recipe found on that page."
private const val READ_FAILED = "Something went wrong reading that page."

class ShareViewModel(
    private val fetcher: HtmlFetcher,
    private val extractor: RecipeExtractor,
    private val store: RecipeStore,
) : ViewModel() {

    private val _state = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    private var lastUrl: String? = null
    private var job: Job? = null

    fun onShareText(text: String?) {
        job?.cancel()
        lastUrl = null
        val url = extractSharedUrl(text)
        if (url == null) {
            _state.value = ShareUiState.Failure("", NO_LINK, canRetry = false)
            return
        }
        load(url)
    }

    fun retry() {
        lastUrl?.let(::load)
    }

    fun dismissFailure() {
        _state.value = ShareUiState.Idle
    }

    fun consumeDone() {
        _state.value = ShareUiState.Idle
    }

    private fun load(url: String) {
        lastUrl = url
        job?.cancel()
        job = viewModelScope.launch {
            store.findByUrl(url)?.let {
                _state.value = ShareUiState.Done(it.id)
                return@launch
            }
            _state.value = ShareUiState.Loading(domainOf(url))
            try {
                val html = fetcher.fetch(url)
                val extracted = extractor.extract(html, url)
                val recipe = extracted.toRecipe(UUID.randomUUID().toString(), url, System.currentTimeMillis())
                store.save(recipe)
                _state.value = ShareUiState.Done(recipe.id)
            } catch (e: FetchException) {
                _state.value = ShareUiState.Failure(url, fetchMessage(e), canRetry = e.canRetry)
            } catch (e: ExtractionException) {
                _state.value = ShareUiState.Failure(url, extractionMessage(e.code), canRetry = e.code != "NO_RECIPE")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ShareUiState.Failure(url, READ_FAILED, canRetry = true)
            }
        }
    }
}

private val FetchException.canRetry: Boolean
    get() = kind != FetchError.NOT_HTML && kind != FetchError.TOO_LARGE

private fun fetchMessage(e: FetchException): String = when (e.kind) {
    FetchError.OFFLINE -> OFFLINE
    FetchError.TIMEOUT -> NO_RESPONSE
    FetchError.NOT_HTML, FetchError.TOO_LARGE -> NOT_A_RECIPE
    FetchError.HTTP -> if (e.status == null || e.status >= 500) NO_RESPONSE else "That page isn't available (error ${e.status})."
}

private fun extractionMessage(code: String): String =
    if (code == "NO_RECIPE") NO_RECIPE_MESSAGE else READ_FAILED
