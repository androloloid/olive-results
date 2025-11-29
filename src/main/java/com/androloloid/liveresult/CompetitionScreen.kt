package com.androloloid.liveresult

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.androloloid.liveresult.data.Competition
import com.androloloid.liveresult.data.Competitions

//https://medium.com/@anu91ch/scan-qr-code-bar-code-android-kotlin-tutorial-using-ml-kit-f76b48e3289b

@Composable
fun CompetitionScreen(navController: NavController, viewModel: CompetitionViewModel) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScanButton(
                onClick = { /*TODO*/ },
                isLoading = viewModel.isLoading,
                modifier = Modifier
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { })
            )
            RefreshButton(
                onClick = { viewModel.loadCompetitions() },
                isLoading = viewModel.isLoading,
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CompetitionList(
            competitions = viewModel.competitions,
            navController = navController,
            viewModel = viewModel,
            searchQuery = searchQuery.text,
            listState = listState
        )
    }
}

@Composable
fun RefreshButton(onClick: () -> Unit, isLoading: Boolean, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, enabled = !isLoading, modifier = modifier) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
    }
}

@Composable
fun ScanButton(onClick: () -> Unit, isLoading: Boolean, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, enabled = !isLoading, modifier = modifier) {
        Icon(painterResource(id = R.drawable.qr_code), contentDescription = "Scan QR Code")
    }
}

@Composable
fun CompetitionList(
    competitions: Competitions,
    navController: NavController,
    viewModel: CompetitionViewModel,
    searchQuery: String,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val filteredCompetitions = if (searchQuery.isEmpty()) {
        competitions.competitions
    } else {
        competitions.competitions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (filteredCompetitions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Press Refresh to load competitions.")
        }
    } else {

        // move to the selected competition
        val firstMatchIndex = if (viewModel.selectedCompetition != null) {
            filteredCompetitions.indexOfFirst { it == viewModel.selectedCompetition }
        } else {
            -1
        }

        LaunchedEffect(firstMatchIndex) {
            if (firstMatchIndex != -1) {
                listState.animateScrollToItem(firstMatchIndex)
            }
        }

        // create the list of competitions to displqy
        LazyColumn(modifier = modifier, state = listState) {
            itemsIndexed(filteredCompetitions) { index, competition ->
                val isMatch = searchQuery.isNotEmpty() &&
                        (competition.name.contains(searchQuery, ignoreCase = true)
                        || competition.organizer.contains(searchQuery, ignoreCase = true)
                        || competition.id.toString().contains(searchQuery, ignoreCase = true))

                CompetitionItem(
                    competition = competition,
                    navController = navController,
                    viewModel = viewModel,
                    isMatch = isMatch
                )
            }
        }
    }
}

@Composable
fun CompetitionItem(
    competition: Competition,
    navController: NavController,
    viewModel: CompetitionViewModel,
    modifier: Modifier = Modifier,
    isMatch: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                viewModel.selectCompetition(competition)
                navController.navigate(Screen.LiveResults.route)
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                competition == viewModel.selectedCompetition -> MaterialTheme.colorScheme.primary
                isMatch -> MaterialTheme.colorScheme.surfaceContainerHighest
                competition.isToday() -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = competition.name,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${competition.date} - ${competition.organizer}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
