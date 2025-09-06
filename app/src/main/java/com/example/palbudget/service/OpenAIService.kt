package com.example.palbudget.service

import android.content.Context
import android.util.Log
import com.example.palbudget.data.Content
import com.example.palbudget.data.ReceiptItem
import com.example.palbudget.data.ReceiptAnalysis
import com.example.palbudget.data.ImageUrl
import com.example.palbudget.data.JsonSchemaWrapper
import com.example.palbudget.data.Message
import com.example.palbudget.data.OpenAIRequest
import com.example.palbudget.data.OpenAIResponse
import com.example.palbudget.data.ResponseFormat
import com.example.palbudget.data.StructuredResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL



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
    
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
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
            val requestBodyResult = buildRequestBody(fullPrompt, imageBase64List)
            
            if (requestBodyResult.isFailure) {
                return@withContext AnalysisResult(
                    success = false,
                    results = emptyList(),
                    error = "Request serialization error: ${requestBodyResult.exceptionOrNull()?.message}"
                )
            }
            
            val requestBody = requestBodyResult.getOrThrow()
            
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
    
    private fun buildRequestBody(prompt: String, imageUris: List<String>): Result<String> {
        return try {
            val content = buildList {
                // Add text prompt
                add(Content(type = "text", text = prompt))
                
                // Add images
                imageUris.forEach { imageUri ->
                    add(Content(type = "image_url", imageUrl = ImageUrl(url = imageUri)))
                }
            }
            
            val messages = listOf(
                Message(role = "user", content = content)
            )
            
            val responseFormat = createResponseFormat()
            val request = OpenAIRequest(messages = messages, responseFormat = responseFormat)
            
            Result.success(json.encodeToString(request))
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to serialize request", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error building request", e)
            Result.failure(e)
        }
    }
    
    private fun createResponseFormat(): ResponseFormat {
        val schemaString = loadSchemaFromAssets()
        val schemaElement = json.parseToJsonElement(schemaString)
        
        return ResponseFormat(
            jsonSchema = JsonSchemaWrapper(schema = schemaElement)
        )
    }
    
    private fun loadSchemaFromAssets(): String {
        return try {
            context.assets.open("openai_schema.json").bufferedReader().readText()
        } catch (e: Exception) {
            Log.e(TAG, "Could not load schema from assets", e)
            throw e
        }
    }
    
    private fun parseSuccessResponse(responseBody: String, originalImageUris: List<String>): AnalysisResult {
        return try {
            // Parse OpenAI response
            val response = json.decodeFromString<OpenAIResponse>(responseBody)
            val contentString = response.choices.firstOrNull()?.message?.content
                ?: return AnalysisResult(
                    success = false,
                    results = emptyList(),
                    error = "No content in response"
                )
            
            // Parse structured response content
            val structuredResponse = json.decodeFromString<StructuredResponse>(contentString)
            
            val imageAnalyses = structuredResponse.results.map { result ->
                val analysis = result.analysis?.let { analysisResponse ->
                    ReceiptAnalysis(
                        items = analysisResponse.items.map { item ->
                            ReceiptItem(name = item.name, price = item.price)
                        },
                        category = analysisResponse.category,
                        finalPrice = analysisResponse.finalPrice,
                        date = analysisResponse.date
                    )
                }
                
                val imageAnalysis = ImageAnalysis(
                    imageIndex = result.imageIndex,
                    imageUri = result.imageUri,
                    isReceipt = result.isReceipt,
                    analysis = analysis
                )
                
                // Log analysis for each image with its URI
                val expectedId = if (result.imageIndex < originalImageUris.size) {
                    val originalUri = originalImageUris[result.imageIndex]
                    "IMG_${result.imageIndex}_${originalUri.hashCode().toString().takeLast(8)}"
                } else {
                    "Unknown"
                }
                
                Log.d(TAG, "Analysis for Image ${result.imageIndex}:")
                Log.d(TAG, "  Expected ID: $expectedId")
                Log.d(TAG, "  Returned ID: ${result.imageUri}")
                Log.d(TAG, "  ID Match: ${expectedId == result.imageUri}")
                Log.d(TAG, "  Full URI: ${if (result.imageIndex < originalImageUris.size) originalImageUris[result.imageIndex] else "Index out of bounds"}")
                Log.d(TAG, "  Is Receipt: ${result.isReceipt}")
                if (analysis != null) {
                    Log.d(TAG, "  Category: ${analysis.category}")
                    Log.d(TAG, "  Date: ${analysis.date ?: "Not available"}")
                    Log.d(TAG, "  Final Price: $${analysis.finalPrice / 100.0}")
                    Log.d(TAG, "  Items (${analysis.items.size}):")
                    analysis.items.forEachIndexed { itemIndex, item ->
                        Log.d(TAG, "    $itemIndex: ${item.name} - $${item.price / 100.0}")
                    }
                } else {
                    Log.d(TAG, "  No analysis (not a receipt)")
                }
                
                imageAnalysis
            }
            
            AnalysisResult(
                success = true,
                results = imageAnalyses
            )
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to deserialize response", e)
            AnalysisResult(
                success = false,
                results = emptyList(),
                error = "Response deserialization error: ${e.message}"
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
