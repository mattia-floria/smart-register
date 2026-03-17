package com.afloria.myapplication.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotesResponse(
    @SerialName("NTTE") val notesNTTE: List<NoteRemoteModel>? = null,
    @SerialName("NTCL") val notesNTCL: List<NoteRemoteModel>? = null,
    @SerialName("NTWN") val notesNTWN: List<NoteRemoteModel>? = null,
    @SerialName("NTST") val notesNTST: List<NoteRemoteModel>? = null
)

@Serializable
data class NoteRemoteModel(
    val authorName: String? = null,
    val evtDate: String? = null,
    val evtId: Int? = null,
    val readStatus: Boolean? = null,
    val extText: String? = null,
    val notes: String? = null, // Campo alternativo
    val evtText: String? = null, // Altro campo alternativo
    val warningType: String? = null
) {
    // Funzione helper per prendere il primo testo disponibile
    fun getDisplayNote(): String {
        return extText ?: notes ?: evtText ?: "Nessun dettaglio disponibile"
    }
}

@Serializable
data class NotesReadResponse(
    val event: NoteEvent? = null
)

@Serializable
data class NoteEvent(
    val evtCode: String? = null,
    val evtId: Int? = null,
    val evtText: String? = null
)

@Serializable
data class DocumentsResponse(
    val documents: List<DocumentRemoteModel>? = null,
    val schoolReports: List<SchoolReportRemoteModel>? = null
)

@Serializable
data class DocumentRemoteModel(
    val hash: String? = null,
    val desc: String? = null
)

@Serializable
data class SchoolReportRemoteModel(
    val desc: String? = null,
    val confirmLink: String? = null,
    val viewLink: String? = null
)
