package com.afloria.smartregister.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class TimetableEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dayOfWeek: Int, // 1 = Monday, ..., 5 = Friday (or 7 for Sunday)
    val period: Int,    // 1st hour, 2nd hour, etc.
    val subjectName: String,
    val room: String? = null
)

@Serializable
data class TimetableData(
    val entries: List<TimetableEntry> = emptyList()
)
