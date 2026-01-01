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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androloloid.oliveresults.data.ClassResults
import com.androloloid.oliveresults.data.Competition
import com.androloloid.oliveresults.data.RunnerResult
import com.androloloid.oliveresults.data.Split
import kotlin.math.ceil

//import com.androloloid.liveresult.R // Make sure to import your app's R class


fun toLowerCamelCase(input: String): String {
    val words = input.lowercase().split(' ').joinToString(" ") {
        if (it.isNotEmpty()) it.replaceFirstChar(Char::titlecaseChar) else it
    }
    return words
}

@Composable
fun getStatusStringResource(status: Int): String {
    val resourceId = when (status) {
        0 -> R.string.status_OK
        1 -> R.string.status_DNS
        2 -> R.string.status_DNF
        3 -> R.string.status_MP
        4 -> R.string.status_DSQ
        5 -> R.string.status_out_of_time
        9, 10 -> R.string.status_not_started_yet
        11 -> R.string.status_walk_over
        12 -> R.string.status_move_up
        else -> R.string.status_Unknown
    }
    return stringResource(id = resourceId)
}
@Composable
fun getLongStatusStringResource(status: Int): String {
    val resourceId = when (status) {
        1 -> R.string.status_long_DNS
        2 -> R.string.status_long_DNF
        3 -> R.string.status_long_MP
        4 -> R.string.status_long_DSQ
        5 -> R.string.status_long_out_of_time
        /*
    9 -> label = stringResource(id = R.string.status_long_not_started_yet)
    10 -> label = stringResource(id = R.string.status_long_not_started_yet)
    */
        11 -> R.string.status_long_walk_over
        else -> null
    }
    if (resourceId == null) {
        return status.toString()
    }
    return stringResource(id = resourceId)
}

@Composable
fun ResultItem(viewModel: CompetitionViewModel, result: RunnerResult, classResults: ClassResults?, modifier: Modifier) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
        ResultItemV(viewModel, result, classResults, modifier)
    } else {
        ResultItemH(viewModel, result, classResults, modifier)
    }
}

    @Composable
fun ResultItemV(viewModel: CompetitionViewModel, result: RunnerResult, classResults: ClassResults?, modifier: Modifier) {
    var expandedRow by remember { mutableStateOf(false) }
    var containerBgColor = MaterialTheme.colorScheme.surfaceContainerLow
    if (result.getRanking() <= 3) {
        containerBgColor = MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { expandedRow = !expandedRow },
        colors = CardDefaults.cardColors(
            containerColor = containerBgColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp, 4.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Spacer(modifier = Modifier.weight(1f))
                CircleChar(result.getRankingStr(), result.getStatus())
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(5.dp))
            NameCLubColumn(Modifier.weight(1f), result, classResults, viewModel)

            Spacer(modifier = Modifier.width(5.dp))
            TimeColumn(result, viewModel.selectedCompetition, displayStartIfNotRunning = true)
        }

        if (expandedRow && result.hasSplits()) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp, bottom=8.dp, top = 0.dp  )
                    .fillMaxWidth()
            ) {
                SplitColumns(Modifier.weight(1f, fill = false), result.getSplits(classResults?.splitcontrols), 6)
                //Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ResultItemH(viewModel: CompetitionViewModel, result: RunnerResult, classResults: ClassResults?, modifier: Modifier) {
    var containerBgColor = MaterialTheme.colorScheme.surfaceContainerLow
    if (result.getRanking() <= 3) {
        containerBgColor = MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerBgColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp, 4.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Spacer(modifier = Modifier.weight(1f))
                CircleChar(result.getRankingStr(), result.getStatus())
                Spacer(modifier = Modifier.weight(1f))
            }

            // compute the size that a text with 10 characters would take
            val textMeasurer = rememberTextMeasurer()
            val colNameWidth = textMeasurer.measure("________").size.width
            val modifierColName = if(classResults?.splitcontrols?.size?:0 > 0) modifier.width(colNameWidth.dp) else modifier.weight(2f).fillMaxWidth()

            Spacer(modifier = Modifier.width(5.dp))
            NameCLubColumn(modifierColName, result, classResults, viewModel)

            if (result.isRunningToday(andHasStartTime = true)) {
                Spacer(modifier = Modifier.width(5.dp))
                StartTimeColumn(result)
            }

            if (result.hasSplits()) {
                Spacer(modifier = Modifier.width(15.dp))
                SplitColumns(Modifier.weight(1f, fill = false), result.getSplits(classResults?.splitcontrols), 8)
            }

            Spacer(modifier = Modifier.width(5.dp))
            TimeColumn(result,viewModel.selectedCompetition, displayStartIfNotRunning = false)
        }
    }
}

