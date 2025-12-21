package com.androloloid.liveresult

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.androloloid.liveresult.ui.theme.LiveResultTheme


//https://fonts.google.com/icons?selected=Material+Symbols+Outlined:leaderboard:FILL@0;wght@400;GRAD@0;opsz@48&icon.query=leaderboard&icon.size=48&icon.color=%231f1f1f

// Sealed class to represent the navigation destinations
sealed class Screen(val route: String, val label: Int, val icon: ImageVector) {
    object Competitions : Screen("competitions", R.string.events, Icons.Default.Menu)
    object LiveResults : Screen("liveresults", R.string.class_results, Icons.Default.Person)
    object ClubResults : Screen("clubresults", R.string.club_results, Icons.Default.Favorite)
}

val items = listOf(
    Screen.Competitions,
    Screen.LiveResults,
    Screen.ClubResults
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveResultTheme(dynamicColor = true) {
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
                        icon = {  Icon(screen.icon, contentDescription = stringResource(screen.label)) },
                        label = { Text(stringResource(screen.label)) },
                        selected = selectedItem == index,
                        onClick = { 
                            selectedItem = index
                            navController.navigate(screen.route)
                         },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Competitions.route, Modifier.padding(innerPadding)) {
            composable(Screen.Competitions.route) { CompetitionScreen(navController, competitionViewModel) }
            composable(Screen.LiveResults.route) { LiveResultsScreen(navController, competitionViewModel) }
            composable(Screen.ClubResults.route) { ClubResultsScreen(navController, competitionViewModel) }
        }
    }
}
