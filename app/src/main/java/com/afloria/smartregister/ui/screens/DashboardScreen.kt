package com.afloria.smartregister.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afloria.smartregister.data.local.*
import com.afloria.smartregister.ui.MainViewModel
import com.afloria.smartregister.ui.components.ExpressiveCard
import com.afloria.smartregister.ui.theme.ExpressiveShapes
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val BASE_ROW_HEIGHT = 160.dp
private const val GRID_COLUMNS = 2

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    var isEditMode by remember { mutableStateOf(false) }
    val dashboardConfig by viewModel.modernDashboardConfig.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showAddSheet by remember { mutableStateOf(false) }

    val wobbleAnim = rememberInfiniteTransition(label = "wobble")
    val rotation by wobbleAnim.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { isEditMode = false },
                    icon = { Icon(Icons.Rounded.Check, null) },
                    text = { Text("Fatto") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = ExpressiveShapes.Pill,
                    modifier = Modifier.padding(bottom = 80.dp)
                )
            }
        }
    ) { padding ->
        val visibleWidgets = remember(dashboardConfig) { 
            dashboardConfig.widgets.filter { it.isVisible }.sortedBy { it.position } 
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { clip = false },
            contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                DashboardHeader(
                    name = viewModel.studentName,
                    isEditMode = isEditMode,
                    onEdit = { 
                        isEditMode = true 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onAdd = { showAddSheet = true }
                )
            }

            items(
                items = visibleWidgets,
                key = { it.id },
                span = { widget ->
                    if (widget.width >= GRID_COLUMNS) StaggeredGridItemSpan.FullLine 
                    else StaggeredGridItemSpan.SingleLane
                }
            ) { widget ->
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }
                var morphWidthPx by remember { mutableFloatStateOf(0f) }
                var morphHeightPx by remember { mutableFloatStateOf(0f) }

                val density = LocalDensity.current
                val morphHeightDp = with(density) { morphHeightPx.toDp() }
                
                val dragIntensity by remember {
                    derivedStateOf {
                        val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
                        (distance / 50f).coerceIn(0f, 1f)
                    }
                }

                Box(
                    modifier = Modifier
                        .animateItem(
                            placementSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 380f,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )
                        )
                        .height(BASE_ROW_HEIGHT * widget.height + (16.dp * (widget.height - 1)) + morphHeightDp)
                        .then(if (isEditMode) {
                            Modifier
                                .graphicsLayer {
                                    translationX = offsetX
                                    translationY = offsetY
                                    
                                    val isDragging = offsetX != 0f || offsetY != 0f
                                    val targetScale = if (isDragging) 1.06f else 1f
                                    scaleX = targetScale
                                    scaleY = targetScale
                                    
                                    alpha = if (isDragging) 0.9f else 1f
                                    shadowElevation = if (isDragging) 32.dp.toPx() else 0f
                                    
                                    if (isDragging) {
                                        val blurRadius = (dragIntensity * 12f).coerceAtLeast(0.01f)
                                        renderEffect = android.graphics.RenderEffect
                                            .createBlurEffect(blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP)
                                            .asComposeRenderEffect()
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    } else {
                                        renderEffect = null
                                        compositingStrategy = CompositingStrategy.Auto
                                    }
                                }
                                .pointerInput(visibleWidgets) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                            
                                            val currentIndex = visibleWidgets.indexOfFirst { it.id == widget.id }
                                            val gridStepX = size.width.toFloat() / 2f
                                            val gridStepY = size.height.toFloat()
                                            
                                            val targetIndex = when {
                                                offsetY < -gridStepY / 2 -> (currentIndex - 2).coerceAtLeast(0)
                                                offsetY > gridStepY / 2 -> (currentIndex + 2).coerceAtMost(visibleWidgets.size - 1)
                                                offsetX < -gridStepX / 2 -> (currentIndex - 1).coerceAtLeast(0)
                                                offsetX > gridStepX / 2 -> (currentIndex + 1).coerceAtMost(visibleWidgets.size - 1)
                                                else -> currentIndex
                                            }
                                            
                                            if (targetIndex != currentIndex) {
                                                viewModel.moveWidget(widget.id, visibleWidgets[targetIndex].id)
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        },
                                        onDragEnd = { offsetX = 0f; offsetY = 0f },
                                        onDragCancel = { offsetX = 0f; offsetY = 0f }
                                    )
                                }
                                .rotate(rotation)
                        } else Modifier)
                ) {
                    DashboardWidgetContainer(
                        widget = widget,
                        viewModel = viewModel,
                        isEditMode = isEditMode,
                        morphWidthPx = morphWidthPx,
                        onMorphWidth = { morphWidthPx += it },
                        onMorphHeight = { morphHeightPx += it },
                        onSnap = { finalWidthPx ->
                            val rowHeightPx = with(density) { BASE_ROW_HEIGHT.toPx() }
                            val deltaRows = (morphHeightPx / rowHeightPx).roundToInt()
                            val newHeight = (widget.height + deltaRows).coerceAtLeast(1)
                            
                            val newWidth = if (morphWidthPx > finalWidthPx / 2f) 2 
                                           else if (morphWidthPx < -finalWidthPx / 2f) 1 
                                           else widget.width
                            
                            viewModel.updateWidgetSize(widget.id, newWidth.coerceIn(1, 2), newHeight)
                            morphWidthPx = 0f
                            morphHeightPx = 0f
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddWidgetSheet(
            currentWidgets = dashboardConfig.widgets,
            onAdd = { type ->
                val id = type.name.lowercase() + "_" + System.currentTimeMillis()
                val newWidget = DashboardWidgetState(id, type, dashboardConfig.widgets.size)
                viewModel.updateModernDashboardConfig(dashboardConfig.copy(widgets = dashboardConfig.widgets + newWidget))
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
fun DashboardHeader(name: String, isEditMode: Boolean, onEdit: () -> Unit, onAdd: () -> Unit) {
    Column(modifier = Modifier.statusBarsPadding().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bentornato,",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditMode) {
                    FilledTonalIconButton(
                        onClick = onAdd, 
                        shape = ExpressiveShapes.Squircle,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Rounded.Add, "Aggiungi")
                    }
                } else {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, ExpressiveShapes.Squircle)
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardWidgetContainer(
    widget: DashboardWidgetState,
    viewModel: MainViewModel,
    isEditMode: Boolean,
    morphWidthPx: Float,
    onMorphWidth: (Float) -> Unit,
    onMorphHeight: (Float) -> Unit,
    onSnap: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val bounceAnim = remember { Animatable(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectTapGestures(
                        onTap = {
                            viewModel.cycleWidgetColor(widget.id)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            }
    ) {
        val bounceScale by animateFloatAsState(
            targetValue = bounceAnim.value,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
            label = "bounce"
        )

        val morphWidthDp = with(density) { morphWidthPx.toDp() }

        ExpressiveCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = (-morphWidthDp).coerceAtMost(0.dp))
                .graphicsLayer {
                    scaleX = bounceScale
                    scaleY = bounceScale
                }
                .then(if (isEditMode) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), ExpressiveShapes.Squircle) else Modifier),
            shape = when {
                widget.width == 1 && widget.height == 1 -> ExpressiveShapes.Squircle
                widget.width == 2 && widget.height == 1 -> ExpressiveShapes.AsymmetricTop
                else -> ExpressiveShapes.ExtraLargeSquircle
            },
            containerColor = when(widget.colorType) {
                WidgetColorType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                WidgetColorType.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                WidgetColorType.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                WidgetColorType.SURFACE -> MaterialTheme.colorScheme.surfaceContainer
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (widget.type) {
                    WidgetType.AI_BRIEF -> AiBriefContent(viewModel)
                    WidgetType.RECOVERY_STATUS -> GpaHeroContent(viewModel)
                    WidgetType.COUNTDOWN -> StatWidgetContent("Fine Scuola", "84gg", Icons.Rounded.Celebration)
                    WidgetType.WEEKLY_CHART -> NoticeboardWidget(viewModel)
                    WidgetType.TOMORROW_AGENDA -> AgendaPreview(viewModel)
                    WidgetType.GRADES_SUMMARY -> RegistryPreview(viewModel)
                    WidgetType.ABSENCES_COUNT -> AbsencesWidget(viewModel)
                    WidgetType.NOTES_PREVIEW -> NotesWidget(viewModel)
                }
            }
        }

        if (isEditMode) {
            val borderHitSize = 32.dp
            
            // Right Border (Width Resize)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(borderHitSize)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onMorphWidth(dragAmount.x)
                            },
                            onDragEnd = { onSnap(size.width.toFloat()) },
                            onDragCancel = { onSnap(size.width.toFloat()) }
                        )
                    }
            )

            // Bottom Border (Height Resize)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(borderHitSize)
                    .align(Alignment.BottomCenter)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onMorphHeight(dragAmount.y)
                            },
                            onDragEnd = { onSnap(size.width.toFloat()) }, // Passing width even here for consistency
                            onDragCancel = { onSnap(size.width.toFloat()) }
                        )
                    }
            )
            
            IconButton(
                onClick = { viewModel.updateWidgetState(widget.id) { it.copy(isVisible = false) } },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)
            ) {
                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
    
    LaunchedEffect(widget.colorType) {
        if (isEditMode) {
            bounceAnim.animateTo(0.92f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
            bounceAnim.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        }
    }
}

@Composable
fun GpaHeroContent(viewModel: MainViewModel) {
    val grades = viewModel.grades
    val avg = if (grades.isNotEmpty()) grades.mapNotNull { it.decimalValue }.average() else 0.0
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BarChart, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Media Totale", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "%.2f".format(avg), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(text = "${grades.size} voti caricati", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
fun AiBriefContent(viewModel: MainViewModel) {
    val brief by remember { derivedStateOf { viewModel.aiBriefSummary } }
    val isLoading = viewModel.isAiBriefLoading
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Smarty Brief", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (isLoading && brief == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape), color = MaterialTheme.colorScheme.primary)
        } else {
            Text(text = brief ?: "Pronto ad analizzare i tuoi impegni.", style = MaterialTheme.typography.bodyLarge, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun StatWidgetContent(label: String, value: String, icon: ImageVector) {
    Column {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RegistryPreview(viewModel: MainViewModel) {
    val grades = viewModel.grades
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ultimi Voti", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (grades.isEmpty()) {
            Text("Nessun voto.", style = MaterialTheme.typography.bodyMedium)
        } else {
            grades.take(3).forEach { grade ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(grade.displayValue ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(grade.subjectDesc ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun AbsencesWidget(viewModel: MainViewModel) {
    val absences = viewModel.absences
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EventBusy, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Assenze", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("${absences.size}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Text("eventi registrati", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NotesWidget(viewModel: MainViewModel) {
    val notes = viewModel.notes
    val count = (notes?.notesNTTE?.size ?: 0) + (notes?.notesNTCL?.size ?: 0)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Note", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("$count", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Text("note disciplinari", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NoticeboardWidget(viewModel: MainViewModel) {
    val notices = viewModel.notices
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Assignment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ultime Circolari", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (notices.isEmpty()) {
            Text("Nessun avviso.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            notices.take(2).forEach { notice ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = notice.cntTitle ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = notice.pubDT ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun AgendaPreview(viewModel: MainViewModel) {
    val events = viewModel.getTomorrowEvents()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Domani", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (events.isEmpty()) {
            Text("Nessun compito.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                events.take(3).forEach { event ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = event.subjectDesc ?: "Materia", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWidgetSheet(
    currentWidgets: List<DashboardWidgetState>,
    onAdd: (WidgetType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = ExpressiveShapes.ExtraLargeSquircle,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 64.dp)) {
            Text("Nuovo Widget", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(24.dp))
            
            WidgetType.entries.forEach { type ->
                val alreadyPresent = currentWidgets.any { it.type == type && it.isVisible }
                val label = when(type) {
                    WidgetType.AI_BRIEF -> "Smarty Brief"
                    WidgetType.RECOVERY_STATUS -> "Media Totale"
                    WidgetType.COUNTDOWN -> "Conto alla rovescia"
                    WidgetType.WEEKLY_CHART -> "Circolari"
                    WidgetType.TOMORROW_AGENDA -> "Agenda Domani"
                    WidgetType.GRADES_SUMMARY -> "Ultimi Voti"
                    WidgetType.ABSENCES_COUNT -> "Assenze"
                    WidgetType.NOTES_PREVIEW -> "Note Disciplinari"
                }
                
                Surface(
                    onClick = { if (!alreadyPresent) onAdd(type) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = ExpressiveShapes.Squircle,
                    color = if (alreadyPresent) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
                    enabled = !alreadyPresent
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when(type) {
                                WidgetType.AI_BRIEF -> Icons.Rounded.AutoAwesome
                                WidgetType.RECOVERY_STATUS -> Icons.Rounded.BarChart
                                WidgetType.COUNTDOWN -> Icons.Rounded.Celebration
                                WidgetType.WEEKLY_CHART -> Icons.Rounded.Assignment
                                WidgetType.TOMORROW_AGENDA -> Icons.Rounded.Event
                                WidgetType.GRADES_SUMMARY -> Icons.Rounded.Star
                                WidgetType.ABSENCES_COUNT -> Icons.Rounded.EventBusy
                                WidgetType.NOTES_PREVIEW -> Icons.Rounded.Description
                            },
                            null,
                            tint = if (alreadyPresent) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            color = if (alreadyPresent) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (alreadyPresent) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}
