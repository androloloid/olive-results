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

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androloloid.oliveresults.data.ClassResults
import com.androloloid.oliveresults.data.Competition
import com.androloloid.oliveresults.data.CompetitionClass
import com.androloloid.oliveresults.data.CompetitionClasses
import com.androloloid.oliveresults.data.Competitions
import com.androloloid.oliveresults.data.LiveResultReq
import com.androloloid.oliveresults.data.Passing
import com.androloloid.oliveresults.data.RunnerResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class CompetitionViewModel(application: Application) : AndroidViewModel(application) {
    private val REFRESH_TIME = 200 // *0.1s
    private var refreshTime by mutableStateOf(-1)
    // getRefreshProgress return a pair of (currentProgress, lastRefreshTime)
    var lastRefreshTime by mutableStateOf("")

    private val sharedPreferences = application.getSharedPreferences("LiveResultPrefs", Context.MODE_PRIVATE)

    var hasShownToast by mutableStateOf(false)

    var competitions by mutableStateOf(Competitions(emptyList()))
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingClubs by mutableStateOf(false)
        private set
    var isLoadingClubsResults by mutableStateOf(false)
        private set

    var selectedCompetition by mutableStateOf<Competition?>(null)
        private set

    var selectedCompetitionId by mutableStateOf(0)
        private set

    var competitionClasses by mutableStateOf(CompetitionClasses("OK", emptyList(), ""))
        private set

    var selectedClass by mutableStateOf<CompetitionClass?>(null)
        private set
    var selectedClassNamePreference by mutableStateOf("")
        private set


    var classResults by mutableStateOf<ClassResults?>(null)
        private set

    var selectedClubs by mutableStateOf<MutableList<String>>(mutableListOf())
        private set
    var selectedClubName by mutableStateOf<String>("")
    var selectedClubsResults by mutableStateOf<List<RunnerResult>>(emptyList())
        private set
    var clubs by mutableStateOf<MutableList<String>>(mutableListOf())
        private set

    var runnersClass by mutableStateOf<MutableMap<String, String>>(mutableMapOf())
        private set

    var lastPassingList by mutableStateOf<List<Passing>>(emptyList())
        private set


    fun init() {
        if (competitions.competitions.isEmpty()) {
            loadCompetitions()
        }
    }

    init {
        // load previous settings
        selectedCompetitionId = sharedPreferences.getInt("selectedCompetitionId", 0)
        selectedClassNamePreference = sharedPreferences.getString("selectedClassName", "") ?: ""
        selectedClubName = sharedPreferences.getString("selectedClubName", "") ?: ""
    }

    fun loadCompetitions() {
        viewModelScope.launch {
            isLoading = true
            competitions = LiveResultReq().getCompetitions()
            competitions.sortForDisplay()
            if (selectedCompetitionId != 0) {
                competitions.competitions.find { it.id == selectedCompetitionId }?.let {
                    if (selectedCompetition == null) {
                        selectCompetition(it)
                    }
                }
            }
            isLoading = false
        }
    }

    fun selectCompetition(competition: Competition) {
        if (selectedCompetition == competition) {
            return
        }

        println("selectCompetition: $competition")
        isLoading = true

        // reset
        selectedCompetition = competition
        selectedCompetitionId = competition.id
        selectedClass = null
        clubs.clear()
        runnersClass.clear()
        selectedClubsResults = emptyList()
        selectedClubs.clear()
        competitionClasses = CompetitionClasses("OK", emptyList(), "")
        classResults = null
        lastPassingList = emptyList()

        // load classes
        loadClasses()

        isLoading = false

        // save selectedCompetitionId in the shared preferences
        sharedPreferences.edit().putInt("selectedCompetitionId", selectedCompetitionId).apply()
    }

    fun loadClubNames(competition: Competition?) {
        if (clubs.isEmpty() && competition != null) {
            viewModelScope.launch {
                isLoadingClubs = true
                for (c in competitionClasses.classes) {
                    // print class name in console
                    val results = LiveResultReq().getClassResults(competition.id, c.className, "")
                    for (r in results.results) {
                        runnersClass.put(r.getName(), c.className)
                        if (!clubs.contains(r.clubName)) {
                            clubs.add(r.clubName)
                        }
                    }
                }
                // sort club name alphabetically
                clubs.sort()

                if (selectedClubName != "") {
                    selectClubs(selectedClubName)
                }

                isLoadingClubs = false
            }
        }
    }

    private fun loadClasses() {
        viewModelScope.launch {
            selectedCompetition?.let {
                competitionClasses = LiveResultReq().getClasses(it.id, "")
                if (competitionClasses.classes.isNotEmpty()) {
                    val it = competitionClasses.classes.find { it.className == selectedClassNamePreference }
                    if (it != null) {
                        selectClass(it)
                    } else {
                        selectClass(competitionClasses.classes[0])
                        selectedClassNamePreference = "" // do not use same class when changing the event
                    }
                }
            }
        }
    }

    fun selectClass(competitionClass: CompetitionClass) {
        isLoading = true
        selectedClass = competitionClass
        selectedClassNamePreference = competitionClass.className
        sharedPreferences.edit().putString("selectedClassName", selectedClassNamePreference).apply()

        refreshTime = REFRESH_TIME
        lastRefreshTime = ""
        loadClassResults()
        isLoading = false
    }

    fun loadClassResults() {
        viewModelScope.launch {
            selectedCompetition?.let { competition ->
                selectedClass?.let { competitionClass ->
                    val newClassResults = LiveResultReq().getClassResults(competition.id,
                        competitionClass.className,
                        classResults?.hash?:"")
                    if (newClassResults.status == "OK") {
                        classResults = newClassResults
                    }
                    lastRefreshTime = java.time.LocalTime.now().toString()
                }
            }
        }
    }
    fun periodicClassResultRefreshTask(tickMS: Int = 100) {
        if (selectedCompetition == null
            || selectedCompetition?.isToday() == false
            || selectedClass == null)
        {
            refreshTime = -1
            return
        }

        if (refreshTime < 0) {
            // load class results every REFRESH_TIME
            loadClassResults()
            refreshTime = REFRESH_TIME
        } else {
            refreshTime -= max(tickMS/100, 1)
        }
    }

    private var newClubFilter = ""
    private var activeClubFilter = ""
    private var clubResultThread: Job? = null
    fun selectClubs(filter: String) {
        newClubFilter = filter
        // start thread calling clubResultThreadProcedure if not already running
        if (clubResultThread?.isActive != true) {
            clubResultThread = viewModelScope.launch {
                clubResultThreadProcedure()
            }
        }
    }

    private suspend fun clubResultThreadProcedure() {
        while (true) {
            val textToSearch = newClubFilter
            if (activeClubFilter != textToSearch && !isLoadingClubsResults && !isLoadingClubs) {
                activeClubFilter = textToSearch
                selectClubsAndLoad(activeClubFilter)
            }
            delay(100)
        }
    }

    private fun selectClubsAndLoad(filter: String) {
        if (filter.isNotEmpty()) {
            selectedClubs.clear()
            selectedClubName=filter
            lastClubResultHash = ""
            for (clubName in clubs) {
                if (clubName.contains(filter, ignoreCase = true)) {
                    selectedClubs.add(clubName)
                }
            }
            sharedPreferences.edit().putString("selectedClubName", selectedClubName).apply()
            loadClubResults()
        } else {
            selectedClubs.clear()
            selectedClubName=""
            lastClubResultHash = ""
            selectedClubsResults = emptyList()
        }
    }

    private var lastClubResultHash = ""
    private fun loadClubResults() {
        if (selectedClubs.isEmpty()) {
            return
        }
        viewModelScope.launch {
            isLoadingClubsResults = true
            refreshTime = REFRESH_TIME
            // compare the content of selectedClubs with previousSelectedClubs
            var tmpSelectedClubsResults = mutableListOf<RunnerResult>()
            val tmpSelectedClubs = selectedClubs.toList()
            selectedCompetition?.let { competition ->
                tmpSelectedClubs.forEach { clubName ->
                    if (tmpSelectedClubs.size == 1 &&  selectedClubsResults.size == 1) {
                        val result = LiveResultReq().getClubResults(competition.id, clubName, lastClubResultHash)
                        if (result.status == "OK") {
                            tmpSelectedClubsResults.addAll(result.results)
                        } else {
                            tmpSelectedClubsResults = selectedClubsResults.toMutableList()
                        }
                        lastClubResultHash = result.hash
                    } else {
                        val result = LiveResultReq().getClubResults(competition.id, clubName, "")
                        tmpSelectedClubsResults.addAll(result.results)
                        lastClubResultHash = ""
                    }
                    if (newClubFilter != activeClubFilter) {
                        // new club filtering is available, stop current result queries to restart a new result query
                        return@forEach
                    }
                }
            }
            selectedClubsResults = tmpSelectedClubsResults.toList()
            lastRefreshTime = java.time.LocalTime.now().toString()
            isLoadingClubsResults = false
        }
    }


    fun periodicClubResultRefreshTask(tickMS: Int = 100) {
        if (selectedCompetition == null
            || selectedCompetition?.isToday() == false
            || selectedClubs.isEmpty())
        {
            refreshTime = -1
            return
        }

        if (refreshTime < 0) {
            // loadClass results every REFRESH_TIME
            // to reduce network impact, just check if there are new resulsts before calling loadClassResults
            if (hasNewResults) {
                hasNewResults = false
                activeClubFilter = "" // force reload club result
            } else {
                checkNeedRefresh()
            }
            refreshTime = REFRESH_TIME
        } else {
            refreshTime -= max(tickMS/100, 1)
        }
    }

    fun getRefreshProgress() : Float {
        if (refreshTime < 0)
            return 0f
        else
            return 1f - refreshTime.toFloat()/REFRESH_TIME
    }

    // function which has less impact on the network
    private var lastPassingHash  by mutableStateOf("")
    private var hasNewResults by mutableStateOf(false)
    fun checkNeedRefresh() {
        viewModelScope.launch {
            val lastPassing = LiveResultReq().getLastPassing(selectedCompetitionId, lastPassingHash)
            lastPassingHash = lastPassing.hash
            hasNewResults = lastPassing.passings.isNotEmpty()
            if (lastPassing.status == "OK") {
                lastPassingList = lastPassing.passings
            }
            lastRefreshTime = java.time.LocalTime.now().toString()
        }
    }
}
