package com.androloloid.liveresult.data

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
        // if in the string there are more than 2 " characters between : and , then remove the additionals " characters
        return result
    }

    suspend fun getCompetitions(): Competitions {
        /// Returns a list of competitions that are available
        var jsonString = liveResultRequest("api.php?method=getcompetitions")
        var result = Competitions(emptyList())
        // if jsonString is null, return an empty list of competitions
        if (jsonString == null) {
            return result
        }
        try {
            println(jsonString)
            //saveJsonToFile(jsonString, "competitions.json")
            jsonString = fixJsonStringBeforeDecode(jsonString)
            // Create a Json instance that ignores unknown keys in the JSON input.
            // This makes parsing more robust if the API adds new fields in the future.
            val format = Json { ignoreUnknownKeys = true }
            result =
                Competitions(format.decodeFromString<Competitions>(jsonString).competitions)
            /*println("Competitions:")
            result.competitions.forEach {
                println("  "+it.name)
            }*/
        } catch (e: Exception) {
            e.printStackTrace()
            return result
        }
        return result
    }

    suspend fun getCompetitionInfo(competitionId: Int): Competition {
        /// Return information about a single competition
        var jsonString = liveResultRequest("api.php?method=getcompetitioninfo&comp="+competitionId)
        // if jsonString is null, return an emptycompetition
        if (jsonString == null) {
            return Competition(-1, "", "", "", 0)
        }
        //println(jsonString)
        // Create a Json instance that ignores unknown keys in the JSON input.
        // This makes parsing more robust if the API adds new fields in the future.
        jsonString = fixJsonStringBeforeDecode(jsonString)
        val format = Json { ignoreUnknownKeys = true }
        val competition = format.decodeFromString<Competition>(jsonString)
        println("CompetitionInfo:" + competition.name)
        return competition
    }

    suspend fun getLastPassing(competitionId:Int, lastHash:String) : LastPassing {
        var jsonString = liveResultRequest("api.php?method=getlastpassings&comp="+competitionId+"&last_hash="+lastHash)
        if (jsonString == null) {
            return LastPassing("Error", emptyList(), "")
        }
        println("getPassing:" + jsonString)
        jsonString = fixJsonStringBeforeDecode(jsonString)
        val format  = Json { ignoreUnknownKeys = true }
        val result = format.decodeFromString<LastPassing>(jsonString)
        return result
    }
    suspend fun getClasses(competitionId:Int, lastHash:String) : CompetitionClasses {
        //api.php?method=getclasses&comp=XXXX&last_hash=abcdefg
        var jsonString =
            liveResultRequest("api.php?method=getclasses&comp=" + competitionId + "&last_hash=" + lastHash)
        if (jsonString == null) {
            return CompetitionClasses("Error", emptyList(), "")
        }
        println("getClasses:" + jsonString)
        jsonString = fixJsonStringBeforeDecode(jsonString)
        val format = Json { ignoreUnknownKeys = true }
        val competitionClasses = format.decodeFromString<CompetitionClasses>(jsonString)
        println("Classes:")
        competitionClasses.classes.forEach {
            println("  "+it.className)
        }
        return competitionClasses
    }
    suspend fun getClassResults(competitionId:Int, className:String, lastHash:String) : ClassResults {
        //api.php?method=getclassresults&comp=10259&unformattedTimes=true&class=Öppen-1
        var jsonString =
            liveResultRequest("api.php?method=getclassresults&comp=" + competitionId + "&unformattedTimes=true&class=" + className + "&last_hash=" + lastHash)
        if (jsonString == null) {
            return ClassResults("Error", "", emptyList(), emptyList(), hash="")
        }
        // replace the string "class:" by "className:" in the jsonString
        jsonString = fixJsonStringBeforeDecode(jsonString)
        println("getClassResults:" + jsonString)
        val format = Json { ignoreUnknownKeys = true }
        val result = format.decodeFromString<ClassResults>(jsonString)
        return result
    }
    suspend fun getClubResults(competitionId:Int, clubName:String, lastHash:String) : ClubResults {
        //api.php?method=getclubresults&comp=10259&unformattedTimes=true&club=Öppen
        var jsonString =
            liveResultRequest("api.php?method=getclubresults&comp=" + competitionId + "&unformattedTimes=true&club=" + clubName + "&last_hash=" + lastHash)
        if (jsonString == null) {
            return ClubResults("Error", "", emptyList(),  hash="")
        }
        jsonString = fixJsonStringBeforeDecode(jsonString)
        println("getClubResults:" + jsonString)
        val format = Json { ignoreUnknownKeys = true }
        val result = format.decodeFromString<ClubResults>(jsonString)
        return result

    }


}
/*
class JsonInt32Converter : JsonConverter<int>
{
    public override bool CanConvert(Type typeToConvert)
    {
        return typeToConvert == typeof(int);
    }

    public override int Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        try
        {
            var value = reader.GetInt32();
            return value;
        }
        catch
        {
            return 0;
        }
    }

    public override void Write(Utf8JsonWriter writer, int value, JsonSerializerOptions options)
    {
        throw new NotImplementedException();
    }
}*/