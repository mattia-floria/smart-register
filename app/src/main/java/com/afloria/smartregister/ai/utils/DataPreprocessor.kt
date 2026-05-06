package com.afloria.smartregister.ai.utils

import com.afloria.smartregister.data.remote.model.AgendaEventRemoteModel
import com.afloria.smartregister.data.remote.model.GradeRemoteModel
import com.afloria.smartregister.data.remote.model.NoteRemoteModel
import com.afloria.smartregister.data.remote.model.NotesResponse
import com.afloria.smartregister.data.remote.model.NoticeRemoteModel
import java.text.SimpleDateFormat
import java.util.*

object DataPreprocessor {

    fun formatAgendaEventsForSummary(events: List<AgendaEventRemoteModel>): String {
        if (events.isEmpty()) return ""
        
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN)

        return events.sortedBy { it.evtDatetimeBegin }.joinToString(separator = "\n") { event ->
            val dateStr = event.evtDatetimeBegin?.let {
                try {
                    val date = inputFormat.parse(it)
                    if (date != null) outputFormat.format(date) else ""
                } catch (e: Exception) { "" }
            } ?: ""
            
            val subject = event.subjectDesc ?: ""
            val notes = event.notes ?: ""
            
            "- $dateStr: $subject ($notes)"
        }
    }

    fun formatGrades(grades: List<GradeRemoteModel>): String {
        if (grades.isEmpty()) return "Nessun voto disponibile."
        return grades.take(10).joinToString("\n") { grade ->
            "- ${grade.evtDate}: ${grade.subjectDesc} -> ${grade.displayValue} (${grade.notesForFamily ?: "Nessuna nota"})"
        }
    }

    fun formatNotes(notesResponse: NotesResponse?): String {
        if (notesResponse == null) return "Nessuna nota disciplinare."
        val allNotes = mutableListOf<NoteRemoteModel>()
        notesResponse.notesNTTE?.let { allNotes.addAll(it) }
        notesResponse.notesNTCL?.let { allNotes.addAll(it) }
        notesResponse.notesNTWN?.let { allNotes.addAll(it) }
        notesResponse.notesNTST?.let { allNotes.addAll(it) }

        if (allNotes.isEmpty()) return "Nessuna nota disciplinare."
        return allNotes.sortedByDescending { it.evtDate }.take(5).joinToString("\n") { note ->
            "- ${note.evtDate} (${note.authorName}): ${note.getDisplayNote()}"
        }
    }

    fun formatNotices(notices: List<NoticeRemoteModel>): String {
        if (notices.isEmpty()) return "Nessuna circolare."
        return notices.take(5).joinToString("\n") { notice ->
            "- ${notice.pubDT}: ${notice.cntTitle}"
        }
    }

    fun buildAiBriefPrompt(events: List<AgendaEventRemoteModel>, studentName: String): String {
        val formattedEvents = formatAgendaEventsForSummary(events)
        val firstName = studentName.split(" ").firstOrNull() ?: studentName
        
        if (formattedEvents.isEmpty()) return "Nessun impegno in agenda per la prossima settimana."

        return "Ciao $firstName, ecco i tuoi impegni per la prossima settimana. Riassumili in un breve paragrafo amichevole:\n$formattedEvents"
    }
}
