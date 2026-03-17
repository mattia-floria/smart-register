package com.afloria.myapplication.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DidacticsResponse(
    @SerialName("didacticts") val teachers: List<TeacherRemoteModel>? = null
)

@Serializable
data class TeacherRemoteModel(
    val teacherId: String? = null,
    val teacherName: String? = null,
    val folders: List<FolderRemoteModel> = emptyList()
)

@Serializable
data class FolderRemoteModel(
    val folderId: Int? = null,
    val folderName: String? = null,
    val contents: List<ContentRemoteModel> = emptyList()
)

@Serializable
data class ContentRemoteModel(
    val contentId: Int? = null,
    val contentName: String? = null,
    val contentType: String? = null
)
