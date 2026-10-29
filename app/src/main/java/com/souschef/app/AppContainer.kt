package com.souschef.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.souschef.app.data.DataStoreRecipeStore
import com.souschef.app.data.HtmlFetcher
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeFetcher
import com.souschef.app.data.DataStoreSettingsStore
import com.souschef.app.data.RecipeParser
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.SettingsStore

private val Context.dataStore by preferencesDataStore(name = "souschef")

/** One instance per process; [RecipeParser]'s mutex only serializes within one instance. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val store: RecipeStore = DataStoreRecipeStore(appContext.dataStore)
    val settings: SettingsStore = DataStoreSettingsStore(appContext.dataStore)
    val fetcher: HtmlFetcher = RecipeFetcher()
    val extractor: RecipeExtractor = RecipeParser()
}
