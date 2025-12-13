package com.androloloid.liveresult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubResultsScreen(navController: NavController, viewModel: CompetitionViewModel) {
    val competition = viewModel.selectedCompetition
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var lastTextToSearch by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // observer viewModel for changes in selectedCompetition
    LaunchedEffect(viewModel.selectedClubsResults) {

    }

    if (competition != null && viewModel.clubs.isEmpty()) {
        viewModel.loadClubNames(competition)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LaunchedEffect(searchQuery) {
                viewModel.selectClubs(searchQuery.text)
            }

            if (competition != null) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoadingClubs,
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Club") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Club Search",
                            modifier = Modifier.clickable {  keyboardController?.hide() }
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.clickable { searchQuery = TextFieldValue(""); keyboardController?.hide()}
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                )

                RefreshProgressBar(viewModel, key = viewModel.selectedClubs, modifier = Modifier) { viewModel.periodicClubResultRefreshTask() }

                if (!viewModel.isLoadingClubs && viewModel.selectedClubsResults.isNotEmpty()) {
                    println("### results changed selectedClubsResults.size=${viewModel.selectedClubsResults.size}")
                    LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                        items(viewModel.selectedClubsResults) { result ->
                            ResultItem(viewModel = viewModel, result = result, classResults=null, modifier = Modifier, language = "en")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isLoadingClubs) {
                            Text("Loading clubs...")
                        } else if (viewModel.isLoadingClubsResults) {
                            Text("Loading results...")
                        } else if (viewModel.selectedClubs.isNotEmpty()) {
                            Text("No results to display.")
                        } else {
                            Text("Please select a club to see results.\n  ${viewModel.clubs.size} clubs loaded")
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No competition selected")
                }
            }
        }
    }
}
