package com.androloloid.liveresult

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.androloloid.liveresult.ui.theme.LiveResultTheme

// Sealed class to represent the navigation destinations
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Competitions : Screen("competitions", "Competitions", Icons.Default.List)
    object LiveResults : Screen("liveresults", "Live Results", Icons.Default.Person)
    object Favorite : Screen("favorite", "Favorite", Icons.Default.Favorite)
}

val items = listOf(
    Screen.Competitions,
    Screen.LiveResults,
    Screen.Favorite
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveResultTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val competitionViewModel: CompetitionViewModel = viewModel()

    var selectedItem by remember { mutableIntStateOf(0) }

    competitionViewModel.init()

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val route = backStackEntry.destination.route
            selectedItem = items.indexOfFirst { it.route == route }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selectedItem == index,
                        onClick = { 
                            selectedItem = index
                            navController.navigate(screen.route)
                         }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Competitions.route, Modifier.padding(innerPadding)) {
            composable(Screen.Competitions.route) { CompetitionScreen(navController, competitionViewModel) }
            composable(Screen.LiveResults.route) { LiveResultsScreen(navController, competitionViewModel) }
            composable(Screen.Favorite.route) { FavoriteScreen(navController, competitionViewModel) }
        }
    }
}
