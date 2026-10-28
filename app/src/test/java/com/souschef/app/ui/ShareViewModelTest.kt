package com.souschef.app.ui

import com.souschef.app.MainDispatcherRule
import com.souschef.app.data.ExtractedRecipe
import com.souschef.app.data.ExtractionException
import com.souschef.app.data.FetchError
import com.souschef.app.data.FetchException
import com.souschef.app.data.HtmlFetcher
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
import com.souschef.app.data.Temperature
import com.souschef.app.sampleRecipe
import com.souschef.app.ui.ShareUiState.Done
import com.souschef.app.ui.ShareUiState.Failure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val URL = "https://example.com/recipe"
private val sampleExtraction = ExtractedRecipe(
    title = "Sunday Ragù",
    siteName = "BBC Good Food",
    ingredients = listOf("2 tbsp olive oil"),
    steps = listOf("Heat the oven to 160°C."),
    temperatures = listOf(Temperature(160, 320, "160°C", "Heat the oven to 160°C.", 0)),
)

private class FakeFetcher : HtmlFetcher {
    var html = ""
    var error: FetchException? = null
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null
    var calls = 0

    override suspend fun fetch(url: String): String {
        calls++
        gate?.await()
        failure?.let { throw it }
        error?.let { throw it }
        return html
    }
}

private class FakeExtractor : RecipeExtractor {
    var recipe = sampleExtraction
    var error: ExtractionException? = null

    override suspend fun extract(html: String, url: String): ExtractedRecipe {
        error?.let { throw it }
        return recipe
    }

    override suspend fun temperatures(texts: List<String>): List<Temperature> = emptyList()
}

private class FakeStore : RecipeStore {
    val saved = mutableListOf<Recipe>()
    private val seeded = MutableStateFlow<List<Recipe>>(emptyList())

    fun seed(recipe: Recipe) {
        seeded.value = seeded.value + recipe
    }

    override val recipes: Flow<List<Recipe>> = seeded
    override val shopping: Flow<List<ShoppingItem>> = MutableStateFlow(emptyList())

    override suspend fun save(recipe: Recipe) {
        saved += recipe
        seeded.value = seeded.value + recipe
    }

    override suspend fun findByUrl(url: String): Recipe? = seeded.value.firstOrNull { it.url == url }

    override suspend fun update(recipe: Recipe) = error("not used")
    override suspend fun update(id: String, transform: (Recipe) -> Recipe) = error("not used")
    override suspend fun delete(id: String) = error("not used")
    override suspend fun addToShopping(recipe: Recipe, texts: List<String>) = error("not used")
    override suspend fun toggleShoppingItem(id: String) = error("not used")
    override suspend fun clearCheckedShopping() = error("not used")
    override suspend fun clearAllShopping() = error("not used")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fetcher = FakeFetcher()
    private val extractor = FakeExtractor()
    private val store = FakeStore()
    private val vm = ShareViewModel(fetcher, extractor, store)

    @Test
    fun `no link fails without fetching`() = runTest {
        vm.onShareText("words")
        advanceUntilIdle()
        assertEquals(Failure("", "That share didn't include a link.", false), vm.state.value)
        assertEquals(0, fetcher.calls)
    }

    @Test
    fun `superseded share cannot overwrite no link failure`() = runTest {
        fetcher.gate = CompletableDeferred()
        vm.onShareText(URL)
        runCurrent()
        vm.onShareText("words")
        advanceUntilIdle()
        assertEquals(Failure("", "That share didn't include a link.", false), vm.state.value)
        vm.retry()
        advanceUntilIdle()
        assertEquals(1, fetcher.calls)
    }

    @Test
    fun `duplicate opens saved copy`() = runTest {
        store.seed(sampleRecipe("id-9", URL))
        vm.onShareText("$URL")
        advanceUntilIdle()
        assertEquals(Done("id-9"), vm.state.value)
        assertEquals(0, fetcher.calls)
    }

    @Test
    fun `fetch error maps to copy`() = runTest {
        fetcher.error = FetchException(FetchError.HTTP, 404)
        vm.onShareText(URL)
        advanceUntilIdle()
        assertEquals("That page isn't available (error 404).", (vm.state.value as Failure).message)
    }

    @Test
    fun `server error uses timeout copy`() = runTest {
        fetcher.error = FetchException(FetchError.HTTP, 503)
        vm.onShareText(URL)
        advanceUntilIdle()
        assertEquals("The site didn't respond. Try again in a moment.", (vm.state.value as Failure).message)
    }

    @Test
    fun `unexpected failure uses fallback copy`() = runTest {
        fetcher.failure = IllegalArgumentException("unexpected url")
        vm.onShareText(URL)
        advanceUntilIdle()
        assertEquals(Failure(URL, "Something went wrong reading that page.", true), vm.state.value)
    }

    @Test
    fun `no recipe maps to copy`() = runTest {
        fetcher.html = "<html></html>"
        extractor.error = ExtractionException("NO_RECIPE")
        vm.onShareText(URL)
        advanceUntilIdle()
        assertEquals("No recipe found on that page.", (vm.state.value as Failure).message)
    }

    @Test
    fun `success saves once and emits Done`() = runTest {
        fetcher.html = "<html></html>"
        extractor.recipe = sampleExtraction
        vm.onShareText(URL)
        advanceUntilIdle()
        assertEquals(1, store.saved.size)
        assertEquals(Done(store.saved.single().id), vm.state.value)
    }

    @Test
    fun `retry refetches the last url`() = runTest {
        fetcher.error = FetchException(FetchError.OFFLINE)
        vm.onShareText(URL)
        advanceUntilIdle()
        assertEquals("No internet connection. Connect and try again.", (vm.state.value as Failure).message)
        fetcher.error = null
        vm.retry()
        advanceUntilIdle()
        assertEquals(2, fetcher.calls)
    }
}
