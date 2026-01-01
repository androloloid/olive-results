/*
This file is part of O'Live Results.

O'Live Results is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

O'Live Results is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with O'Live Results. If
not, see <https://www.gnu.org/licenses/>

@Author: androloloid@gmail.com
@Date: 2026-01
 */

package com.androloloid.oliveresults

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
import com.androloloid.oliveresults.ui.theme.OLiveResultsTheme

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
            OLiveResultsTheme(dynamicColor = false /*use app colors*/) {
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
