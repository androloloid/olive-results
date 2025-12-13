package com.androloloid.liveresult

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androloloid.liveresult.data.ClassResults
import com.androloloid.liveresult.data.Competition
import com.androloloid.liveresult.data.CompetitionClass
import com.androloloid.liveresult.data.CompetitionClasses
import com.androloloid.liveresult.data.Competitions
import com.androloloid.liveresult.data.LiveResultReq
import com.androloloid.liveresult.data.RunnerResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class CompetitionViewModel(application: Application) : AndroidViewModel(application) {
    private val REFRESH_TIME = 200 // *0.1s
    private var refreshTime by mutableStateOf(-1)

    private val sharedPreferences = application.getSharedPreferences("LiveResultPrefs", Context.MODE_PRIVATE)

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

    var classResults by mutableStateOf<ClassResults?>(null)
        private set

    var selectedClubs by mutableStateOf<MutableList<String>>(mutableListOf())
        private set
    var selectedClubsResults by mutableStateOf<List<RunnerResult>>(emptyList())
        private set
    var clubs by mutableStateOf<MutableList<String>>(mutableListOf())
        private set

    var runnersClass by mutableStateOf<MutableMap<String, String>>(mutableMapOf())
        private set

    fun init() {
        if (competitions.competitions.isEmpty()) {
            loadCompetitions()
        }
    }

    init {
        selectedCompetitionId = sharedPreferences.getInt("selectedCompetitionId", 0)
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
                isLoadingClubs = false
            }
        }
    }

    private fun loadClasses() {
        viewModelScope.launch {
            selectedCompetition?.let {
                competitionClasses = LiveResultReq().getClasses(it.id, "")
                if (competitionClasses.classes.isNotEmpty()) {
                    selectClass(competitionClasses.classes[0])
                }
            }
            //loadClubNames(selectedCompetition)
        }
    }

    fun selectClass(competitionClass: CompetitionClass) {
        isLoading = true
        selectedClass = competitionClass
        refreshTime = REFRESH_TIME
        loadClassResults()
        isLoading = false
    }

    fun loadClassResults() {
        viewModelScope.launch {
            selectedCompetition?.let { competition ->
                selectedClass?.let { competitionClass ->
                    classResults = LiveResultReq().getClassResults(competition.id, competitionClass.className, "")
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
        println("selectClubs filter=$filter")
        newClubFilter = filter
        // start thread calling clubResultThreadProcedure if not already running
        if (clubResultThread?.isActive != true) {
            println("selectClubs starting thread")
            clubResultThread = viewModelScope.launch {
                clubResultThreadProcedure()
            }
        }
    }

    suspend fun clubResultThreadProcedure() {
        while (true) {
            val textToSearch = newClubFilter
            if (activeClubFilter != textToSearch && !isLoadingClubsResults) {
                println("clubResultThreadProcedure textToSearch=$textToSearch")
                activeClubFilter = textToSearch
                selectClubsAndLoad(activeClubFilter)
            }
            delay(100)
        }
    }

    private fun selectClubsAndLoad(filter: String) {
        println("selectClubs filter=$filter")
        if (filter.isNotEmpty()) {
            selectedClubs.clear()
            for (clubName in clubs) {
                if (clubName.contains(filter, ignoreCase = true)) {
                    selectedClubs.add(clubName)
                }
            }
            loadClubResults()
        } else {
            selectedClubs.clear()
            selectedClubsResults = emptyList()
        }
    }

    private fun loadClubResults() {
        println("++++ loadClubResults")
        if (selectedClubs.isEmpty()) {
            return
        }
        viewModelScope.launch {
            isLoadingClubsResults = true
            refreshTime = REFRESH_TIME
            // compare the content of selectedClubs with previousSelectedClubs
            val tmpSelectedClubsResults = mutableListOf<RunnerResult>()
            val tmpSelectedClubs = selectedClubs.toList()
            selectedCompetition?.let { competition ->
                tmpSelectedClubs.forEach { clubName ->
                    //print(" load club results for clubName=$clubName")
                    val result = LiveResultReq().getClubResults(competition.id, clubName, "")
                    tmpSelectedClubsResults.addAll(result.results)
                    if (newClubFilter != activeClubFilter) {
                        return@forEach
                    }
                }
            }
            selectedClubsResults = tmpSelectedClubsResults.toList()
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
            activeClubFilter = "" // force reload result
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
}
