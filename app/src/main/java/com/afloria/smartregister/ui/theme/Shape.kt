package com.afloria.smartregister.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// Material 3 Expressive - Custom Shapes
object ExpressiveShapes {
    val Squircle = RoundedCornerShape(28.dp)
    val ExtraLargeSquircle = RoundedCornerShape(40.dp)
    val Pill = RoundedCornerShape(50)
    
    // Asymmetric shapes for expressive layout
    val AsymmetricTop = RoundedCornerShape(
        topStart = 40.dp,
        topEnd = 12.dp,
        bottomEnd = 12.dp,
        bottomStart = 12.dp
    )
    
    val AsymmetricBottom = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomEnd = 40.dp,
        bottomStart = 12.dp
    )
    
    val CardExpressive = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomStart = 8.dp,
        bottomEnd = 24.dp
    )
}
