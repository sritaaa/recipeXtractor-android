package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.network.RecipeApi
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var extractButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultScrollView: ScrollView
    private lateinit var resultContainer: LinearLayout

    // ✅ Use RecipeApi directly (no Retrofit needed)
    private val api = RecipeApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        urlInput = findViewById(R.id.urlInput)
        extractButton = findViewById(R.id.extractButton)
        progressBar = findViewById(R.id.progressBar)
        resultScrollView = findViewById(R.id.resultScrollView)
        resultContainer = findViewById(R.id.resultContainer)

        extractButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Enter YouTube URL", Toast.LENGTH_SHORT).show()
            } else {
                extractRecipe(url)
            }
        }
    }

    private fun extractRecipe(input: String) {
        progressBar.visibility = View.VISIBLE
        extractButton.isEnabled = false
        resultContainer.removeAllViews()
        resultScrollView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isYouTubeUrl = input.contains("youtube.com") || input.contains("youtu.be")

                val response = if (isYouTubeUrl) {
                    // Call backend for YouTube URLs
                    api.extractRecipeFromYouTube(input)
                } else {
                    // Direct text processing
                    api.extractRecipeFromText(input)
                }

                withContext(Dispatchers.Main) {
                    displayRecipe(response.recipe, response.groceryList)
                }

            } catch (e: Exception) {
                Log.e("RecipeXtractor", "API error", e)
                withContext(Dispatchers.Main) {
                    showError(e.message ?: "Failed to extract recipe")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    extractButton.isEnabled = true
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun displayRecipe(recipe: com.example.myapplication.data.Recipe,
                              groceryList: com.example.myapplication.data.GroceryList) {
        resultContainer.removeAllViews()

        val title = TextView(this).apply {
            text = recipe.title
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        resultContainer.addView(title)

        val groceryHeader = TextView(this).apply {
            text = "\n🛒 Grocery List"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        resultContainer.addView(groceryHeader)

        groceryList.items.forEach { (category, items) ->
            val catView = TextView(this).apply {
                text = category
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            resultContainer.addView(catView)

            items.forEach {
                val itemView = TextView(this).apply {
                    text = "• ${it.amount} ${it.name}"
                }
                resultContainer.addView(itemView)
            }
        }

        val stepsHeader = TextView(this).apply {
            text = "\n📝 Instructions"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        resultContainer.addView(stepsHeader)

        recipe.instructions.forEachIndexed { i, step ->
            val stepView = TextView(this).apply {
                text = "${i + 1}. $step"
            }
            resultContainer.addView(stepView)
        }

        resultScrollView.visibility = View.VISIBLE
    }
}