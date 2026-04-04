package com.afloria.smartregister.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class PuterChatRequest(
    val query: String,
    val model: String = "gpt-5-nano",
    val stream: Boolean = false
)

@Serializable
data class PuterChatResponse(
    val message: PuterMessage? = null
)

@Serializable
data class PuterMessage(
    val content: List<PuterContent>? = null
)

@Serializable
data class PuterContent(
    val text: String? = null
)

interface PuterApi {
    @POST("v2/ai/chat")
    suspend fun chat(
        @Body request: PuterChatRequest
    ): PuterChatResponse
}
