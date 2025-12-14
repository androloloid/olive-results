package com.androloloid.liveresult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryNotEditable
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveResultsScreen(navController: NavController, viewModel: CompetitionViewModel) {
    val competition = viewModel.selectedCompetition
    val classes = viewModel.competitionClasses.classes
    var expanded by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val results = viewModel.classResults?.results?.filter { it.getName().contains(searchQuery, ignoreCase = true) || it.clubName.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (competition != null) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TextField(
                        readOnly = true,
                        value = viewModel.selectedClass?.className ?: stringResource(R.string.select_class),
                        onValueChange = { },
                        label = {
                            Text(
                                competition.name,
                                //style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        classes.forEach { competitionClass ->
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

                RefreshProgressBar(viewModel, key = viewModel.selectedClass, modifier = Modifier) { viewModel.periodicClassResultRefreshTask() }

                if (searching) {
                    val focusRequester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.search)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search),
                                //modifier = Modifier.clickable { searchQuery = TextFieldValue("") }
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear1),
                                modifier = Modifier.clickable { searching = false; searchQuery = "" }
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            searching = false;
                            keyboardController?.hide()
                        })
                    )
                }

                if (results != null) {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(results) { result ->
                            ResultItem(viewModel = viewModel, result = result, classResults=viewModel.classResults, modifier = Modifier)
                        }
                        // add an empty item to push the floating action button to the bottom of the screen
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_results))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_competition_selected))
                }
            }
        } // end of column
        if (!searching) {
            FloatingActionButton(
                onClick = { searching = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Search, stringResource(R.string.search))
            }
        } else {

        }
    }
}
