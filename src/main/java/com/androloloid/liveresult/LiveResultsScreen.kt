package com.androloloid.liveresult

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.androloloid.liveresult.data.ClassResults
import com.androloloid.liveresult.data.RunnerResult
import kotlinx.coroutines.delay


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
                        value = viewModel.selectedClass?.className ?: "Select a class",
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
                        label = { Text("Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                //modifier = Modifier.clickable { searchQuery = TextFieldValue("") }
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear",
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
                            ResultItem(viewModel = viewModel, result = result, classResults=viewModel.classResults, modifier = Modifier, language = "en")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No results to display.")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No competition selected")
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
                Icon(Icons.Filled.Search, "Search")
            }
        } else {

        }
    }
}

@Composable
fun ResultItem(viewModel: CompetitionViewModel, result: RunnerResult, classResults: ClassResults?, modifier: Modifier, language: String) {
    var expandedRow by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
            },
        onClick = { expandedRow = !expandedRow },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier) {
                Row(modifier = Modifier) {
                    Text(
                        text = result.getPlace(language),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier) {
                    Text(
                        text = result.getName(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier) {
                    if (classResults == null) {
                        Text(text = viewModel.runnersClass[result.getName()] ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = result.clubName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            // compute the column width so the text "22:22:22" using the default font with bold style can fit the column
            Column(modifier = Modifier) {
                Row(modifier = Modifier) {
                    Text(
                        result.getResult(language),
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier) {
                    Text(
                        text = result.getTimePlus(),
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = false
                    )
                }
            }
        }
        if (expandedRow && result.hasSplits()) {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom=16.dp, top = 0.dp  )
                    .fillMaxWidth()
            ) {
                for(split in result.getSplits(classResults?.splitcontrols)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally)
                    {
                        Text(
                            text = split.code,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = false
                        )
                        Text(
                            text = if (split.status == 0) { split.time + "("+split.place+")" } else { split.time },
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = false
                        )
                        Text(
                            text = "+"+split.timeplus,
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
