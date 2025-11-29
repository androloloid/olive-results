package com.androloloid.liveresult

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.androloloid.liveresult.data.RunnerResult
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveResultsScreen(navController: NavController, viewModel: CompetitionViewModel) {
    val competition = viewModel.selectedCompetition
    val classes = viewModel.competitionClasses.classes
    val results =  viewModel.classResults?.results
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
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
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor(PrimaryNotEditable)
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
            
            RefreshProgressBar(viewModel, Modifier)

            if (results != null) {
                LazyColumn(modifier = Modifier) {
                    items(results) { result ->
                        ResultItem(result = result, modifier = Modifier, language="en")
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
    }
}

@Composable
fun RefreshProgressBar(viewModel: CompetitionViewModel, modifier: Modifier = Modifier) {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    if (viewModel.selectedCompetition?.isToday() == true) {
        LaunchedEffect(Unit) {
                while (true) {
                    viewModel.periodicClassResultRefreshTask()
                    currentProgress = viewModel.getRefreshProgress()
                    delay(100)
                }
        }
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
    }
}

@Composable
fun ResultItem(result: RunnerResult, modifier: Modifier, language : String) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            Column(modifier = Modifier) {
                Row(modifier = Modifier) {
                    Text(
                        text = result.getPlace(language),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier) {
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
    }
}
