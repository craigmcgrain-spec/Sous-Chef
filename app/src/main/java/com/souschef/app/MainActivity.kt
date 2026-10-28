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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        composable("recipe/{id}") { Placeholder("Recipe") }
        composable("shopping") { Placeholder("Shopping") }
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
