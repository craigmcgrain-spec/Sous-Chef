package com.souschef.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.souschef.app.ui.LibraryScreen
import com.souschef.app.ui.LibraryViewModel
import com.souschef.app.ui.recipe.RecipeScreen
import com.souschef.app.ui.recipe.RecipeTab
import com.souschef.app.ui.recipe.RecipeViewModel
import com.souschef.app.ui.theme.SousChefTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SousChefApp).container
        setContent {
            SousChefTheme {
                SousChefNavHost(container)
            }
        }
    }
}

@Composable
private fun SousChefNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LibraryViewModel(container.store) }
                },
            )
            val recipes by libraryViewModel.recipes.collectAsStateWithLifecycle()
            val uncheckedCount by libraryViewModel.uncheckedShoppingCount.collectAsStateWithLifecycle()
            LibraryScreen(
                recipes = recipes,
                uncheckedShoppingCount = uncheckedCount,
                onOpenRecipe = { id -> navController.navigate("recipe/$id") },
                onOpenShopping = { navController.navigate("shopping") },
                // Task 13 wires the paste dialog; until then the action is inert.
                onPasteLink = {},
            )
        }
        composable("recipe/{id}") { entry ->
            val recipeId = entry.arguments?.getString("id").orEmpty()
            val recipeViewModel: RecipeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { RecipeViewModel(recipeId, container.store, container.extractor) }
                },
            )
            val recipe by recipeViewModel.recipe.collectAsStateWithLifecycle()
            var activeTab by rememberSaveable { mutableStateOf(RecipeTab.Ingredients.name) }
            recipe?.let { current ->
                RecipeScreen(
                    recipe = current,
                    activeTab = RecipeTab.valueOf(activeTab),
                    onTabSelected = { activeTab = it.name },
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("recipe/$recipeId/edit") },
                    onDelete = { recipeViewModel.delete { navController.popBackStack() } },
                    onToggleIngredient = recipeViewModel::toggleIngredient,
                    onToggleStep = recipeViewModel::toggleStep,
                    // Task 10 switches to Steps and scrolls the chosen card.
                    onJumpToStep = {},
                    addedShoppingTexts = emptySet(),
                    onAddToShopping = recipeViewModel::addToShopping,
                    onOpenShopping = { navController.navigate("shopping") },
                )
            }
        }
        // Task 11 replaces this edit placeholder with the real form.
        composable("recipe/{id}/edit") { Placeholder(stringResource(R.string.edit_recipe)) }
        composable("shopping") { Placeholder(stringResource(R.string.shopping_list)) }
    }
}

@Composable
private fun Placeholder(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}
