package com.example.myapplication.utilis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeExtractor {

    fun extractVideoId(url: String): String? {
        val regex = "(?:v=|/)([0-9A-Za-z_-]{11})".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    suspend fun buildPrompt(videoUrl: String): String = withContext(Dispatchers.IO) {
        // Try to get the actual transcript
        val transcript = YouTubeTranscriptFetcher.getTranscript(videoUrl)

        if (transcript != null) {
            // We have the transcript! Use it
            """
            Here is a transcript from a cooking video:
            
            $transcript
            
            Please extract the recipe from this transcript and return it in the following STRICT JSON format (no markdown, no code blocks, ONLY JSON):
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
        } else {
            // Fallback: No transcript available, use test data
            """
            Here is a recipe for Spaghetti Carbonara:
            
            Ingredients:
            - 400g spaghetti
            - 200g pancetta or guanciale
            - 4 large eggs
            - 100g Parmesan cheese
            - Black pepper
            - Salt
            
            Instructions:
            1. Boil pasta in salted water
            2. Fry pancetta until crispy
            3. Beat eggs with grated Parmesan
            4. Drain pasta, mix with pancetta
            5. Remove from heat, add egg mixture
            6. Toss quickly, season with pepper
            
            Please extract this recipe and return it in the following STRICT JSON format (no markdown, no code blocks, ONLY JSON):
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
        }
    }
}