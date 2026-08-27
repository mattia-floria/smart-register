package com.afloria.smartregister.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class WidgetColorType {
    PRIMARY, SECONDARY, TERTIARY, SURFACE
}

@Serializable
data class DashboardWidgetState(
    val id: String,
    val type: WidgetType,
    val position: Int,
    val width: Int = 1, // grid columns (1 or 2)
    val height: Int = 1, // grid rows
    val colorType: WidgetColorType = WidgetColorType.SURFACE,
    val isVisible: Boolean = true
)

@Serializable
data class ModernDashboardConfig(
    val widgets: List<DashboardWidgetState>
)
