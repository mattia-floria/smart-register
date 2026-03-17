package com.afloria.myapplication.data.remote

import com.afloria.myapplication.data.remote.model.*
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

@Serializable
data class GradesListResponse(
    val grades: List<GradeRemoteModel>
)

@Serializable
data class AgendaResponse(
    val agenda: List<AgendaEventRemoteModel>
)

@Serializable
data class NoticeboardResponse(
    val items: List<NoticeRemoteModel>
)

interface SpaggiariApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("students/{studentId}/notes/all/")
    suspend fun getNotes(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): NotesResponse

    @GET("students/{studentId}/grades")
    suspend fun getGrades(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): GradesListResponse

    @GET("students/{studentId}/agenda/all/{begin}/{end}")
    suspend fun getAgenda(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("begin") begin: String,
        @Path("end") end: String
    ): AgendaResponse

    @GET("students/{studentId}/noticeboard")
    suspend fun getNoticeboard(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): NoticeboardResponse

    @GET("students/{studentId}/didactics")
    suspend fun getDidactics(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): DidacticsResponse

    @POST("students/{studentId}/notes/{type}/read/{layout_note}")
    suspend fun markNote(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("type") type: String,
        @Path("layout_note") note: Int,
        @Body body: String
    ): NotesReadResponse

    @POST("students/{studentId}/documents")
    suspend fun getDocuments(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): DocumentsResponse

    @POST("students/{studentId}/documents/check/{documentHash}")
    @Streaming
    suspend fun checkDocumentAvailability(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("documentHash") documentHash: String
    ): ResponseBody

    @POST("students/{studentId}/documents/read/{documentHash}")
    @Streaming
    suspend fun readDocument(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("documentHash") documentHash: String
    ): Response<ResponseBody>
}
