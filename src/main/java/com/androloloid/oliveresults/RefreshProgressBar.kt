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
