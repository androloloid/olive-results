package com.androloloid.liveresult

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androloloid.liveresult.data.ClassResults
import com.androloloid.liveresult.data.Competition
import com.androloloid.liveresult.data.CompetitionClass
import com.androloloid.liveresult.data.CompetitionClasses
import com.androloloid.liveresult.data.Competitions
import com.androloloid.liveresult.data.LiveResultReq
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class CompetitionViewModel : ViewModel() {
    private val REFRESH_TIME = 200 // *0.1s
    private var refreshTime by mutableStateOf(-1)

    var competitions by mutableStateOf(Competitions(emptyList()))
        private set

    var isLoading by mutableStateOf(false)
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

    fun init() {
        if (competitions.competitions.isEmpty()) {
            loadCompetitions()
        }
    }

    fun loadCompetitions() {
        viewModelScope.launch {
            isLoading = true
            competitions = LiveResultReq().getCompetitions()
            competitions.sortForDisplay()
            isLoading = false
        }
    }

    fun selectCompetition(competition: Competition) {
        isLoading = true
        selectedCompetition = competition
        selectedCompetitionId = competition.id
        selectedClass = null
        competitionClasses = CompetitionClasses("OK", emptyList(), "")
        classResults = null
        loadClasses()
        isLoading = false
    }

    private fun loadClasses() {
        viewModelScope.launch {
            selectedCompetition?.let {
                competitionClasses = LiveResultReq().getClasses(it.id, "")
                if (competitionClasses.classes.isNotEmpty()) {
                    selectClass(competitionClasses.classes[0])
                }
            }
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
    fun getRefreshProgress() : Float {
        if (refreshTime < 0)
            return 0f
        else
            return 1f - refreshTime.toFloat()/REFRESH_TIME
    }
}