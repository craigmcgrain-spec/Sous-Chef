package com.souschef.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.souschef.app.sampleRecipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DataStoreRecipeStoreTest {
    private lateinit var file: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: DataStoreRecipeStore

    @Before
    fun setUp() {
        file = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "store-${UUID.randomUUID()}.preferences_pb",
        )
        open()
    }

    @After
    fun tearDown() {
        runBlocking {
            close()
            file.delete()
        }
    }

    private fun open() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        store = DataStoreRecipeStore(dataStore)
    }

    // DataStore refuses two live instances on one file, so the first scope must finish.
    private suspend fun close() {
        scope.cancel()
        scope.coroutineContext[Job]?.join()
    }

    private suspend fun newStore(): RecipeStore {
        close()
        open()
        return store
    }

    @Test fun save_then_findByUrl_normalizes() = runBlocking {
        store.save(sampleRecipe(url = "https://Example.com/r#top"))
        assertEquals("id-1", store.findByUrl("https://example.com/r")?.id)
    }

    @Test fun update_toggle_persists_across_new_instance() = runBlocking {
        val recipe = sampleRecipe()
        store.save(recipe)
        store.update(recipe.copy(steps = recipe.steps.mapIndexed { i, s -> if (i == 0) s.copy(checked = true) else s }))
        val reopened = newStore()      // fresh DataStore over the same temp file
        assertTrue(reopened.recipes.first().first().steps.first().checked)
    }

    @Test fun addToShopping_dedupes_same_recipe_and_text() = runBlocking {
        val recipe = sampleRecipe()
        store.addToShopping(recipe, listOf("2 tbsp olive oil")); store.addToShopping(recipe, listOf("2 tbsp olive oil"))
        assertEquals(1, store.shopping.first().size)
    }

    @Test fun delete_recipe_keeps_shopping_items() = runBlocking {
        val recipe = sampleRecipe()
        store.save(recipe); store.addToShopping(recipe, listOf("1 egg")); store.delete(recipe.id)
        assertTrue(store.recipes.first().isEmpty())
        assertEquals(1, store.shopping.first().size)
    }

    @Test fun clearChecked_removes_only_checked() = runBlocking {
        store.addToShopping(sampleRecipe(), listOf("a", "b"))
        store.toggleShoppingItem(store.shopping.first().first().id)
        store.clearCheckedShopping()
        assertEquals(listOf("b"), store.shopping.first().map { it.text })
    }

    @Test fun corrupt_json_yields_empty_and_keeps_backup() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("recipes")] = "{oops" }
        assertTrue(store.recipes.first().isEmpty())
        assertEquals("{oops", dataStore.data.first()[stringPreferencesKey("recipes_backup")])
    }

    @Test fun recipes_sort_newest_first() = runBlocking {
        store.save(sampleRecipe(id = "old").copy(savedAtEpochMs = 1L))
        store.save(sampleRecipe(id = "new").copy(savedAtEpochMs = 2L))
        assertEquals(listOf("new", "old"), store.recipes.first().map { it.id })
    }
}
