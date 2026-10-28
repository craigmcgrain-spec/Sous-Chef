package com.souschef.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.souschef.app.ui.edit.EditRecipeScreen
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

private const val EDIT_ROUTE = "recipe/{id}/edit"

@Composable
private fun SousChefNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
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
                val shopping by recipeViewModel.shopping.collectAsStateWithLifecycle()
                val addedShoppingTexts = remember(shopping, recipeId) {
                    shopping.filter { it.recipeId == recipeId }
                        .mapTo(HashSet()) { it.text.trim() }
                }
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
                        onJumpToStep = { activeTab = RecipeTab.Steps.name },
                        addedShoppingTexts = addedShoppingTexts,
                        onAddToShopping = recipeViewModel::addToShopping,
                        onOpenShopping = { navController.navigate("shopping") },
                        shoppingCount = shopping.size,
                    )
                }
            }
            composable(EDIT_ROUTE) { entry ->
                val recipeId = entry.arguments?.getString("id").orEmpty()
                val recipeViewModel: RecipeViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { RecipeViewModel(recipeId, container.store, container.extractor) }
                    },
                )
                val recipe by recipeViewModel.recipe.collectAsStateWithLifecycle()
                var saving by remember { mutableStateOf(false) }
                recipe?.let { current ->
                    EditRecipeScreen(
                        recipe = current,
                        saving = saving,
                        onCancel = { navController.popBackStack() },
                        onSave = { edited ->
                            saving = true
                            recipeViewModel.saveEdited(edited) { stale ->
                                saving = false
                                // The text saved either way; only temperatures were left stale.
                                if (stale != null) {
                                    snackbarMessage = context.getString(R.string.temperatures_stale)
                                }
                                // Only close the route we saved from; a save that
                                // outlives the screen must not pop the recipe.
                                if (navController.currentDestination?.route == EDIT_ROUTE) {
                                    navController.popBackStack()
                                }
                            }
                        },
                    )
                }
            }
            composable("shopping") { Placeholder(stringResource(R.string.shopping_list)) }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
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
