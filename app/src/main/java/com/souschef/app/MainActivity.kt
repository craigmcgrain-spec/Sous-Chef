package com.souschef.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.souschef.app.ui.LibraryScreen
import com.souschef.app.ui.LibraryViewModel
import com.souschef.app.ui.PasteLinkDialog
import com.souschef.app.ui.ShareDialog
import com.souschef.app.ui.ShareUiState
import com.souschef.app.ui.ShareViewModel
import com.souschef.app.ui.ShoppingScreen
import com.souschef.app.ui.ShoppingViewModel
import com.souschef.app.ui.edit.EditRecipeScreen
import com.souschef.app.ui.recipe.RecipeScreen
import com.souschef.app.ui.recipe.RecipeTab
import com.souschef.app.ui.recipe.RecipeViewModel
import com.souschef.app.ui.theme.SousChefTheme

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    private val shareViewModel: ShareViewModel by viewModels {
        viewModelFactory {
            initializer { ShareViewModel(container.fetcher, container.extractor, container.store) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = (application as SousChefApp).container
        // A rotation re-delivers the launch intent; only a fresh screen consumes it.
        if (savedInstanceState == null) handleShareIntent(intent)
        setContent {
            SousChefTheme {
                SousChefNavHost(container, shareViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            shareViewModel.onShareText(intent.getStringExtra(Intent.EXTRA_TEXT))
        }
    }
}

private const val EDIT_ROUTE = "recipe/{id}/edit"

@Composable
private fun SousChefNavHost(container: AppContainer, shareViewModel: ShareViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var pasteLinkOpen by rememberSaveable { mutableStateOf(false) }
    val shareState by shareViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }
    LaunchedEffect(shareState) {
        val done = shareState as? ShareUiState.Done ?: return@LaunchedEffect
        navController.navigate("recipe/${done.recipeId}") { launchSingleTop = true }
        shareViewModel.consumeDone()
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
                    onPasteLink = { pasteLinkOpen = true },
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
            composable("shopping") {
                val shoppingViewModel: ShoppingViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ShoppingViewModel(container.store) }
                    },
                )
                val items by shoppingViewModel.items.collectAsStateWithLifecycle()
                ShoppingScreen(
                    items = items,
                    onBack = { navController.popBackStack() },
                    onToggle = shoppingViewModel::toggle,
                    onClearChecked = shoppingViewModel::clearChecked,
                    onClearAll = shoppingViewModel::clearAll,
                )
            }
        }
        ShareDialog(
            state = shareState,
            onRetry = shareViewModel::retry,
            onDismiss = shareViewModel::dismissFailure,
            onPasteLink = { pasteLinkOpen = true },
        )
        if (pasteLinkOpen) {
            PasteLinkDialog(
                onDismiss = { pasteLinkOpen = false },
                onOpen = { url ->
                    pasteLinkOpen = false
                    shareViewModel.onShareText(url)
                },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}
