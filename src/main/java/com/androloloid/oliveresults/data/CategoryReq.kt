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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * CategoryReq handles fetching and caching runner categories from the server.
 */
class CategoryReq {
    private val client = OkHttpClient()
    private val url = "http://androloloid.free.fr/oliveresults/ffco_licenses/getCategory.php"

    // categoryMap return the category for a given runner name for the active year. It is invalid
    // if the year change
    // Use a nullable String to cache "null" (not found) results and avoid unnecessary network calls
    private val categoryMap = mutableMapOf<String, String?>()

    private var year = 2026

    /**
     * Returns the category for a given runner name from the local cache.
     * Returns null if not found or if the runner has no category.
     */
    fun getCategory(name: String): String? {
        return categoryMap[name]
    }

    fun setYear(year: Int) {
        if (this.year == year) return
        categoryMap.clear()
        this.year = year
    }

    /**
     * Queries the server for a list of runner names that are not already in the local cache.
     * Updates the cache with the results and returns the updated map.
     */
    suspend fun fetchCategories(names: List<String>): Map<String, String?> {
        val namesToFetch = names.filter { !categoryMap.containsKey(it) }
        if (namesToFetch.isEmpty()) return categoryMap

        val formBuilder = FormBody.Builder()
        for (name in namesToFetch) {
            formBuilder.add("names[]", name)
        }
        formBuilder.add("year", year.toString())
        val formBody = formBuilder.build()

        println("Fetching categories for $namesToFetch")
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string() ?: "[]"
                        
                        val format = Json { 
                            ignoreUnknownKeys = true 
                            explicitNulls = false
                        }

                        val categories = format.decodeFromString<List<String?>>(jsonString)
                        
                        namesToFetch.zip(categories).forEach { (name, category) ->
                            categoryMap[name] = category
                        }
                        println("Fetched categories: $categories")
                    } else {
                        println("Error fetching categories: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            categoryMap
        }
    }
}
