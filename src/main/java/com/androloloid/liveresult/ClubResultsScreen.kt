package com.androloloid.liveresult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ClubResultsScreen(navController: NavController, viewModel: CompetitionViewModel) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var sortMode by remember { mutableStateOf("abc") }

    val competition = viewModel.selectedCompetition
    if (competition != null && viewModel.clubs.isEmpty()) {
        viewModel.loadClubNames(competition)
    }

    LaunchedEffect(searchQuery) {
        viewModel.selectClubs(searchQuery.text)
    }

    Scaffold(
        floatingActionButton = {
            if (viewModel.shouldShowClubSortButton()) {
                SortButton(sortMode) { newSortMode -> sortMode = newSortMode }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                viewModel = viewModel
            )
            Spacer(Modifier.height(16.dp))
            MainContent(viewModel, sortMode)
        }
    }

    LoadingOverlay(viewModel)
}

@Composable
private fun MainContent(viewModel: CompetitionViewModel, sortMode: String) {
    if (viewModel.selectedCompetition == null) {
        EmptyState(stringResource(R.string.no_competition_selected))
        return
    }

    when {
        viewModel.isLoadingClubs && viewModel.clubs.isEmpty() -> {
            // Loading state is handled by the LoadingOverlay
        }
        viewModel.isLoadingClubsResults && viewModel.selectedClubsResults.isEmpty()-> {
            // Loading state is handled by the LoadingOverlay
        }
        !viewModel.isLoadingClubs && viewModel.selectedClubsResults.isNotEmpty() -> {
            ClubResultsList(viewModel, sortMode)
        }
        viewModel.selectedClubs.isNotEmpty() && !viewModel.isLoadingClubsResults -> {
            EmptyState(stringResource(R.string.no_results))
        }
        viewModel.selectedClubs.isEmpty() && !viewModel.isLoadingClubs -> {
            EmptyState(stringResource(R.string.enter_club) + "${viewModel.clubs.size} " + stringResource(R.string.clubs_loaded))
        }
    }
}


@Composable
private fun LoadingOverlay(viewModel: CompetitionViewModel) {
    val showLoadingClubs = viewModel.isLoadingClubs && viewModel.clubs.isEmpty()
    val showLoadingResults = viewModel.isLoadingClubsResults

    if (showLoadingClubs || showLoadingResults) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                if (showLoadingClubs) {
                    Text(stringResource(R.string.loading_clubs))
                } else if (showLoadingResults) {
                    if (viewModel.selectedClubsResults.isEmpty()) {
                        Text(stringResource(R.string.loading_results))
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    viewModel: CompetitionViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            enabled = !viewModel.isLoadingClubs,
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(id = R.string.search_club)) },
            label = { Text(stringResource(id = R.string.search)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Club Search",
                    modifier = Modifier.clickable { keyboardController?.hide() }
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier.clickable {
                        onSearchQueryChange(TextFieldValue(""))
                        keyboardController?.hide()
                    }
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )
        if (viewModel.selectedCompetition?.isToday() == true) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { viewModel.periodicClubResultRefreshTask() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

@Composable
private fun ClubResultsList(viewModel: CompetitionViewModel, sortMode: String) {
    val sortedResults = remember(viewModel.selectedClubsResults, sortMode) {
        when (sortMode) {
            "abc" -> viewModel.selectedClubsResults.sortedBy { it.getName() }
            "123" -> viewModel.selectedClubsResults.sortedBy { it.getRanking() }
            else -> viewModel.selectedClubsResults.sortedBy { it.clubName }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sortedResults) { result ->
            ResultItem(
                viewModel = viewModel,
                result = result,
                classResults = null,
                modifier = Modifier
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
        }
    }
}

@Composable
private fun SortButton(sortMode: String, onSortModeChange: (String) -> Unit) {
    FloatingActionButton(
        onClick = {
            val newSortMode = when (sortMode) {
                "abc" -> "123"
                "123" -> "Team"
                else -> "abc"
            }
            onSortModeChange(newSortMode)
        },
    ) {
        when (sortMode) {
            "abc" -> Icon(painterResource(R.drawable.sort_alpha), contentDescription = "Sort Alpha")
            "123" -> Icon(painterResource(R.drawable.sort_numeric), contentDescription = "Sort Numeric")
            else -> Icon(painterResource(R.drawable.sort_team), contentDescription = "Sort Team")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, textAlign = TextAlign.Center)
    }
}

private fun CompetitionViewModel.shouldShowClubSortButton(): Boolean {
    return !isLoadingClubs && !isLoadingClubsResults && selectedClubsResults.isNotEmpty()
}
