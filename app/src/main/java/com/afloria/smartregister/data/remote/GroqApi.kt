package com.afloria.smartregister.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqChatResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)

interface GroqApi {
    @POST("chat/completions")
    suspend fun chat(
        @Body request: GroqChatRequest
    ): GroqChatResponse
}
