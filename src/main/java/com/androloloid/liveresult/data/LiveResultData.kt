package com.androloloid.liveresult.data

import android.annotation.SuppressLint
import androidx.compose.ui.text.capitalize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.util.Locale

// This class represents the top-level JSON structure, which is an object containing a list of competitions.
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Competitions(
    var competitions: List<Competition>)
{
    fun sortForDisplay() {
        //remove competitions which are returning true for isInTheFuture() or  isNMonthOld()
        competitions = competitions.filter { !it.isInTheFuture() && !it.isNMonthOld() }
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
    val time: Int
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

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ClubResults(
    val status: String,
    val clubName: String,
    val results: List<RunnerResult>,
    val hash: String)
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SplitControl(
    val code: Int,
    val name: String)

class Split(val code: String,
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
    private fun getTimeFromString(t: String): String {
        val tSeconds = try {
            // The API can return time in centiseconds as a string, or an already formatted time string
            t.toLong() / 100
        } catch (e: NumberFormatException) {
            return t // It's already a formatted string like "17:02" or "+01:21"
        }

        val hours = tSeconds / 3600
        val minutes = (tSeconds % 3600) / 60
        val seconds = tSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%2d:%02d", minutes, seconds)
        }
    }

    // time or status if status is not valid
    fun getResult(language: String = "en"): String {
        try {
            if (status == 0L) {
                return getTimeFromString(result)
            } else {
                return RunnerResultStatus(status.toInt()).getStatusString(language)
            }
        } catch (e: Exception) {
            return "---"
        }
    }

    fun getStartTime(): String {
        val hours = start / 3600
        val minutes = (start % 3600) / 60
        val seconds = start % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
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
    fun getPlace(language: String = "en"): String {
        if (status == 0L) {
            return place
        } else {
            return RunnerResultStatus(status.toInt()).getStatusAbrev(language)
        }
    }
    fun getName(): String {
        // use upper case for first letter after space
        return name.replaceFirstChar({ if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }

    fun hasSplits() : Boolean { return splits != null && splits.isNotEmpty() }

    fun getSplits(splitcontrols: List<SplitControl>?) : List<Split> {
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
                    splitList.add(Split(ctrl.name,
                        getTimeFromString(splitResult.toString()),
                        splitStatus,
                        splitPlace.toString(),
                        getTimeFromString(splitTimePlus.toString())))
                }
            }
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
        fun getStatusString(language: String = "en"): String {
            when (language) {
                "fr" -> {
                    when (status) {
                        0 -> return "OK"
                        1 -> return "Abs"
                        2 -> return "Abd"
                        3 -> return "PM"
                        4 -> return "Disq."
                        5 -> return "Temps Max"
                        9 -> return "En course"
                        10 -> return "En course"
                        11 -> return "Abs"
                        12 -> return "Abs"
                        else -> return "???"
                    }
                }

                else -> {
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
            }
        }
        fun getStatusAbrev(language: String = "en"): String {
            when (language) {
                "fr" -> {
                    when (status) {
                        0 -> return "OK"
                        1 -> return "Abs"
                        2 -> return "Abd"
                        3 -> return "PM"
                        4 -> return "Dsq"
                        else -> return "--"
                    }
                }

                else -> {
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
    }
}
