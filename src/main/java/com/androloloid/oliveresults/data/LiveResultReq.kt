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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class LiveResultReq {
    private suspend fun liveResultRequest(cmd: String): String? {
        val url = "https://liveresultat.orientering.se/" + cmd

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    // print the response in the console and return it
                    response.body?.string()
                } else {
                    // You can handle errors here, for example, by logging or returning a specific error message.
                    println("Error: ${response.code}")
                    null
                }
            } catch (e: IOException) {
                // Handle exceptions like no network connection
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveJsonToFile(jsonString: String, filename: String) {
        val file = java.io.File(filename)
        file.writeText(jsonString)
    }

    private fun fixJsonStringBeforeDecode(jsonString: String): String {
        // Replace the string "class:" by "className:" in the jsonString
        var result = jsonString
        result = result.replace("\"class\":", "\"className\":")
        result = result.replace("\"club\":", "\"clubName\":")
        result = result.replace("\"start\": \"\"", "\"start\": 0")
        return result
    }

    suspend fun getCompetitions(): Competitions {
        try {
            /// Returns a list of competitions that are available
            var jsonString = liveResultRequest("api.php?method=getcompetitions")
            // if jsonString is null, return an empty list of competitions
            if (jsonString == null) {
                return Competitions(emptyList())
            }
            //println("getCompetitions")
            //println(jsonString)
            //saveJsonToFile(jsonString, "competitions.json")
            jsonString = fixJsonStringBeforeDecode(jsonString)
            // Create a Json instance that ignores unknown keys in the JSON input.
            // This makes parsing more robust if the API adds new fields in the future.
            val format = Json { ignoreUnknownKeys = true }
            val result =
                Competitions(format.decodeFromString<Competitions>(jsonString).competitions)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return Competitions(emptyList())
        }
    }

    suspend fun getCompetitionInfo(competitionId: Int): Competition {
        try {
            /// Return information about a single competition
            var jsonString = liveResultRequest("api.php?method=getcompetitioninfo&comp="+competitionId)
            // if jsonString is null, return an emptycompetition
            if (jsonString == null) {
                return Competition(-1, "", "", "", 0)
            }
            //println("getCompetitionInfo")
            //println(jsonString)
            // Create a Json instance that ignores unknown keys in the JSON input.
            // This makes parsing more robust if the API adds new fields in the future.
            jsonString = fixJsonStringBeforeDecode(jsonString)
            val format = Json { ignoreUnknownKeys = true }
            val competition = format.decodeFromString<Competition>(jsonString)
            return competition
        } catch (e: Exception) {
            e.printStackTrace()
            return Competition(-1, "", "", "", 0)
        }
    }

    suspend fun getLastPassing(competitionId:Int, lastHash:String) : LastPassing {
        try {
            var jsonString = liveResultRequest("api.php?method=getlastpassings&comp="+competitionId+"&last_hash="+lastHash)
            if (jsonString == null) {
                return LastPassing("Error", emptyList(), "")
            }
            //println("getLastPassing")
            //println(jsonString)
            jsonString = fixJsonStringBeforeDecode(jsonString)
            if (jsonString.contains("\"status\": \"NOT MODIFIED\"")) {
                return LastPassing("NOT MODIFIED", emptyList(), lastHash)
            }
            val format  = Json { ignoreUnknownKeys = true }
            val result = format.decodeFromString<LastPassing>(jsonString)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return LastPassing("Error", emptyList(), "")
        }
    }
    suspend fun getClasses(competitionId:Int, lastHash:String) : CompetitionClasses {
        try {
            //api.php?method=getclasses&comp=XXXX&last_hash=abcdefg
            var jsonString =
                liveResultRequest("api.php?method=getclasses&comp=" + competitionId + "&last_hash=" + lastHash)
            if (jsonString == null) {
                return CompetitionClasses("Error", emptyList(), "")
            }
            //println("getClasses")
            //println(jsonString)
            jsonString = fixJsonStringBeforeDecode(jsonString)
            if (jsonString.contains("\"status\": \"NOT MODIFIED\"")) {
                return CompetitionClasses("NOT MODIFIED", emptyList(), lastHash)
            }
            val format = Json { ignoreUnknownKeys = true }
            val competitionClasses = format.decodeFromString<CompetitionClasses>(jsonString)
            return competitionClasses
        } catch (e: Exception) {
            e.printStackTrace()
            return CompetitionClasses("Error", emptyList(), "")
        }
    }
    suspend fun getClassResults(competitionId:Int, className:String, lastHash:String) : ClassResults {
        try {
            //api.php?method=getclassresults&comp=10259&unformattedTimes=true&class=Öppen-1
            val urlClassName = java.net.URLEncoder.encode(className, "UTF-8")
            var jsonString =
                liveResultRequest("api.php?method=getclassresults&comp=" + competitionId + "&unformattedTimes=true&class=" + urlClassName + "&last_hash=" + lastHash)
            if (jsonString == null) {
                return ClassResults("Error", "", emptyList(), emptyList(), hash = "")
            }
            //println("getClassResults")
            //println(jsonString)
            // replace the string "class:" by "className:" in the jsonString
            jsonString = fixJsonStringBeforeDecode(jsonString)
            if (jsonString.contains("\"status\": \"NOT MODIFIED\"")) {
                return ClassResults("NOT MODIFIED", "", emptyList(), emptyList(), hash = "")
            }
            val format = Json { ignoreUnknownKeys = true }
            val result = format.decodeFromString<ClassResults>(jsonString)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return ClassResults("Error", "", emptyList(), emptyList(), hash = "")
        }
    }
    suspend fun getClubResults(competitionId:Int, clubName:String, lastHash:String) : ClubResults {
        try {
            //api.php?method=getclubresults&comp=10259&unformattedTimes=true&club=Öppen
            // convert clubname to url parameter and replace special characters
            val urlClubName = java.net.URLEncoder.encode(clubName, "UTF-8")
            var jsonString =
                liveResultRequest("api.php?method=getclubresults&comp=" + competitionId + "&unformattedTimes=true&club=" + urlClubName + "&last_hash=" + lastHash)
            if (jsonString == null) {
                return ClubResults("Error", "", emptyList(), hash = "")
            }
            //println("getClubResults")
            //println(jsonString)
            jsonString = fixJsonStringBeforeDecode(jsonString)
            if (jsonString.contains("\"status\": \"NOT MODIFIED\"")) {
                return ClubResults("NOT MODIFIED", "", emptyList(), hash = "")
            }
            val format = Json { ignoreUnknownKeys = true }
            val result = format.decodeFromString<ClubResults>(jsonString)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return ClubResults("Error", "", emptyList(), hash = "")
        }

    }
}