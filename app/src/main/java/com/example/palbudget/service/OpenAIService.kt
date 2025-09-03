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

data class AnalysisResult(
    val success: Boolean,
    val message: String,
    val error: String? = null
)

class OpenAIService(private val context: Context) {
    
    companion object {
        private const val TAG = "OpenAIService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        // TODO: Set the actual OpenAI API key
        private const val API_KEY = ""
    }
    
    suspend fun analyzeReceipts(imageUris: List<String>): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            if (API_KEY.isEmpty()) {
                return@withContext AnalysisResult(
                    success = false,
                    message = "",
                    error = "OpenAI API key not configured"
                )
            }
            
            val prompt = loadPromptFromAssets()
            val requestBody = buildRequestBody(prompt, imageUris)
            
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
                parseSuccessResponse(responseBody)
            } else {
                AnalysisResult(
                    success = false,
                    message = "",
                    error = "API Error ($responseCode): $responseBody"
                )
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            AnalysisResult(
                success = false,
                message = "",
                error = "Network error: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            AnalysisResult(
                success = false,
                message = "",
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
        val messageProperty = JSONObject()
        messageProperty.put("type", "string")
        properties.put("message", messageProperty)
        
        schema.put("properties", properties)
        schema.put("required", JSONArray().put("message"))
        schema.put("additionalProperties", false)
        
        jsonSchema.put("schema", schema)
        responseFormat.put("json_schema", jsonSchema)
        
        requestBody.put("response_format", responseFormat)
        
        return requestBody.toString()
    }
    
    private fun parseSuccessResponse(responseBody: String): AnalysisResult {
        return try {
            val response = JSONObject(responseBody)
            val choices = response.getJSONArray("choices")
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val contentString = message.getString("content")
            
            // Parse the structured JSON response
            val structuredResponse = JSONObject(contentString)
            val analysisMessage = structuredResponse.getString("message")
            
            AnalysisResult(
                success = true,
                message = analysisMessage
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response", e)
            AnalysisResult(
                success = false,
                message = "",
                error = "Failed to parse response: ${e.message}"
            )
        }
    }
}
