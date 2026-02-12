package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.network.RecipeApi
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var extractButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultScrollView: ScrollView
    private lateinit var resultContainer: LinearLayout

    private val api = RecipeApi()

    private lateinit var prefs: SharedPreferences
    private val FREE_EXTRACTIONS_LIMIT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        urlInput = findViewById(R.id.urlInput)
        extractButton = findViewById(R.id.extractButton)
        progressBar = findViewById(R.id.progressBar)
        resultScrollView = findViewById(R.id.resultScrollView)
        resultContainer = findViewById(R.id.resultContainer)

        prefs = getSharedPreferences("RecipeApp", MODE_PRIVATE)

        extractButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Enter YouTube URL", Toast.LENGTH_SHORT).show()
            } else {
                checkAccessAndExtract(url)
            }
        }
    }

    private fun checkAccessAndExtract(input: String) {
        Purchases.sharedInstance.getCustomerInfoWith(
            onSuccess = { customerInfo ->
                val allEntitlements = customerInfo.entitlements.all
                val premiumEntitlement = allEntitlements["Premium"]
                val isPremium = premiumEntitlement?.isActive == true

                if (isPremium) {
                    extractRecipe(input)
                } else {
                    val usedExtractions = prefs.getInt("extraction_count", 0)

                    if (usedExtractions < FREE_EXTRACTIONS_LIMIT) {
                        prefs.edit().putInt("extraction_count", usedExtractions + 1).apply()
                        extractRecipe(input)
                        Toast.makeText(this, "Free trial used! Subscribe for unlimited recipes", Toast.LENGTH_LONG).show()
                    } else {
                        showPaywall()
                    }
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showPaywall() {
        Purchases.sharedInstance.getOfferingsWith(
            onSuccess = { offerings ->
                val packageToPurchase = offerings.current?.availablePackages?.firstOrNull()

                if (packageToPurchase != null) {
                    Purchases.sharedInstance.purchaseWith(
                        purchaseParams = PurchaseParams.Builder(this, packageToPurchase).build(),
                        onSuccess = { _, customerInfo ->
                            val allEntitlements = customerInfo.entitlements.all
                            val premiumEntitlement = allEntitlements["Premium"]
                            if (premiumEntitlement?.isActive == true) {
                                Toast.makeText(this, "Welcome to Premium! 🎉", Toast.LENGTH_LONG).show()
                            }
                        },
                        onError = { error, _ ->
                            Toast.makeText(this, "Purchase cancelled", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(this, "Subscribe to continue extracting recipes!", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
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
                    api.extractRecipeFromYouTube(input)
                } else {
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
