package com.afloria.myapplication.data.repository

import com.afloria.myapplication.data.remote.SpaggiariApi
import com.afloria.myapplication.data.remote.model.*

class SpaggiariRepository(private val api: SpaggiariApi) {

    suspend fun login(ident: String, pass: String): LoginResponse {
        return api.login(LoginRequest(ident = ident, pass = pass, uid = ident))
    }

    suspend fun getNotes(token: String, ident: String): NotesResponse {
        val studentId = extractStudentId(ident)
        return api.getNotes(token, studentId)
    }

    suspend fun getGrades(token: String, ident: String): List<GradeRemoteModel> {
        val studentId = extractStudentId(ident)
        return api.getGrades(token, studentId).grades
    }

    suspend fun getAgenda(token: String, ident: String, begin: String, end: String): List<AgendaEventRemoteModel> {
        val studentId = extractStudentId(ident)
        return api.getAgenda(token, studentId, begin, end).agenda
    }

    suspend fun getNoticeboard(token: String, ident: String): List<NoticeRemoteModel> {
        val studentId = extractStudentId(ident)
        return api.getNoticeboard(token, studentId).items
    }

    private fun extractStudentId(ident: String): String {
        return ident.filter { it.isDigit() }
    }
}
