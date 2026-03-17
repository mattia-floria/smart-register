package com.afloria.myapplication.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GradeRemoteModel(
    val subjectId: Int? = null,
    val subjectCode: String? = null,
    val subjectDesc: String? = null,
    val evtId: Int? = null,
    val evtCode: String? = null,
    val evtDate: String? = null,
    val decimalValue: Double? = null,
    val displayValue: String? = null,
    val notesForFamily: String? = null,
    val color: String? = null,
    val canceled: Boolean? = null,
    val underlined: Boolean? = null,
    val periodDesc: String? = null,
    val componentDesc: String? = null
)

@Serializable
data class AgendaEventRemoteModel(
    val evtId: Int? = null,
    val evtCode: String? = null,
    val evtDatetimeBegin: String? = null,
    val evtDatetimeEnd: String? = null,
    val isFullDay: Boolean? = null,
    val notes: String? = null,
    val authorName: String? = null,
    val classDesc: String? = null,
    val subjectDesc: String? = null
)

@Serializable
data class NoticeRemoteModel(
    val pubId: Int? = null,
    val pubDT: String? = null,
    val readStatus: Boolean? = null,
    val cntTitle: String? = null,
    val cntCategory: String? = null,
    val cntHasAttach: Boolean? = null
)

@Serializable
data class DidacticFolderRemoteModel(
    val folderId: Int? = null,
    val folderName: String? = null,
    val teacherName: String? = null
)
