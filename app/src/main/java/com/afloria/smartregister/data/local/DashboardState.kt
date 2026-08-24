package com.afloria.smartregister.data.local

import kotlinx.serialization.Serializable

@Serializable
enum class DashboardSpanSize {
    SMALL, // 1x1
    WIDE,  // 2x1
    LARGE  // 2x2
}

@Serializable
enum class WidgetColorType {
    PRIMARY, SECONDARY, TERTIARY, SURFACE
}

@Serializable
data class DashboardWidgetState(
    val id: String,
    val type: WidgetType,
    val position: Int,
    val spanSize: DashboardSpanSize = DashboardSpanSize.WIDE,
    val colorType: WidgetColorType = WidgetColorType.SURFACE,
    val isVisible: Boolean = true
)

@Serializable
data class ModernDashboardConfig(
    val widgets: List<DashboardWidgetState>
)
