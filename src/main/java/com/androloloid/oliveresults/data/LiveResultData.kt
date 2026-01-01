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

package com.androloloid.oliveresults.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

// This class represents the top-level JSON structure, which is an object containing a list of competitions.
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Competitions(
    var competitions: List<Competition>)
{
    fun sortForDisplay() {
        //remove competitions which are returning true for isInTheFuture() or  isNMonthOld()
        competitions = competitions.filter { !it.isInTheFuture() && !it.isNMonthOld(4) }
        // sort with the higher date first
        competitions = competitions.sortedByDescending { it.date }
    }
}

/* example of competition info
   "id" : 10279,
  "name" : "Demo #2",
  "organizer" : "TestOrganizer",
   "date" : "2012-06-02",
   "timediff" : 1,
   "multidaystage" : 1,
   "multidayfirstday" : 10278

  */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Competition(
    val id: Int,
    val name: String,
    val organizer: String,
    val date: String,
    val timediff: Int,
    val multidaystage: Int? = null,
    val multidayfirstday: Int? = null)
{
    // timediff represents number of hours that the timezone of the competition is + or - compared to Central European time (CET)
    // timediff = -1  : eventLocalTime = FranceTime - 1h  -> 10h local time = 11h CET (Paris)
    fun isInTheFuture(): Boolean {
        val today = LocalDate.now()
        val eventDate = LocalDate.parse(date)
        return eventDate.isAfter(today)
    }

    fun isToday(): Boolean {
        val today = LocalDate.now()
        val eventDate = LocalDate.parse(date)
        return eventDate.isEqual(today)
    }

    fun isNMonthOld(numMonths:Long=1) : Boolean {
        val today = LocalDate.now()
        val eventDate = LocalDate.parse(date)
        return eventDate.isBefore(today.minusMonths(numMonths))
    }
    fun eventDateToString() : String {
        val eventDate = LocalDate.parse(date)
        return eventDate.toString()
    }
}

