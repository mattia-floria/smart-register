package com.afloria.smartregister.ai.runtime

import com.afloria.smartregister.data.remote.model.GradeRemoteModel
import com.afloria.smartregister.data.remote.model.AbsenceRemoteModel
import com.afloria.smartregister.data.remote.model.AgendaEventRemoteModel
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import java.util.Locale

class SmartyToolSet(
    private val grades: List<GradeRemoteModel> = emptyList(),
    private val absences: List<AbsenceRemoteModel> = emptyList(),
    private val agenda: List<AgendaEventRemoteModel> = emptyList()
) : ToolSet {
    
    @Tool(description = "Ottiene la media dei voti per una specifica materia o totale")
    fun getGradesAverage(
        @ToolParam(description = "Il nome della materia (es. Matematica, Italiano). Opzionale, se vuoto calcola la media totale.") subject: String? = null
    ): String {
        val filteredGrades = if (subject.isNullOrBlank()) {
            grades
        } else {
            grades.filter { (it.subjectDesc ?: "").contains(subject, ignoreCase = true) }
        }

        if (filteredGrades.isEmpty()) return "Non ho trovato voti per questa richiesta."

        val sum = filteredGrades.mapNotNull { it.decimalValue }.sum()
        val count = filteredGrades.count { it.decimalValue != null }
        
        if (count == 0) return "Non ci sono voti numerici validi."
        
        val average = sum / count
        return "La media per ${subject ?: "tutte le materie"} è ${String.format(Locale.getDefault(), "%.2f", average)}"
    }

    @Tool(description = "Ottiene il numero totale di assenze effettuate")
    fun getTotalAbsences(): Int {
        return absences.size
    }

    @Tool(description = "Verifica se ci sono compiti o eventi per una data specifica")
    fun getAgendaEvents(
        @ToolParam(description = "La data per cui cercare gli eventi (formato YYYY-MM-DD)") date: String
    ): String {
        val events = agenda.filter { it.evtDatetimeBegin?.startsWith(date) == true }
        if (events.isEmpty()) return "Non ci sono eventi o compiti per il $date."
        
        return events.joinToString("\n") { "- ${it.authorName}: ${it.notes}" }
    }
}
