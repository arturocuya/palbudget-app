package com.example.palbudget.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ReceiptItem(
    val name: String,
    val price: Int // in cents
)

data class ReceiptAnalysis(
    val items: List<ReceiptItem>,
    val category: String,
    val finalPrice: Int // in cents
)

data class ImageAnalysis(
    val imageIndex: Int,
    val imageUri: String,
    val isReceipt: Boolean,
    val analysis: ReceiptAnalysis?
)

data class AnalysisResult(
    val success: Boolean,
    val results: List<ImageAnalysis> = emptyList(),
    val error: String? = null
)

class OpenAIService(private val context: Context) {
    
    companion object {
        private const val TAG = "OpenAIService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        // TODO: Set the actual OpenAI API key
        private const val API_KEY = ""
    }
    
    suspend fun analyzeReceipts(imageBase64List: List<String>, originalUris: List<String>): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            if (API_KEY.isEmpty()) {
                return@withContext AnalysisResult(
                    success = false,
                    results = emptyList(),
                    error = "OpenAI API key not configured"
                )
            }
            
            val basePrompt = loadPromptFromAssets()
            val fullPrompt = appendImageUrisToPrompt(basePrompt, originalUris)
            val requestBody = buildRequestBody(fullPrompt, imageBase64List)
            
            Log.d(TAG, "Processing ${originalUris.size} images:")
            originalUris.forEachIndexed { index, uri ->
                Log.d(TAG, "Image $index: $uri")
            }
            