/* example of lastPassing
{
     "status" : "OK", "passings" : [
     {   "passtime" : "10:05:23",
         "runnerName" : "TestRunner 1",
         "class" : "Men Elite",
         "control" : 1000,
         "controlName" : "Finish",
         "time" : 23430},
     {   "passtime" : "10:05:22",
         "runnerName" : "TestRunner 3",
         "class" : "Men Elite",
         "control" : 1000,
         "controlName" : "Finish",
         "time" : 23330}
   ],
   "hash" : "abcdef...."
}
 getLastPassing
{ "status": "OK", "passings" : [{"passtime": "11:39:22", "runnerName": "Alona OLIINYK", "class": "W40", "control": 1000, "controlName" : "", "time": "46:06" },{"passtime": "11:38:52", "runnerName": "class": "M55", "control": 1000, "controlName" : "", "time": "63:18" },{"passtime": "11:38:51", "runnerName": "Samuel SARANEN", "class": "M14", "control": 1000, "controlName" : "", "time": "34:20" }], "hash": "83c6b6e2f9aaf41e67730c28d0adbd33"}
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LastPassing(
    val status: String,
    val passings: List<Passing>,
    val hash: String)
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Passing(
    val passtime: String,
    val runnerName: String,
    val className: String,
    val control: Int,
    val controlName: String,
    val time: String
)

/* example of competitionClasses
{
 "status": "OK",
 "classes" : [
         {"className": "Öppen-1"},
         {"className": "Öppen-10"},
         ....
        {"className": "Öppen-8"}],
"hash": "84b1fdfe67a524a1580132baa174cce1"
}
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CompetitionClasses(
    val status: String,
    val classes: List<CompetitionClass>,
    val hash: String)
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CompetitionClass(
    val className: String)

/* example of classResult
{
   "status":"OK",
   "className":"Gul h",
   "splitcontrols": [
        { "code": 1110, "name": "(110)"},
        { "code": 2110, "name": "(110)"},
        { "code": 1139, "name": "(139)"}
    ],
   "results":[
      {
         "place":"1",
         "name":"Anton Mörkfors",
         "club":"Järfälla OK",
         "result":"17:02",
         "status":0,
         "timeplus":"+00:00",
         "progress":100,
         "start":6840000,
          "splits": {"1110": 118300,"1110_status": 0,"1110_place": 1,"1110_timeplus": 0,"2110": 324600,"2110_status": 0,"2110_place": 1,"2110_timeplus": 0,"1139": 684100,"1139_status": 0,"1139_place": 1,"1139_timeplus": 0}
      },
      {
         "place":"2",
         "name":"Leif Mörkfors",
         "club":"Järfälla OK",
         "result":"18:23",
         "status":0,
         "timeplus":"+01:21",
         "progress":100,
         "start":6840000
      },
      {
         "place":"3",
         "name":"Martin Kvarnefalk",
         "club":"Järfälla OK",
         "result":"21:07",
         "status":0,
         "timeplus":"+04:05",
         "progress":100,
         "start":6840000
      }
   ],
   "hash":"883fae6e4b8f0727b6ffabb7c403277c"
}

{
  "status": "OK",
   "clubName": "Bengans Orienteringsklubb",
    "results": [
            {
              "place": "1",
              "name": "Kalle Karlsson",
              "club": "Bengans Orienteringsklubb",
              "class": "H21",
              "result": "231600",
              "status" : 0,
              "timeplus": "0",
              "start": 3600000
            },
            {
              "place": "3",
               "name": "Junia Jönsson",
               "club": "Bengans Orienteringsklubb",
               "class": "D21",
               "result": "248700",
               "status" : 0,
               "timeplus": "17100",
               "start": 3636000
             }],
        "hash": "fa6508670a66f7022847645b49b353a6"
}
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClassResults(
    val status: String,
    val className: String,
    val splitcontrols: List<SplitControl>,
    val results: List<RunnerResult>,
    val hash: String)
{
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClubResults(
    val status: String,
    val clubName: String,
    val results: List<RunnerResult>,
    val hash: String)
{
}
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SplitControl(
    val code: Int,
    val name: String)

class Split(val code: String,
            val timeInt: Int,
            val time: String,
            val status: Int,
            val place: String,
            val timeplus: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class RunnerResult(
    private val place: String,
    private val name: String,
    val clubName: String,
    val className: String? = null,
    private val result: String,
    private val status: Long,
    private val timeplus: String,
    private val progress: Int? = null,
    private val start: Long,
    private val splits: Map<String, MyInt>? = null
) {
    private fun getTimeToString(tSeconds: Int): String {
        val hours = tSeconds / 3600
        val minutes = (tSeconds % 3600) / 60
        val seconds = tSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%2d:%02d", minutes, seconds)
        }
    }

    private fun getTimeFromString(t: String): String {
        val tSeconds = try {
            // The API can return time in centiseconds as a string, or an already formatted time string
            t.toInt() / 100
        } catch (e: NumberFormatException) {
            return t // It's already a formatted string like "17:02" or "+01:21"
        }
        return getTimeToString(tSeconds)
    }

    // time or status if status is not valid
    fun getResult(): String {
        try {
            if (status == 0L) {
                return getTimeFromString(result)
            } else {
                return RunnerResultStatus(status.toInt()).getStatusString()
            }
        } catch (e: Exception) {
            return "---"
        }
    }

    fun getStartTime(): String {
        return getTimeFromString(start.toString())
    }
    private fun getStartTimeCETseconds(competition: Competition?) : Long {
        val localStartSeconds = start/100 - (competition?.timediff?:0) * 3600
        return localStartSeconds
    }
    fun getTimeFromStart(competition: Competition?): String {
        val cetZone = ZoneId.of("CET")
        val cetTime = ZonedDateTime.now(cetZone).toLocalTime()
        val startTime = getStartTimeCETseconds(competition)
        //println("getStartTimeCETseconds: $startTime cetTime: $cetTime  ${cetTime.toSecondOfDay()}")
        if (startTime >= cetTime.toSecondOfDay()) { // TODO replace it with >=
            return ""
        }
        val runTime = cetTime.minusSeconds(startTime)

        return "("+getTimeToString(runTime.toSecondOfDay())+")"
    }

    // diff with first runner
    fun getTimePlus(): String {
        try {
            if (status == 0L) {
                return "+" + getTimeFromString(timeplus)
            } else {
                return ""
            }
        } catch (e: Exception) {
            return ""
        }
    }
    fun getRankingStr(): String {
        if (status == 0L) {
            return place
        } else {
            return RunnerResultStatus(status.toInt()).getStatusAbrev()
        }
    }
    fun getRanking(): Int {
        if (status == 0L) {
            try {
                return place.toInt()
            } catch(e: Exception) {
                return 10000
            }
        } else {
            // finisher first
            if (status == 5L) { // Out of Time
                return 10001
            } else if (status == 3L) { // Missing punch
                return 10002
            } else if (status == 2L) { // Did Not Finish
                return 10003
            } else if (status == 4L) { // Disqualified
                return 10004
            } else if (status == 9L || status == 10L) { // running later
                return 20000 + (start/100).toInt() // 20 000 + 86 400 = 106 400
            } else {
                // not started later
                return 110000 + status.toInt()
            }
        }
    }
    fun isRunningToday(andHasStartTime:Boolean = false): Boolean {
        val hasStartStatus = status == 9L || status == 10L
        val hasStartTime = start > 0
        if (andHasStartTime) {
            return hasStartStatus && hasStartTime
        } else {
            return hasStartStatus
        }
    }

    fun getStatus(): Int {
        return status.toInt()
    }

    fun getName(): String {
        // use upper case for first letter after space
        return name.replaceFirstChar({ if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }

    fun hasSplits() : Boolean { return splits != null && splits.isNotEmpty() }
    fun getNumSplits() : Int { return splits?.size ?: 0 }

    fun getSplits(splitcontrols: List<SplitControl>?) : List<Split> {
        //println("getSplits() called with splitcontrols.size = ${splits?.size}")
        var splitList = mutableListOf<Split>()
        if (splits != null && splitcontrols != null) {
            for (ctrl in splitcontrols) {
                //  "splitcontrols": [{ "code": 1110, "name": "(110)"},
                val code = ctrl.code.toString()
                // splits: [{  "1110": 118300,"1110_status": 0,"1110_place": 1,"1110_timeplus": 0,
                val splitResult = splits.get(code)?.invoke()
                val splitStatus = splits.get(code+"_status")?.invoke()
                val splitPlace = splits.get(code+"_place")?.invoke()
                val splitTimePlus = splits.get(code+"_timeplus")?.invoke()
                if (splitResult != null && splitStatus != null && splitPlace != null && splitTimePlus != null) {
                   // for (i in 1..15) {
                        splitList.add(
                            Split(
                                ctrl.name,
                                splitResult?:0,
                                getTimeFromString(splitResult.toString()),
                                splitStatus,
                                splitPlace.toString(),
                                getTimeFromString(splitTimePlus.toString())
                            )
                        )
                   // }
                }
            }
            splitList.sortBy { it.timeInt }
        }
        return splitList
    }

    /* status mapping from int to string
0 - OK
1 - DNS (Did Not Start)
2 - DNF (Did not finish)
3 - MP (Missing Punch)
4 - DSQ (Disqualified)
5 - OT (Over (max) time)
9 - Not Started Yet
10 - Not Started Yet
11 - Walk Over (Resigned before the race started)
12 - Moved up (The runner have been moved to a higher class)
 */
    class RunnerResultStatus(
        val status: Int
    ) {
        fun getStatusString(): String {
            when (status) {
                0 -> return "OK"
                1 -> return "DNS"
                2 -> return "DNF"
                3 -> return "MP"
                4 -> return "DSQ"
                5 -> return "OT"
                9 -> return "Not Started Yet"
                10 -> return "Not Started Yet"
                11 -> return "Walk Over"
                12 -> return "Moved up"
                else -> return "Unknown"
            }
        }
        fun getStatusAbrev(): String {
            when (status) {
                0 -> return "OK"
                1 -> return "DNS"
                2 -> return "DNF"
                3 -> return "MP"
                4 -> return "DSQ"
                5 -> return "OT"
                else -> return "--"
            }
        }
    }
}
