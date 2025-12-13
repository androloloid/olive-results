package com.androloloid.liveresult

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun RefreshProgressBar(
    viewModel: CompetitionViewModel,
    key: Any?,
    modifier: Modifier = Modifier,
    refreshTask: suspend () -> Unit
) {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    if (viewModel.selectedCompetition?.isToday() == true) {
        LaunchedEffect(key) {
            while (true) {
                refreshTask()
                currentProgress = viewModel.getRefreshProgress()
                delay(100)
            }
        }
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )
    }
}
