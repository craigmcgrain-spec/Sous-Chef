package com.souschef.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.UUID

interface RecipeStore {
    val recipes: Flow<List<Recipe>>
    val shopping: Flow<List<ShoppingItem>>
    suspend fun save(recipe: Recipe)
    suspend fun update(recipe: Recipe)

    /** Atomic read-modify-write: [transform] runs on the stored copy under the write lock. */
    suspend fun update(id: String, transform: (Recipe) -> Recipe)
    suspend fun delete(id: String)
    suspend fun findByUrl(url: String): Recipe?
    suspend fun addToShopping(recipe: Recipe, texts: List<String>)
    suspend fun toggleShoppingItem(id: String)
    suspend fun clearCheckedShopping()
    suspend fun clearAllShopping()
}

private val RECIPES = stringPreferencesKey("recipes")
private val SHOPPING = stringPreferencesKey("shopping")
private val RECIPES_BACKUP = stringPreferencesKey("recipes_backup")

class DataStoreRecipeStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = SousChefJson,
) : RecipeStore {

    private val mutex = Mutex()

    // shortcut: whole-list JSON rewrite; move to Room past ~500 recipes.
    override val recipes: Flow<List<Recipe>> = dataStore.data.map { prefs ->
        readRecipes(prefs).sortedByDescending { it.savedAtEpochMs }
    }

    override val shopping: Flow<List<ShoppingItem>> = dataStore.data.map { prefs ->
        readShopping(prefs)
    }

    override suspend fun save(recipe: Recipe) = mutateRecipes { current ->
        current.filterNot { it.id == recipe.id } + recipe
    }

    override suspend fun update(recipe: Recipe) = save(recipe)

    override suspend fun update(id: String, transform: (Recipe) -> Recipe) = mutateRecipes { current ->
        current.map { if (it.id == id) transform(it) else it }
    }

    override suspend fun delete(id: String) = mutateRecipes { current ->
        current.filterNot { it.id == id }
    }

    override suspend fun findByUrl(url: String): Recipe? {
        val key = normalizeUrl(url)
        return recipes.first().firstOrNull { normalizeUrl(it.url) == key }
    }

    override suspend fun addToShopping(recipe: Recipe, texts: List<String>) {
        val now = System.currentTimeMillis()
        mutateShopping { current ->
            val seen = current.mapTo(HashSet()) { it.recipeId to it.text }
            val added = texts.map { it.trim() }
                .filter { it.isNotEmpty() && seen.add(recipe.id to it) }
                .map {
                    ShoppingItem(
                        id = UUID.randomUUID().toString(),
                        text = it,
                        checked = false,
                        recipeId = recipe.id,
                        recipeTitle = recipe.title,
                        addedAtEpochMs = now,
                    )
                }
            current + added
        }
    }

    override suspend fun toggleShoppingItem(id: String) = mutateShopping { current ->
        current.map { if (it.id == id) it.copy(checked = !it.checked) else it }
    }

    override suspend fun clearCheckedShopping() = mutateShopping { current ->
        current.filterNot { it.checked }
    }

    override suspend fun clearAllShopping() = mutateShopping { emptyList() }

    private suspend fun mutateRecipes(transform: (List<Recipe>) -> List<Recipe>) {
        mutex.withLock {
            val current = readRecipes(dataStore.data.first())
            dataStore.edit { it[RECIPES] = json.encodeToString(transform(current)) }
        }
    }

    private suspend fun mutateShopping(transform: (List<ShoppingItem>) -> List<ShoppingItem>) {
        mutex.withLock {
            val current = readShopping(dataStore.data.first())
            dataStore.edit { it[SHOPPING] = json.encodeToString(transform(current)) }
        }
    }

    private suspend fun readRecipes(prefs: Preferences): List<Recipe> {
        val raw = prefs[RECIPES] ?: return emptyList()
        return try {
            json.decodeFromString<List<Recipe>>(raw)
        } catch (e: SerializationException) {
            // Keep the unreadable payload for inspection instead of losing it to the next write.
            dataStore.edit { it[RECIPES_BACKUP] = raw }
            emptyList()
        }
    }

    private fun readShopping(prefs: Preferences): List<ShoppingItem> {
        val raw = prefs[SHOPPING] ?: return emptyList()
        return try {
            json.decodeFromString<List<ShoppingItem>>(raw)
        } catch (e: SerializationException) {
            emptyList()
        }
    }
}
