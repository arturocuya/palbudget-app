package com.example.palbudget.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String = "gpt-4o",
    val temperature: Int = 0,
    val messages: List<Message>,
    @SerialName("response_format")
    val responseFormat: ResponseFormat
)

@Serializable
data class Message(
    val role: String,
    val content: List<Content>
)

@Serializable
data class Content(
    val type: String,
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class ResponseFormat(
    val type: String = "json_schema",
    @SerialName("json_schema")
    val jsonSchema: JsonSchemaWrapper
)

@Serializable
data class JsonSchemaWrapper(
    val name: String = "ReceiptAnalysisResponse", 
    val strict: Boolean = true,
    val schema: kotlinx.serialization.json.JsonElement
)

// Response models
@Serializable
data class OpenAIResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ResponseMessage
)

@Serializable
data class ResponseMessage(
    val content: String
)

@Serializable
data class StructuredResponse(
    val results: List<ImageAnalysisResponse>
)

@Serializable
data class ImageAnalysisResponse(
    @SerialName("image_index")
    val imageIndex: Int,
    @SerialName("image_uri")
    val imageUri: String,
    @SerialName("is_receipt")
    val isReceipt: Boolean,
    val analysis: ReceiptAnalysisResponse?
)

@Serializable
data class ReceiptAnalysisResponse(
    val items: List<ReceiptItemResponse>,
    val category: String,
    @SerialName("final_price")
    val finalPrice: Int,
    val date: String?
)

@Serializable
data class ReceiptItemResponse(
    val name: String,
    val price: Int
)
