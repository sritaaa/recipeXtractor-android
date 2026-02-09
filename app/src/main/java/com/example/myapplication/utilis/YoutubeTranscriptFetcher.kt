package com.example.myapplication.utilis

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object YouTubeTranscriptFetcher {

    private val client = OkHttpClient()

    suspend fun getTranscript(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val videoId = YouTubeExtractor.extractVideoId(videoUrl)

            if (videoId == null) {
                Log.e("YouTubeTranscript", "Could not extract video ID")
                return@withContext null
            }

            Log.d("YouTubeTranscript", "Fetching transcript for video: $videoId")

            // Fetch the YouTube page HTML
            val pageUrl = "https://www.youtube.com/watch?v=$videoId"
            val request = Request.Builder()
                .url(pageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e("YouTubeTranscript", "Failed to fetch YouTube page: ${response.code}")
                return@withContext null
            }

            val html = response.body?.string() ?: return@withContext null

            // Extract caption tracks from the page
            val captionTrackRegex = """"captionTracks":\[(.*?)\]""".toRegex()
            val match = captionTrackRegex.find(html)

            if (match == null) {
                Log.e("YouTubeTranscript", "No captions found - video may not have subtitles")
                return@withContext null
            }

            val captionsJson = "[${match.groupValues[1]}]"
            Log.d("YouTubeTranscript", "Found caption data")

            val captions = JSONArray(captionsJson)

            if (captions.length() == 0) {
                Log.e("YouTubeTranscript", "No caption tracks available")
                return@withContext null
            }

            // Get the first English caption track
            var captionUrl: String? = null
            for (i in 0 until captions.length()) {
                val caption = captions.getJSONObject(i)
                val languageCode = caption.optString("languageCode", "")
                if (languageCode.startsWith("en")) {
                    captionUrl = caption.getString("baseUrl")
                    break
                }
            }

            // If no English, just take the first one
            if (captionUrl == null) {
                captionUrl = captions.getJSONObject(0).getString("baseUrl")
            }

            Log.d("YouTubeTranscript", "Fetching caption file...")

            // Fetch the actual caption XML
            val captionRequest = Request.Builder()
                .url(captionUrl)
                .build()

            val captionResponse = client.newCall(captionRequest).execute()

            if (!captionResponse.isSuccessful) {
                Log.e("YouTubeTranscript", "Failed to fetch captions: ${captionResponse.code}")
                return@withContext null
            }

            val captionXml = captionResponse.body?.string() ?: return@withContext null

            Log.d("YouTubeTranscript", "Parsing caption XML...")

            // Extract text from XML
            val textRegex = """<text[^>]*>(.*?)</text>""".toRegex()
            val transcript = textRegex.findAll(captionXml)
                .map { it.groupValues[1] }
                .map { decodeHtml(it) }
                .joinToString(" ")
                .trim()

            if (transcript.isEmpty()) {
                Log.e("YouTubeTranscript", "Transcript is empty")
                return@withContext null
            }

            Log.d("YouTubeTranscript", "Successfully extracted transcript (${transcript.length} chars)")
            Log.d("YouTubeTranscript", "Preview: ${transcript.take(200)}...")

            transcript

        } catch (e: Exception) {
            Log.e("YouTubeTranscript", "Error extracting transcript", e)
            null
        }
    }

    private fun decodeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("<br>", "\n")
            .trim()
    }
}