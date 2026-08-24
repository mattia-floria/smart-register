package com.afloria.smartregister.data.remote

import com.afloria.smartregister.data.remote.model.*
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
data class LessonsResponse(
    val lessons: List<LessonRemoteModel> = emptyList()
)

@Serializable
data class LessonRemoteModel(
    val authorName: String? = null,
    val lessonArg: String? = null,
    val subjectDesc: String? = null,
    val evtDate: String? = null
)

@Serializable
data class NoticeboardResponse(
    val items: List<NoticeRemoteModel>
)

@Serializable
data class AbsencesResponse(
    val events: List<AbsenceRemoteModel>
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

    @GET("students/{studentId}/lessons/{begin}/{end}")
    suspend fun getLessons(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("begin") begin: String,
        @Path("end") end: String
    ): LessonsResponse

    @GET("students/{studentId}/noticeboard")
    suspend fun getNoticeboard(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): NoticeboardResponse

    @GET("students/{studentId}/noticeboard/attach/{evtCode}/{pubId}/{attachNum}")
    @Streaming
    suspend fun getNoticeboardAttachment(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("evtCode") evtCode: String,
        @Path("pubId") pubId: Int,
        @Path("attachNum") attachNum: Int
    ): Response<ResponseBody>

    @GET("students/{studentId}/didactics")
    suspend fun getDidactics(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): DidacticsResponse

    @GET("students/{studentId}/didactics/item/{contentId}")
    @Streaming
    suspend fun getDidacticItem(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String,
        @Path("contentId") contentId: Int
    ): Response<ResponseBody>

    @GET("students/{studentId}/absences/details")
    suspend fun getAbsences(
        @Header("Z-Auth-Token") token: String,
        @Path("studentId") studentId: String
    ): AbsencesResponse

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

    @GET
    @Streaming
    suspend fun downloadFile(
        @Header("Z-Auth-Token") token: String,
        @Url url: String
    ): Response<ResponseBody>
}
