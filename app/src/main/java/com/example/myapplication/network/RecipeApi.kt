package com.example.myapplication.network

import android.util.Log
import com.example.myapplication.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RecipeApi {

    // ✅ INCREASED TIMEOUTS FOR LONG VIDEOS
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)  // 5 MINUTES for long videos!
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey = "AIzaSyAK7fCufbHme4GP4nYzv7Irx8PjVYX14kg"

    private val backendUrl = "http://10.81.66.215:8080"

    suspend fun extractRecipeFromYouTube(youtubeUrl: String): RecipeResponse =
        withContext(Dispatchers.IO) {

            val transcriptResult = getTranscriptFromBackend(youtubeUrl)

            when (transcriptResult) {
                is TranscriptResult.Success -> {
                    Log.d("RecipeApi", "Transcript fetched (${transcriptResult.text.length} chars)")
                    extractRecipeFromText(transcriptResult.text)
                }

                is TranscriptResult.Error -> {
                    Log.e("RecipeApi", "Transcript error: ${transcriptResult.message}")
                    throw Exception(transcriptResult.message)
                }
            }
        }

    // 🔽 SEALED RESULT (NO MORE NULLS)
    private sealed class TranscriptResult {
        data class Success(val text: String) : TranscriptResult()
        data class Error(val message: String) : TranscriptResult()
    }

    private suspend fun getTranscriptFromBackend(youtubeUrl: String): TranscriptResult =
        withContext(Dispatchers.IO) {
            try {
                val requestBody = JSONObject().apply {
                    put("url", youtubeUrl)
                }

                val body = requestBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$backendUrl/get-transcript")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("RecipeApi", "Calling backend: $backendUrl/get-transcript")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    return@withContext TranscriptResult.Error(
                        "Backend failed (${response.code})"
                    )
                }

                val json = JSONObject(responseBody)

                if (!json.optBoolean("success", false)) {
                    return@withContext TranscriptResult.Error(
                        json.optString("error", "Unknown transcript error")
                    )
                }

                TranscriptResult.Success(json.getString("transcript"))

            } catch (e: Exception) {
                TranscriptResult.Error("Network error: ${e.message}")
            }
        }

    // ================= GEMINI PART (UNCHANGED, BUT SAFE) =================

    suspend fun extractRecipeFromText(recipeText: String): RecipeResponse =
        withContext(Dispatchers.IO) {

            val prompt = """
            Here is recipe information:
            
            $recipeText
            
            Please extract the recipe and return it in the following STRICT JSON format (no markdown, no code blocks, ONLY JSON):
            {
              "title": "Recipe Name",
              "instructions": ["step1", "step2", "step3"],
              "grocery_list": {
                "Vegetables": [{"name": "ingredient", "amount": "quantity"}],
                "Proteins": [{"name": "ingredient", "amount": "quantity"}],
                "Dairy": [{"name": "ingredient", "amount": "quantity"}],
                "Pantry": [{"name": "ingredient", "amount": "quantity"}]
              }
            }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val body = requestBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .post(body)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Gemini API failed (${response.code})")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty Gemini response")

            parseGeminiResponse(responseBody)
        }

    private fun parseGeminiResponse(raw: String): RecipeResponse {

        val text = JSONObject(raw)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val cleanedText = text
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val json = JSONObject(cleanedText)

        val recipe = Recipe(
            title = json.getString("title"),
            instructions = json.getJSONArray("instructions").let { arr ->
                List(arr.length()) { arr.getString(it) }
            }
        )

        val groceryJson = json.getJSONObject("grocery_list")
        val groceryMap = mutableMapOf<String, List<GroceryItem>>()

        groceryJson.keys().forEach { category ->
            val items = groceryJson.getJSONArray(category)
            groceryMap[category] = List(items.length()) {
                val obj = items.getJSONObject(it)
                GroceryItem(
                    name = obj.getString("name"),
                    amount = obj.getString("amount")
                )
            }
        }

        return RecipeResponse(recipe, GroceryList(groceryMap))
    }
}