@Composable
fun CircleChar(text: String, runner_status_id: Int, modifier: Modifier = Modifier) {
    var label = text
    var txtColor = MaterialTheme.colorScheme.onPrimary
    var bgColor = MaterialTheme.colorScheme.primary
    // if text is not an int set the color to secondary
    if (runner_status_id != 0) {
        label = getStatusStringResource(runner_status_id)
        txtColor = MaterialTheme.colorScheme.onSurface
        bgColor = MaterialTheme.colorScheme.surface
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .background(bgColor, CircleShape)
    ) {
        Text(
            text = label,
            color = txtColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NameCLubColumn(modifier: Modifier, result: RunnerResult, classResults: ClassResults?, viewModel: CompetitionViewModel) {
    Column(modifier = modifier) {
        Row(modifier = Modifier) {
            val name = toLowerCamelCase(result.getName())
            Text(
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier) {
            if (classResults == null) {
                Text(
                    text = viewModel.runnersClass[result.getName()] ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = result.clubName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StartTimeColumn(result: RunnerResult) {
    Column(
        modifier = Modifier.padding(horizontal = 5.dp).wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Text(
            text = stringResource(id = R.string.start),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = result.getStartTime(),
            style = MaterialTheme.typography.bodySmall,
            softWrap = false
        )
    }
}

@Composable
fun TimeColumn(result: RunnerResult, competition : Competition?, displayStartIfNotRunning : Boolean) {
    Column(
        modifier = Modifier.padding(horizontal = 5.dp).wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        if (result.getStatus() == 0) {
            Text(
                result.getResult(),
                fontWeight = FontWeight.Bold,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.getTimePlus(),
                style = MaterialTheme.typography.bodySmall,
                softWrap = false
            )
        } else if (result.isRunningToday(true)) {
            // refresh getTimeFromStart text every seconds
            var runTime by remember { mutableStateOf("") }
            //result.getTimeFromStart(competition)
            LaunchedEffect(result) {
                while (true) {
                    runTime = result.getTimeFromStart(competition)
                    kotlinx.coroutines.delay(1000)
                }
            }
            if (runTime != "") {
                Text(
                    text = runTime,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF808080) ,
                    //style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    softWrap = false
                )
            } else if (displayStartIfNotRunning) {
                Text(
                    text = stringResource(id = R.string.start),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.getStartTime(),
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = false
                )
            }
        } else {
            // MP / disqualified / ...
            Text(
                getLongStatusStringResource(result.getStatus()),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                softWrap = false
            )
        }
    }
}

@Composable
fun SplitColumns(modifier : Modifier, splits: List<Split>, numSplitsMax: Int = 6) {
    Column(modifier = modifier)
    {
        for(lineId in 0 until ceil(splits.size/numSplitsMax.toDouble()).toInt()) {
            Row() {
                for (splitIndex in lineId*numSplitsMax until splits.size) {
                    val split = splits[splitIndex]
                    Column(
                        modifier = Modifier.padding(horizontal = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    )
                    {
                        Text(
                            text = split.code,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = false
                        )
                        Text(
                            text = if (split.status == 0) {
                                split.time + "(" + split.place + ")"
                            } else {
                                split.time
                            },
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = false
                        )
                        Text(
                            text = "+" + split.timeplus,
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