            val connection = URL(OPENAI_API_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            
            // Send request
            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.toByteArray())
            }
            
            val responseCode = connection.responseCode
            val responseBody = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            }
            
            Log.d(TAG, "Response code: $responseCode")
            Log.d(TAG, "Response body: $responseBody")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Full successful response: $responseBody")
                parseSuccessResponse(responseBody, originalUris)
            } else {
                AnalysisResult(
                    success = false,
                    results = emptyList(),
                    error = "API Error ($responseCode): $responseBody"
                )
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            AnalysisResult(
                success = false,
                results = emptyList(),
                error = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            AnalysisResult(
                success = false,
                results = emptyList(),
                error = "Unexpected error: ${e.message}"
            )
        }
    }
    
    private fun loadPromptFromAssets(): String {
        return try {
            context.assets.open("prompts/receipt_analysis.md").bufferedReader().readText()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load prompt from assets, using default", e)
            "Analyze these receipt images and provide insights. u like jazz? (eyes you)"
        }
    }
    
    private fun appendImageUrisToPrompt(basePrompt: String, imageUris: List<String>): String {
        val uriList = imageUris.mapIndexed { index, uri ->
            val shortId = "IMG_${index}_${uri.hashCode().toString().takeLast(8)}"
            "${index + 1}. Image ${index + 1} (ID: $shortId): ${uri.takeLast(30)}"
        }.joinToString("\n")
        
        return "$basePrompt\n$uriList"
    }
    
    private fun buildRequestBody(prompt: String, imageUris: List<String>): String {
        val messages = JSONArray()
        val userMessage = JSONObject()
        val content = JSONArray()
        
        // Add text prompt
        content.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        
        // Add images (assuming they are base64 encoded)
        imageUris.forEach { imageUri ->
            content.put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", imageUri) // This should be base64 data URI or URL
                })
            })
        }
        
        userMessage.put("role", "user")
        userMessage.put("content", content)
        messages.put(userMessage)
        
        val requestBody = JSONObject()
        requestBody.put("model", "gpt-4o")
        requestBody.put("temperature", 0)
        requestBody.put("messages", messages)
        
        // Structured output schema
        val responseFormat = JSONObject()
        responseFormat.put("type", "json_schema")
        
        val jsonSchema = JSONObject()
        jsonSchema.put("name", "ReceiptAnalysisResponse")
        jsonSchema.put("strict", true)
        
        val schema = JSONObject()
        schema.put("type", "object")
        
        val properties = JSONObject()
        val resultsProperty = JSONObject()
        resultsProperty.put("type", "array")
        
        val itemsSchema = JSONObject()
        itemsSchema.put("type", "object")
        
        val itemProperties = JSONObject()
        // image_index
        val imageIndexProperty = JSONObject()
        imageIndexProperty.put("type", "integer")
        itemProperties.put("image_index", imageIndexProperty)
        
        // image_uri
        val imageUriProperty = JSONObject()
        imageUriProperty.put("type", "string")
        itemProperties.put("image_uri", imageUriProperty)
        
        // is_receipt
        val isReceiptProperty = JSONObject()
        isReceiptProperty.put("type", "boolean")
        itemProperties.put("is_receipt", isReceiptProperty)
        
        // analysis (required field, but can be null when is_receipt is false)
        val analysisProperty = JSONObject()
        analysisProperty.put("type", JSONArray().put("object").put("null"))
        
        val analysisProperties = JSONObject()
        
        // items array
        val itemsArrayProperty = JSONObject()
        itemsArrayProperty.put("type", "array")
        val itemObjectSchema = JSONObject()
        itemObjectSchema.put("type", "object")
        val itemObjectProperties = JSONObject()
        val nameProperty = JSONObject()
        nameProperty.put("type", "string")
        val priceProperty = JSONObject()
        priceProperty.put("type", "integer")
        itemObjectProperties.put("name", nameProperty)
        itemObjectProperties.put("price", priceProperty)
        itemObjectSchema.put("properties", itemObjectProperties)
        itemObjectSchema.put("required", JSONArray().put("name").put("price"))
        itemObjectSchema.put("additionalProperties", false)
        itemsArrayProperty.put("items", itemObjectSchema)
        analysisProperties.put("items", itemsArrayProperty)
        
        // category
        val categoryProperty = JSONObject()
        categoryProperty.put("type", "string")
        categoryProperty.put("enum", JSONArray().put("groceries").put("health").put("entertainment").put("restaurant"))
        analysisProperties.put("category", categoryProperty)
        
        // final_price
        val finalPriceProperty = JSONObject()
        finalPriceProperty.put("type", "integer")
        analysisProperties.put("final_price", finalPriceProperty)
        
        analysisProperty.put("properties", analysisProperties)
        analysisProperty.put("required", JSONArray().put("items").put("category").put("final_price"))
        analysisProperty.put("additionalProperties", false)
        itemProperties.put("analysis", analysisProperty)
        
        itemsSchema.put("properties", itemProperties)
        itemsSchema.put("required", JSONArray().put("image_index").put("image_uri").put("is_receipt").put("analysis"))
        itemsSchema.put("additionalProperties", false)
        
        resultsProperty.put("items", itemsSchema)
        properties.put("results", resultsProperty)
        
        schema.put("properties", properties)
        schema.put("required", JSONArray().put("results"))
        schema.put("additionalProperties", false)
        
        jsonSchema.put("schema", schema)
        responseFormat.put("json_schema", jsonSchema)
        
        requestBody.put("response_format", responseFormat)
        
        return requestBody.toString()
    }
    
    private fun parseSuccessResponse(responseBody: String, originalImageUris: List<String>): AnalysisResult {
        return try {
            val response = JSONObject(responseBody)
            val choices = response.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val contentString = message.getString("content")
            
            // Parse the structured JSON response
            val structuredResponse = JSONObject(contentString)
            val resultsArray = structuredResponse.getJSONArray("results")
            
            val imageAnalyses = mutableListOf<ImageAnalysis>()
            
            for (i in 0 until resultsArray.length()) {
                val result = resultsArray.getJSONObject(i)
                val imageIndex = result.getInt("image_index")
                val imageUri = result.getString("image_uri")
                val isReceipt = result.getBoolean("is_receipt")
                
                val analysis = if (isReceipt && result.has("analysis") && !result.isNull("analysis")) {
                    val analysisObj = result.getJSONObject("analysis")
                    val itemsArray = analysisObj.getJSONArray("items")
                    val category = analysisObj.getString("category")
                    val finalPrice = analysisObj.getInt("final_price")
                    
                    val items = mutableListOf<ReceiptItem>()
                    for (j in 0 until itemsArray.length()) {
                        val item = itemsArray.getJSONObject(j)
                        items.add(ReceiptItem(
                            name = item.getString("name"),
                            price = item.getInt("price")
                        ))
                    }
                    
                    ReceiptAnalysis(
                        items = items,
                        category = category,
                        finalPrice = finalPrice
                    )
                } else {
                    null
                }
                
                val imageAnalysis = ImageAnalysis(
                    imageIndex = imageIndex,
                    imageUri = imageUri,
                    isReceipt = isReceipt,
                    analysis = analysis
                )
                
                // Log analysis for each image with its URI
                val expectedId = if (imageIndex < originalImageUris.size) {
                    val originalUri = originalImageUris[imageIndex]
                    "IMG_${imageIndex}_${originalUri.hashCode().toString().takeLast(8)}"
                } else {
                    "Unknown"
                }
                
                Log.d(TAG, "Analysis for Image $imageIndex:")
                Log.d(TAG, "  Expected ID: $expectedId")
                Log.d(TAG, "  Returned ID: $imageUri")
                Log.d(TAG, "  ID Match: ${expectedId == imageUri}")
                Log.d(TAG, "  Full URI: ${if (imageIndex < originalImageUris.size) originalImageUris[imageIndex] else "Index out of bounds"}")
                Log.d(TAG, "  Is Receipt: $isReceipt")
                if (analysis != null) {
                    Log.d(TAG, "  Category: ${analysis.category}")
                    Log.d(TAG, "  Final Price: $${analysis.finalPrice / 100.0}")
                    Log.d(TAG, "  Items (${analysis.items.size}):")
                    analysis.items.forEachIndexed { itemIndex, item ->
                        Log.d(TAG, "    $itemIndex: ${item.name} - $${item.price / 100.0}")
                    }
                } else {
                    Log.d(TAG, "  No analysis (not a receipt)")
                }
                
                imageAnalyses.add(imageAnalysis)
            }
            
            AnalysisResult(
                success = true,
                results = imageAnalyses
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response", e)
            AnalysisResult(
                success = false,
                results = emptyList(),
                error = "Failed to parse response: ${e.message}"
            )
        }
    }
}
