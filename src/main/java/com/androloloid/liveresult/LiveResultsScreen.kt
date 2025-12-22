package com.androloloid.liveresult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.androloloid.liveresult.data.RunnerResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveResultsScreen(navController: NavController, viewModel: CompetitionViewModel) {
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            if (!searching && viewModel.selectedCompetition != null) {
                FloatingActionButton(
                    onClick = { searching = true },
                ) {
                    Icon(Icons.Filled.Search, stringResource(R.string.search))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        ) {
            if (viewModel.selectedCompetition != null) {
                LiveResultsContent(
                    viewModel = viewModel,
                    searching = searching,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSearchingChange = {
                        searching = it
                        if (!it) searchQuery = ""
                    }
                )
            } else {
                EmptyState(stringResource(R.string.no_competition_selected))
            }
        }
    }
}

@Composable
private fun ColumnScope.LiveResultsContent(
    viewModel: CompetitionViewModel,
    searching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchingChange: (Boolean) -> Unit
) {
    ClassSelectorDropdown(viewModel)

    if (viewModel.classResults?.needRefresh() == true) {
        RefreshProgressBar(
            viewModel,
            key = viewModel.selectedClass,
            modifier = Modifier
        ) { viewModel.periodicClassResultRefreshTask() }
    }

    if (searching) {
        ResultsSearchField(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onDone = { onSearchingChange(false) }
        )
    }

    val results = remember(viewModel.classResults, searchQuery) {
        viewModel.classResults?.results
            ?.filter {
                it.getName().contains(searchQuery, ignoreCase = true) ||
                        it.clubName.contains(searchQuery, ignoreCase = true)
            }
            ?.sortedBy { it.getRanking() }
    }

    if (results != null) {
        ResultsList(
            results = results,
            viewModel = viewModel,
            modifier = Modifier.weight(1f)
        )
    } else {
        EmptyState(message = stringResource(R.string.no_results), modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassSelectorDropdown(viewModel: CompetitionViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val competition = viewModel.selectedCompetition ?: return

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(top = 0.dp),
    ) {
        TextField(
            readOnly = true,
            value = viewModel.selectedClass?.className ?: stringResource(R.string.select_class),
            onValueChange = {},
            label = { Text(competition.name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            viewModel.competitionClasses.classes.forEach { competitionClass ->
                DropdownMenuItem(
                    text = { Text(competitionClass.className) },
                    onClick = {
                        viewModel.selectClass(competitionClass)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultsSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text(stringResource(R.string.search)) },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search)) },
        trailingIcon = {
            Icon(
                Icons.Filled.Clear,
                contentDescription = stringResource(R.string.clear1),
                modifier = Modifier.clickable {
                    onSearchQueryChange("")
                    onDone()
                }
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            onDone()
            keyboardController?.hide()
        })
    )
}

@Composable
private fun ResultsList(
    results: List<RunnerResult>,
    viewModel: CompetitionViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(results) { result ->
            ResultItem(
                viewModel = viewModel,
                result = result,
                classResults = viewModel.classResults,
                modifier = Modifier
            )
        }
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message)
    }
}
