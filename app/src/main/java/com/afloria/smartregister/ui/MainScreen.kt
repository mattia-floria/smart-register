@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.afloria.smartregister.ui

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afloria.smartregister.ai.models.AiModels
import com.afloria.smartregister.data.remote.model.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

// --- Helper Modifiers for Liquid Effect ---

fun Modifier.liquidFadeEdge(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.White,
                1f to Color.Transparent,
                startY = size.height - 250f,
                endY = size.height
            ),
            blendMode = BlendMode.DstIn
        )
    }

fun Modifier.liquidItem(
    index: Int,
    listState: LazyListState
): Modifier = this.graphicsLayer {
    val layoutInfo = listState.layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (visibleItem != null) {
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val itemBottom = (visibleItem.offset + visibleItem.size).toFloat()
        val threshold = viewportEnd - 400f

        if (itemBottom > threshold) {
            val progress = ((itemBottom - threshold) / 400f).coerceIn(0f, 1f)
            scaleX = 1f - (progress * 0.15f)
            scaleY = 1f - (progress * 0.15f)
            alpha = 1f - progress
        }
    }
}

fun Modifier.liquidGridItem(
    index: Int,
    gridState: LazyGridState
): Modifier = this.graphicsLayer {
    val layoutInfo = gridState.layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (visibleItem != null) {
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val itemBottom = (visibleItem.offset.y + visibleItem.size.height).toFloat()
        val threshold = viewportEnd - 400f

        if (itemBottom > threshold) {
            val progress = ((itemBottom - threshold) / 400f).coerceIn(0f, 1f)
            scaleX = 1f - (progress * 0.15f)
            scaleY = 1f - (progress * 0.15f)
            alpha = 1f - progress
        }
    }
}

// --- Main Screen ---

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                state = pullToRefreshState,
                indicator = {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                val contentModifier = Modifier.fillMaxSize().statusBarsPadding().let {
                    if (selectedTab == 0) it else it.liquidFadeEdge()
                }
                Box(modifier = contentModifier) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { fullWidth -> direction * fullWidth }
                            ) + fadeIn(animationSpec = tween(200)) togetherWith
                                    slideOutHorizontally(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        targetOffsetX = { fullWidth -> -direction * fullWidth }
                                    ) + fadeOut(animationSpec = tween(200))
                        },
                        label = "TabContentTransition"
                    ) { targetTab ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (targetTab) {
                                0 -> DashboardSection(viewModel)
                                1 -> AgendaTabSection(viewModel, viewModel.agenda)
                                2 -> RegistryTabSection(viewModel)
                                3 -> SettingsSection(viewModel, onLogout)
                            }
                        }
                    }
                }
            }

            // AI Chat Overlay
            val isChatOpen = viewModel.isChatOpen
            AnimatedVisibility(
                visible = isChatOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.zIndex(1f)
            ) {
                AiChatOverlay(viewModel)
            }

            // Global Download Loading Overlay with Progress
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Scaricamento file",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            if (downloadProgress > 0f) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Il file verrà aperto automaticamente al termine.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Floating Bottom Navigation Bar
            if (!isChatOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                        .navigationBarsPadding()
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    ) {
                        val itemWidth = this.maxWidth / 4

                        val indicatorStart by animateDpAsState(
                            targetValue = itemWidth * selectedTab,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 300f
                            ),
                            label = "indicatorStart"
                        )

                        val indicatorEnd by animateDpAsState(
                            targetValue = itemWidth * (selectedTab + 1),
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 300f
                            ),
                            label = "indicatorEnd"
                        )

                        // Selection Background (Liquid Morphing)
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorStart)
                                .width(indicatorEnd - indicatorStart)
                                .height(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingNavItem(
                                icon = Icons.Default.Home,
                                label = "Home",
                                isSelected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )
                            FloatingNavItem(
                                icon = Icons.Default.CalendarMonth,
                                label = "Agenda",
                                isSelected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                            FloatingNavItem(
                                icon = Icons.AutoMirrored.Filled.ListAlt,
                                label = "Registro",
                                isSelected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )
                            FloatingNavItem(
                                icon = Icons.Default.Settings,
                                label = "Impostazioni",
                                isSelected = selectedTab == 3,
                                onClick = { selectedTab = 3 }
                            )
                        }
                    }
                }

                // Floating AI Action Button
                if (viewModel.isExperimentalEnabled && viewModel.isChatEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 80.dp, end = 16.dp)
                            .navigationBarsPadding()
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.isChatOpen = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Chat AI",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FloatingNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun WeeklyEventsChart(agenda: List<AgendaEventRemoteModel>) {
    val dayFrequencies = remember(agenda) {
        val counts = IntArray(6)
        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()
        now.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        val startOfWeek = now.timeInMillis
        now.add(Calendar.DAY_OF_YEAR, 6)
        val endOfWeek = now.timeInMillis

        agenda.forEach { event ->
            val dateStr = event.evtDatetimeBegin?.split("T")?.first()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr ?: "")
            date?.let {
                if (it.time in startOfWeek until endOfWeek) {
                    calendar.time = it
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek != Calendar.SUNDAY) {
                        val index = (dayOfWeek + 5) % 7
                        if (index < 6) counts[index]++
                    }
                }
            }
        }
        counts
    }

    val maxCount = dayFrequencies.maxOrNull()?.coerceAtLeast(1) ?: 1
    val days = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Impegni della settimana",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Smooth Line Chart using Canvas
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (dayFrequencies.size - 1)
                    
                    val points = dayFrequencies.indices.map { i ->
                        Offset(
                            x = i * spacing,
                            y = height - (dayFrequencies[i].toFloat() / maxCount * (height * 0.8f)) - (height * 0.1f)
                        )
                    }

                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                                val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                                cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                            }
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    
                    // Draw dots
                    points.forEachIndexed { index, point ->
                        drawCircle(
                            color = if (dayFrequencies[index] > 0) primaryColor else outlineColor,
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSection(viewModel: MainViewModel) {
    val tomorrowEvents = viewModel.getTomorrowEvents()
    val listState = rememberLazyListState()

    val daysToSchoolEnd = remember {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val endOfSchool = Calendar.getInstance().apply {
            // Heuristic for Italian school end (approx June 8th)
            // Ideally should be fetched from an API or regional setting
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 8)
            if (calendar.get(Calendar.MONTH) > Calendar.JUNE) {
                set(Calendar.YEAR, currentYear + 1)
            } else {
                set(Calendar.YEAR, currentYear)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = endOfSchool.timeInMillis - calendar.timeInMillis
        (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    }

    val subjectsToRecover = remember(viewModel.grades) {
        if (viewModel.grades.isEmpty()) return@remember 0

        // Determine current period based on the latest grade's periodDesc
        val lastGrade = viewModel.grades.firstOrNull()
        val lastGradePeriod = lastGrade?.periodDesc?.lowercase() ?: ""
        
        val isSecondPeriod = lastGradePeriod.contains("2") || 
                             lastGradePeriod.contains("secondo") || 
                             lastGradePeriod.contains("pentamestre") || 
                             (lastGradePeriod.contains("quadrimestre") && lastGradePeriod.contains("secondo"))

        val currentPeriodGrades = viewModel.grades.filter {
            val desc = it.periodDesc?.lowercase() ?: ""
            if (isSecondPeriod) {
                desc.contains("2") || desc.contains("secondo") || desc.contains("pentamestre") || (desc.contains("quadrimestre") && desc.contains("secondo"))
            } else {
                desc.contains("1") || desc.contains("primo") || desc.contains("trimestre") || (desc.contains("quadrimestre") && !desc.contains("secondo"))
            }
        }

        currentPeriodGrades
            .groupBy { it.subjectDesc ?: "Altro" }
            .mapValues { entry ->
                val validGrades = entry.value.filter { !isNonContributing(it) }.mapNotNull { it.decimalValue }
                if (validGrades.isEmpty()) 0.0 else validGrades.average()
            }
            .filter { it.value in 0.1..5.99 }
            .size
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Bentornato,",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Light
                        )
                    )
                    Text(
                        text = viewModel.studentName,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // School countdown
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = daysToSchoolEnd.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "giorni alla fine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // AI Status Section
        item {
            val modelDisplayName = viewModel.currentModel?.displayName ?: viewModel.selectedAiModelName
            when {
                viewModel.isModelDownloading -> {
                    AiModelDownloadCard(modelDisplayName, viewModel.modelDownloadProgress)
                }
                viewModel.isLlmInitializing -> {
                    AiInitializationCard(modelDisplayName)
                }
            }
        }

        if (viewModel.isAiBriefEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "AI Brief",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            if (viewModel.isAiBriefLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        val summary = viewModel.aiBriefSummary
                        if (summary != null) {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else if (!viewModel.isAiBriefLoading) {
                            Text(
                                text = "Nessun riassunto disponibile. Trascina per aggiornare.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (subjectsToRecover > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (subjectsToRecover > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = (if (subjectsToRecover > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subjectsToRecover.toString(),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (subjectsToRecover > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Column {
                        Text(
                            text = "Materie da recuperare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (subjectsToRecover == 0) "Puoi stare tranquillo!" else "Dai il massimo per recuperare!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = (if (subjectsToRecover > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            WeeklyEventsChart(viewModel.agenda)
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Agenda di domani",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (tomorrowEvents.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "Per domani non hai nulla da fare",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    tomorrowEvents.forEach { event ->
                        DashboardAgendaItem(event, viewModel)
                    }
                }
            }
        }
    }
}

enum class RegistrySection {
    MENU, GRADES, NOTES, NOTICEBOARD, TEACHER_FILES, ABSENCES, FINAL_GRADES, TIMETABLE, WEB
}

data class RegistryMenuItem(
    val title: String,
    val icon: ImageVector,
    val section: RegistrySection,
    val color: Color
)

@Composable
fun RegistryTabSection(viewModel: MainViewModel) {
    var currentSection by remember { mutableStateOf(RegistrySection.MENU) }
    var selectedReportUrl by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()

    val menuItems = listOf(
        RegistryMenuItem("Voti", Icons.Default.Numbers, RegistrySection.GRADES, Color(0xFF4CAF50)),
        RegistryMenuItem("Note", Icons.AutoMirrored.Filled.Assignment, RegistrySection.NOTES, Color(0xFFFF9800)),
        RegistryMenuItem("Bacheca", Icons.Default.Campaign, RegistrySection.NOTICEBOARD, Color(0xFF2196F3)),
        RegistryMenuItem("Materiale", Icons.Default.Folder, RegistrySection.TEACHER_FILES, Color(0xFF9C27B0)),
        RegistryMenuItem("Assenze", Icons.Default.EventBusy, RegistrySection.ABSENCES, Color(0xFFF44336)),
        RegistryMenuItem("Scrutinio", Icons.Default.School, RegistrySection.FINAL_GRADES, Color(0xFF795548)),
        RegistryMenuItem("Orario", Icons.Default.Schedule, RegistrySection.TIMETABLE, Color(0xFF607D8B)),
        RegistryMenuItem("ClasseViva Web", Icons.Default.Public, RegistrySection.WEB, Color(0xFFE91E63))
    )

    BackHandler(enabled = currentSection != RegistrySection.MENU || selectedReportUrl != null) {
        if (selectedReportUrl != null) {
            selectedReportUrl = null
        } else {
            currentSection = RegistrySection.MENU
        }
    }

    if (selectedReportUrl != null) {
        val state = viewModel.appState.collectAsState().value
        val token = if (state is AppState.LoggedIn) state.response.token else null
        WebViewScreen(url = selectedReportUrl!!, token = token, title = "Scrutinio", onBack = { selectedReportUrl = null })
    } else {
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                if (targetState != RegistrySection.MENU) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "RegistrySectionTransition"
        ) { section ->
            when (section) {
                RegistrySection.MENU -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "Registro Scolastico",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp).liquidGridItem(0, gridState)
                            )
                        }
                        itemsIndexed(menuItems) { index, item ->
                            Box(modifier = Modifier.liquidGridItem(index + 1, gridState)) {
                                RegistryMenuCard(item) { currentSection = item.section }
                            }
                        }
                    }
                }
                RegistrySection.GRADES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Voti") { currentSection = RegistrySection.MENU }
                        GradesTabSection(viewModel.grades)
                    }
                }
                RegistrySection.NOTES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Note") { currentSection = RegistrySection.MENU }
                        NotesTabSection(viewModel.notes)
                    }
                }
                RegistrySection.NOTICEBOARD -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Bacheca") { currentSection = RegistrySection.MENU }
                        NoticeboardTabSection(viewModel, viewModel.notices)
                    }
                }
                RegistrySection.TEACHER_FILES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Materiale Didattico") { currentSection = RegistrySection.MENU }
                        DidacticsTabSection(viewModel, viewModel.teachersMaterials)
                    }
                }
                RegistrySection.ABSENCES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Assenze") { currentSection = RegistrySection.MENU }
                        AbsencesTabSection(viewModel.absences)
                    }
                }
                RegistrySection.FINAL_GRADES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Scrutinio Finale") { currentSection = RegistrySection.MENU }
                        FinalGradesTabSection(reports = viewModel.finalGrades) { report ->
                            selectedReportUrl = report.viewLink
                        }
                    }
                }
                RegistrySection.TIMETABLE -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RegistrySectionHeader("Orario Lezioni") { currentSection = RegistrySection.MENU }
                        TimetableTabSection(viewModel)
                    }
                }
                RegistrySection.WEB -> {
                    ClasseVivaWebSection(viewModel) { currentSection = RegistrySection.MENU }
                }
            }
        }
    }
}

@Composable
fun ClasseVivaWebSection(viewModel: MainViewModel, onBack: () -> Unit) {
    val (ident, pass) = viewModel.getCredentials()
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RegistrySectionHeader(title = "ClasseViva Web", onBack = onBack)
        
        if (ident != null && pass != null) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (url?.contains("login.php") == true) {
                                    val js = """
                                        (function() {
                                            var uid = document.getElementById('uid') || document.querySelector('input[name="uid"]');
                                            var pwd = document.getElementById('pwd') || document.querySelector('input[name="pwd"]');
                                            if (uid && pwd && uid.value === '' && pwd.value === '') {
                                                uid.value = '$ident';
                                                pwd.value = '$pass';
                                                var btn = document.querySelector('button[type="submit"]') || document.querySelector('.btn-login') || document.querySelector('input[type="submit"]');
                                                if (btn) {
                                                    setTimeout(function() { btn.click(); }, 500);
                                                }
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(js, null)
                                }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                        loadUrl("https://web.spaggiari.eu/home/app/default/login.php")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            EmptyState("Credenziali non salvate.")
        }
    }
}


@Composable
fun WebViewScreen(url: String, token: String?, title: String, onBack: () -> Unit) {
    val headers = remember(token) {
        token?.let { mapOf("Z-Auth-Token" to it) } ?: emptyMap()
    }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RegistrySectionHeader(title = title, onBack = onBack)
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    if (token != null) {
                        loadUrl(url, headers)
                    } else {
                        loadUrl(url)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun RegistrySectionHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun RegistryMenuCard(item: RegistryMenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = item.color.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun GradesTabSection(grades: List<GradeRemoteModel>) {
    var selectedGradeTab by remember { mutableIntStateOf(0) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    val tabs = listOf("Tutti", "1° Periodo", "2° Periodo", "Materie")
    val listState = rememberLazyListState()
    var showAveragesOnly by remember { mutableStateOf(false) }

    val filteredGrades = when (selectedGradeTab) {
        1 -> grades.filter {
            val desc = it.periodDesc?.lowercase() ?: ""
            desc.contains("1") || desc.contains("primo") || desc.contains("trimestre") || (desc.contains("quadrimestre") && !desc.contains("secondo"))
        }
        2 -> grades.filter {
            val desc = it.periodDesc?.lowercase() ?: ""
            desc.contains("2") || desc.contains("secondo") || desc.contains("pentamestre") || (desc.contains("quadrimestre") && desc.contains("secondo"))
        }
        else -> grades
    }.sortedByDescending { it.evtDate }

    if (selectedSubject != null) {
        val subjectGrades = filteredGrades.filter { it.subjectDesc == selectedSubject }.sortedBy { it.evtDate }
        SubjectDetailView(
            subjectName = selectedSubject!!,
            grades = subjectGrades,
            onBack = { selectedSubject = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // First Level Tabs (Categories)
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedGradeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                .clickable { selectedGradeTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Second Level Toggle (Voti vs Medie) - Only for 1°/2° Periodo
            if (selectedGradeTab == 1 || selectedGradeTab == 2) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        listOf("Voti", "Medie ${tabs[selectedGradeTab]}").forEachIndexed { index, title ->
                            val isSelected = (index == 0 && !showAveragesOnly) || (index == 1 && showAveragesOnly)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { showAveragesOnly = index == 1 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val filteredGrades = when (selectedGradeTab) {
                1 -> grades.filter {
                    val desc = it.periodDesc?.lowercase() ?: ""
                    desc.contains("1") || desc.contains("primo") || desc.contains("trimestre") || (desc.contains("quadrimestre") && !desc.contains("secondo"))
                }
                2 -> grades.filter {
                    val desc = it.periodDesc?.lowercase() ?: ""
                    desc.contains("2") || desc.contains("secondo") || desc.contains("pentamestre") || (desc.contains("quadrimestre") && desc.contains("secondo"))
                }
                else -> grades
            }.sortedByDescending { it.evtDate }

            if (selectedGradeTab == 3 || showAveragesOnly) {
                SubjectsSummaryList(filteredGrades) { selectedSubject = it }
            } else {
                val currentAverage = calculateAverage(filteredGrades)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().liquidItem(0, listState),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "MEDIA PERIODO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = tabs[selectedGradeTab],
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = currentAverage,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    if (filteredGrades.isEmpty()) {
                        item { EmptyState("Nessun voto presente") }
                    } else {
                        itemsIndexed(filteredGrades) { index, grade ->
                            Box(modifier = Modifier.liquidItem(index + 1, listState)) {
                                GradeItem(grade)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesTabSection(notesResponse: NotesResponse?) {
    val listState = rememberLazyListState()
    val allNotes = remember(notesResponse) {
        val list = mutableListOf<Pair<String, NoteRemoteModel>>()
        notesResponse?.notesNTTE?.forEach { list.add("Nota" to it) }
        notesResponse?.notesNTCL?.forEach { list.add("Classe" to it) }
        notesResponse?.notesNTWN?.forEach { list.add("Richiamo" to it) }
        notesResponse?.notesNTST?.forEach { list.add("Sanzione" to it) }
        list.sortedByDescending { it.second.evtDate }
    }

    if (allNotes.isEmpty()) {
        EmptyState("Nessuna nota presente")
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(allNotes) { index, (type, note) ->
                Card(
                    modifier = Modifier.fillMaxWidth().liquidItem(index, listState),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when(type) {
                            "Richiamo", "Sanzione" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = when(type) {
                                "Richiamo", "Sanzione" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            }) {
                                Text(type, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(note.evtDate ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(note.getDisplayNote(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.authorName ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeboardTabSection(viewModel: MainViewModel, notices: List<NoticeRemoteModel>) {
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredNotices = remember(notices, searchQuery) {
        if (searchQuery.isBlank()) notices
        else notices.filter { 
            (it.cntTitle ?: "").contains(searchQuery, ignoreCase = true) ||
            (it.pubDT ?: "").contains(searchQuery, ignoreCase = true) ||
            (it.cntCategory ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (filteredNotices.isEmpty()) {
            EmptyState(if (searchQuery.isEmpty()) "Nessuna comunicazione" else "Nessun risultato per \"$searchQuery\"")
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(filteredNotices) { index, notice ->
                    Card(
                        modifier = Modifier.fillMaxWidth().liquidItem(index, listState),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = notice.cntTitle ?: "Senza titolo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notice.pubDT?.substringBefore("T") ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (!notice.attachments.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Allegati:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                notice.attachments.forEach { attachment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.downloadNoticeAttachment(notice, attachment) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = attachment.fileName ?: "Allegato",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else if (notice.cntHasAttach == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                                    Text("Allegato disponibile (scarica per visualizzare)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Search Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cerca tra le comunicazioni...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Pulisci")
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }
    }
}

@Composable
fun DidacticsTabSection(viewModel: MainViewModel, teachers: List<TeacherRemoteModel>) {
    var selectedTeacher by remember { mutableStateOf<TeacherRemoteModel?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderRemoteModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler(enabled = selectedTeacher != null || searchQuery.isNotEmpty()) {
        if (searchQuery.isNotEmpty()) searchQuery = ""
        else if (selectedFolder != null) selectedFolder = null
        else selectedTeacher = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val filteredTeachers = remember(teachers, searchQuery) {
            if (searchQuery.isBlank()) teachers
            else teachers.filter { teacher ->
                (teacher.teacherName ?: "").contains(searchQuery, ignoreCase = true) ||
                        teacher.folders.any { folder ->
                            (folder.folderName ?: "").contains(searchQuery, ignoreCase = true) ||
                                    folder.contents.any { content ->
                                        (content.contentName ?: "").contains(searchQuery, ignoreCase = true)
                                    }
                        }
            }
        }

        AnimatedContent(
            targetState = Triple(selectedTeacher, selectedFolder, filteredTeachers),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "DidacticsContentTransition",
            modifier = Modifier.fillMaxSize()
        ) { (teacher, folder, allTeachers) ->
            when {
                folder != null && teacher != null -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedFolder = null }.liquidItem(0, listState)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(folder.folderName ?: "Cartella", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (folder.contents.isEmpty()) {
                            item { EmptyState("Cartella vuota") }
                        } else {
                            itemsIndexed(folder.contents) { index, content ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidItem(index + 1, listState)
                                        .clickable { viewModel.downloadDidacticFile(content) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(content.contentName ?: "Senza nome", fontWeight = FontWeight.Bold)
                                            Text(content.contentType ?: "File", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                teacher != null -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedTeacher = null }.liquidItem(0, listState)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(teacher.teacherName ?: "Docente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (teacher.folders.isEmpty()) {
                            item { EmptyState("Nessuna cartella disponibile") }
                        } else {
                            itemsIndexed(teacher.folders) { index, folder ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().liquidItem(index + 1, listState).clickable { selectedFolder = folder },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(folder.folderName ?: "Senza nome", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (allTeachers.isEmpty()) {
                        EmptyState(if (searchQuery.isEmpty()) "Nessun materiale disponibile" else "Nessun risultato per \"$searchQuery\"")
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(allTeachers) { index, teacher ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().liquidItem(index, listState).clickable { selectedTeacher = teacher },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(teacher.teacherName?.firstOrNull()?.toString() ?: "D", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(teacher.teacherName ?: "Docente", fontWeight = FontWeight.Bold)
                                            Text("${teacher.folders.size} cartelle", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Search Bar (Only shown in the main menu list)
        if (selectedTeacher == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cerca docente o materiale...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Pulisci")
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    }
}

@Composable
fun SubjectsSummaryList(grades: List<GradeRemoteModel>, onSubjectClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    val grouped = grades.groupBy { it.subjectDesc ?: "Altro" }
        .toList()
        .sortedByDescending { calculateAverage(it.second).replace(",", ".").toDoubleOrNull() ?: 0.0 }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(grouped) { index, (subject, subjectGrades) ->
            Box(modifier = Modifier.liquidItem(index, listState)) {
                SubjectCardItem(subject, subjectGrades, onSubjectClick)
            }
        }
    }
}

@Composable
fun SubjectCardItem(subject: String, grades: List<GradeRemoteModel>, onClick: (String) -> Unit) {
    val avg = calculateAverage(grades)
    val avgValue = avg.replace(",", ".").toDoubleOrNull() ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(subject) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (avgValue >= 6.0) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = avg, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(subject, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("${grades.size} voti", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun SubjectDetailView(subjectName: String, grades: List<GradeRemoteModel>, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(subjectName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(modifier = Modifier.liquidItem(0, listState)) {
                    Column {
                        Text("Andamento Media", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        AverageTrendChart(grades)
                    }
                }
            }

            item {
                Text(
                    "Voti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.liquidItem(1, listState)
                )
            }

            itemsIndexed(grades.sortedByDescending { it.evtDate }) { index, grade ->
                Box(modifier = Modifier.liquidItem(index + 2, listState)) {
                    GradeItem(grade, showSubject = false)
                }
            }
        }
    }
}

@Composable
fun AverageTrendChart(grades: List<GradeRemoteModel>) {
    val sortedGrades = remember(grades) { grades.sortedBy { it.evtDate } }
    val points = remember(sortedGrades) {
        val history = mutableListOf<Double>()
        var sum = 0.0
        sortedGrades.forEach { grade ->
            grade.decimalValue?.let {
                sum += it
                history.add(sum / (history.size + 1))
            }
        }
        history
    }

    var selectedPointIdx by remember { mutableStateOf<Int?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary

    if (points.isEmpty()) return

    Column {
        AnimatedVisibility(visible = selectedPointIdx != null) {
            selectedPointIdx?.let { idx ->
                val avg = String.format(Locale.getDefault(), "%.2f", points[idx])
                val date = sortedGrades[idx].evtDate ?: ""
                Text(
                    text = "Media: $avg il $date",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val xStep = if (points.size > 1) width / (points.size - 1) else width
                        val idx = (offset.x / xStep)
                            .toInt()
                            .coerceIn(0, points.size - 1)
                        selectedPointIdx = idx
                    }
                }
            ) {
                val width = size.width
                val height = size.height
                val maxAvg = 10f
                val minAvg = 4f

                val path = Path()
                points.forEachIndexed { index, avg ->
                    val x = if (points.size > 1) index * (width / (points.size - 1)) else width / 2
                    val y = height - ((avg.toFloat() - minAvg) / (maxAvg - minAvg) * height).coerceIn(0f, height)

                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 4.dp.toPx())
                )

                points.forEachIndexed { index, avg ->
                    val x = if (points.size > 1) index * (width / (points.size - 1)) else width / 2
                    val y = height - ((avg.toFloat() - minAvg) / (maxAvg - minAvg) * height).coerceIn(0f, height)
                    drawCircle(
                        color = if (selectedPointIdx == index) primaryColor else Color.White,
                        radius = (if (selectedPointIdx == index) 6.dp else 4.dp).toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun AgendaTabSection(viewModel: MainViewModel, agenda: List<AgendaEventRemoteModel>) {
    val sdfDay = SimpleDateFormat("d", Locale.getDefault())
    val sdfDayOfWeek = SimpleDateFormat("EEE", Locale.getDefault())
    val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val days = remember {
        (-90..90).map {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, it)
            c
        }
    }

    val initialPage = 90
    val pagerState = rememberPagerState(initialPage = initialPage) { days.size }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isCalendarVisible by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemSize = 72.dp
    val itemPadding = 16.dp
    val contentPadding = (screenWidth - itemSize) / 2

    val activeIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) pagerState.currentPage
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { 
                    val itemCenter = it.offset + it.size / 2
                    val diff = itemCenter - viewportCenter
                    if (diff < 0) -diff else diff
                }?.index ?: pagerState.currentPage
            }
        }
    }

    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(activeIndex) {
        if (!isSyncing && listState.isScrollInProgress && !pagerState.isScrollInProgress) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pagerState.scrollToPage(activeIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (!isSyncing && pagerState.currentPage != activeIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(pagerState.currentPage)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentPadding = PaddingValues(horizontal = contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(itemPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(days.size) { index ->
                        val day = days[index]
                        val isSelected = index == pagerState.currentPage

                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            animationSpec = tween(300)
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(300)
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.1f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .size(itemSize)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(bgColor)
                                .clickable {
                                    if (isSelected) {
                                        isCalendarVisible = true
                                    } else {
                                        coroutineScope.launch {
                                            isSyncing = true
                                            val p1 = launch { pagerState.animateScrollToPage(index) }
                                            val p2 = launch { listState.animateScrollToItem(index) }
                                            joinAll(p1, p2)
                                            isSyncing = false
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = sdfDayOfWeek.format(day.time).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            )
                            Text(
                                text = sdfDay.format(day.time),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contentColor
                                )
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val dateStr = sdfFull.format(days[page].time)
                val filteredEvents = agenda.filter { it.evtDatetimeBegin?.startsWith(dateStr) == true }
                val pagerListState = rememberLazyListState()

                if (filteredEvents.isEmpty()) {
                    EmptyState("Nessun impegno per questo giorno")
                } else {
                    LazyColumn(
                        state = pagerListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredEvents) { index, event ->
                            Box(modifier = Modifier.liquidItem(index, pagerListState)) {
                                AgendaItem(event, viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCalendarVisible) {
        Dialog(
            onDismissRequest = { isCalendarVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isCalendarVisible = false },
                contentAlignment = Alignment.Center
            ) {
                CustomCalendarOverlay(
                    initialDate = days[pagerState.currentPage],
                    onDateSelected = { calendar ->
                        val targetDateStr = sdfFull.format(calendar.time)
                        val targetIndex = days.indexOfFirst { sdfFull.format(it.time) == targetDateStr }
                        if (targetIndex != -1) {
                            coroutineScope.launch {
                                isSyncing = true
                                val p1 = launch { pagerState.animateScrollToPage(targetIndex) }
                                val p2 = launch { listState.animateScrollToItem(targetIndex) }
                                joinAll(p1, p2)
                                isSyncing = false
                            }
                        }
                        isCalendarVisible = false
                    },
                    onClose = { isCalendarVisible = false }
                )
            }
        }
    }
}

@Composable
fun CustomCalendarOverlay(
    initialDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    onClose: () -> Unit
) {
    var viewMonth by remember { mutableStateOf(initialDate.clone() as Calendar) }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN).format(viewMonth.time)

    val daysInMonth = remember(viewMonth) {
        val cal = viewMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        List(42) { index ->
            if (index < firstDayOfWeek || index >= firstDayOfWeek + lastDay) {
                null
            } else {
                val day = index - firstDayOfWeek + 1
                val date = cal.clone() as Calendar
                date.set(Calendar.DAY_OF_MONTH, day)
                date
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
            .clickable(enabled = false) {}, // Prevent clicks through to background
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    IconButton(onClick = {
                        val next = viewMonth.clone() as Calendar
                        next.add(Calendar.MONTH, -1)
                        viewMonth = next
                    }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    IconButton(onClick = {
                        val next = viewMonth.clone() as Calendar
                        next.add(Calendar.MONTH, 1)
                        viewMonth = next
                    }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayStr = sdfDay.format(Calendar.getInstance().time)
            val initialDateStr = sdfDay.format(initialDate.time)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                daysInMonth.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isToday = sdfDay.format(day.time) == todayStr
                                    val isSelected = sdfDay.format(day.time) == initialDateStr
                                    
                                    Surface(
                                        onClick = { onDateSelected(day) },
                                        shape = CircleShape,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        },
                                        contentColor = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.get(Calendar.DAY_OF_MONTH).toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(viewModel: MainViewModel, onLogout: () -> Unit) {
    var isAppearanceOpen by remember { mutableStateOf(false) }
    var isAiModelOpen by remember { mutableStateOf(false) }
    var isAboutOpen by remember { mutableStateOf(false) }
    var showExperimentalDialog by remember { mutableStateOf(false) }

    if (showExperimentalDialog) {
        AlertDialog(
            onDismissRequest = { showExperimentalDialog = false },
            title = { Text("Attiva funzioni sperimentali") },
            text = { Text("Le funzioni sperimentali sono instabili e potrebbero causare malfunzionamenti. Vuoi continuare?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleExperimental(true)
                    showExperimentalDialog = false
                }) {
                    Text("Attiva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExperimentalDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    BackHandler(enabled = isAppearanceOpen || isAiModelOpen || isAboutOpen) {
        isAppearanceOpen = false
        isAiModelOpen = false
        isAboutOpen = false
    }

    if (isAppearanceOpen) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopAppBar(
                title = { Text("Aspetto") },
                navigationIcon = {
                    IconButton(onClick = { isAppearanceOpen = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
            ThemeSelectionContent(
                viewModel = viewModel,
                showTitle = false,
                showButton = false
            )
        }
    } else if (isAiModelOpen) {
        AiModelSelectionPage(
            viewModel = viewModel,
            onBack = { isAiModelOpen = false }
        )
    } else if (isAboutOpen) {
        AboutPage(onBack = { isAboutOpen = false })
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (viewModel.studentName.isNotEmpty()) "Ciao, ${viewModel.studentName.split(" ").first()}" else "Impostazioni",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "AI",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Gestisci il tuo registro intelligente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Account Section
            item {
                SettingsCategory("Account") {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Profilo",
                        subtitle = viewModel.studentName,
                        onClick = { /* Implement profile view if needed */ }
                    )
                }
            }

            // Appearance Section
            item {
                SettingsCategory("Aspetto") {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Personalizza Tema",
                        subtitle = "Colori e modalità scura",
                        onClick = { isAppearanceOpen = true }
                    )
                }
            }

            item {
                SettingsCategory("Sperimentazione") {
                    SettingsItem(
                        icon = Icons.Default.Science,
                        title = "Funzioni sperimentali",
                        subtitle = "Accedi a funzionalità in anteprima",
                        trailing = {
                            Switch(
                                checked = viewModel.isExperimentalEnabled,
                                onCheckedChange = {
                                    if (it) showExperimentalDialog = true
                                    else viewModel.toggleExperimental(false)
                                }
                            )
                        },
                        onClick = {
                            if (!viewModel.isExperimentalEnabled) showExperimentalDialog = true
                            else viewModel.toggleExperimental(false)
                        }
                    )
                }
            }

            if (viewModel.isExperimentalEnabled) {
                item {
                    SettingsCategory("Smarty AI") {
                        SettingsItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "Assistente Smarty",
                            subtitle = if (viewModel.isChatEnabled || viewModel.isAiBriefEnabled) "Configurato" else "Configura assistente locale",
                            onClick = { isAiModelOpen = true }
                        )
                    }
                }
            }

            // Info Section
            item {
                SettingsCategory("Informazioni") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Informazioni",
                        subtitle = "Smart Register v1.0",
                        onClick = { isAboutOpen = true }
                    )
                }
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Esci dall'account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun AboutPage(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Informazioni") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Use our custom AppLogo composable
                        AppLogo(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                text = "Smart Register",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "1.0.0",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "STABILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Developer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = com.afloria.smartregister.R.drawable.profile_pic),
                            contentDescription = "Mattia Floria",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mattia Floria",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sviluppatore Capo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SocialButton(
                                icon = Icons.Default.Language,
                                onClick = { uriHandler.openUri("https://floriatechlab.it") },
                                modifier = Modifier.weight(1f)
                            )
                            SocialButton(
                                iconPath = "M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.305-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.372.82 1.102.82 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z", // GitHub
                                onClick = { uriHandler.openUri("https://github.com/mattia-floria") },
                                modifier = Modifier.weight(1f)
                            )
                            SocialButton(
                                iconPath = "M12 2c2.717 0 3.056.01 4.122.06 1.065.05 1.79.22 2.428.47a4.9 4.9 0 0 1 1.77 1.15 4.9 4.9 0 0 1 1.15 1.77c.25.637.42 1.363.47 2.428.05 1.066.06 1.405.06 4.122s-.01 3.056-.06 4.122c-.05 1.065-.22 1.79-.47 2.428a4.9 4.9 0 0 1-1.15 1.77 4.9 4.9 0 0 1-1.77 1.15c-.638.25-1.363.42-2.428.47-1.066.05-1.405.06-4.122.06s-3.056-.01-4.122-.06c-1.065-.05-1.79-.22-2.428-.47a4.9 4.9 0 0 1-1.77-1.15 4.9 4.9 0 0 1-1.15-1.77c-.25-.637-.42-1.363-.47-2.428C2.01 15.056 2 14.717 2 12s.01-3.056.06-4.122c.05-1.065.22-1.79.47-2.428a4.9 4.9 0 0 1 1.15-1.77 4.9 4.9 0 0 1 1.77-1.15c.637-.25 1.363-.42 2.428-.47C8.944 2.01 9.283 2 12 2zm0 1.8c-2.67 0-2.987.01-4.042.059-1.01.045-1.56.213-1.924.354a3.1 3.1 0 0 0-1.144.745 3.1 3.1 0 0 0-.745 1.144c-.14.364-.31.914-.354 1.924C3.81 8.987 3.8 9.33 3.8 12s.01 2.987.059 4.042c.045 1.01.213 1.56.354 1.924a3.1 3.1 0 0 0 .745 1.144 3.1 3.1 0 0 0 1.144.745c.364.14.914.31 1.924.354 1.055.048 1.37.059 4.042.059s2.987-.01 4.042-.059c1.01-.045 1.56-.213 1.924-.354a3.1 3.1 0 0 0 1.144-.745 3.1 3.1 0 0 0 .745-1.144c.14-.364.31-.914.354-1.924.048-1.055.059-1.37.059-4.042s-.01-2.987-.059-4.042c-.045-1.01-.213-1.56-.354-1.924a3.1 3.1 0 0 0-.745-1.144 3.1 3.1 0 0 0-1.144-.745c-.364-.14-.914-.31-1.924-.354-1.055-.048-1.37-.059-4.042-.059zM12 6.865a5.135 5.135 0 1 1 0 10.27 5.135 5.135 0 0 1 0-10.27zm0 1.8a3.335 3.335 0 1 0 0 6.67 3.335 3.335 0 0 0 0-6.67zM17.335 5.465a1.2 1.2 0 1 1 0 2.4 1.2 1.2 0 0 1 0-2.4z", // Instagram Improved Path
                                onClick = { uriHandler.openUri("https://instagram.com/mattia_floria") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Smart Register è un progetto indipendente non affiliato a Spaggiari ClasseViva.",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SocialButton(
    icon: ImageVector? = null,
    iconPath: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vector = remember(icon, iconPath) {
        if (icon != null) return@remember icon
        if (iconPath != null) {
            try {
                ImageVector.Builder(
                    name = "SocialIcon",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f
                ).addPath(
                    pathData = PathParser().parsePathString(iconPath).toNodes(),
                    fill = SolidColor(Color.White),
                    pathFillType = PathFillType.EvenOdd
                ).build()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vector ?: Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AiModelSelectionPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importModelFromUri(it) }
        }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Intelligenza Artificiale", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().liquidFadeEdge(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(modifier = Modifier.liquidItem(0, listState).padding(bottom = 8.dp)) {
                        Text(
                            text = "Potenzia il tuo registro",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Attiva le funzioni avanzate basate su Gemma 3.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Sezione Toggle Funzioni
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().liquidItem(1, listState),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column {
                            SettingsItem(
                                icon = Icons.Default.ChatBubbleOutline,
                                title = "Chat Smarty",
                                subtitle = "Parla con il tuo registro per analisi e consigli.",
                                trailing = {
                                    Switch(
                                        checked = viewModel.isChatEnabled,
                                        onCheckedChange = { viewModel.toggleChat(it) }
                                    )
                                },
                                onClick = { viewModel.toggleChat(!viewModel.isChatEnabled) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            SettingsItem(
                                icon = Icons.Default.AutoAwesome,
                                title = "AI Brief",
                                subtitle = "Riassunto intelligente degli eventi in dashboard.",
                                trailing = {
                                    Switch(
                                        checked = viewModel.isAiBriefEnabled,
                                        onCheckedChange = { viewModel.toggleAiBrief(it) }
                                    )
                                },
                                onClick = { viewModel.toggleAiBrief(!viewModel.isAiBriefEnabled) }
                            )
                        }
                    }
                }

                // Sezione Download Modelli
                itemsIndexed(AiModels.ALL_MODELS) { index, model ->
                    val isSelected = viewModel.selectedAiModelName == model.name
                    val isReady = isSelected && viewModel.isLlmReady
                    val isInitializing = isSelected && viewModel.isLlmInitializing
                    val isDownloading = isSelected && viewModel.isModelDownloading
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .liquidItem(index + 2, listState),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isReady) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                             else if (isInitializing) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                             else if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        onClick = { if (!isSelected) viewModel.switchAiModel(model.name) }
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isReady) MaterialTheme.colorScheme.primary
                                            else if (isInitializing) MaterialTheme.colorScheme.tertiary
                                            else if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isInitializing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onTertiary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                if (isReady) Icons.Default.Check else Icons.Default.CloudDownload,
                                                null,
                                                tint = if (isReady) MaterialTheme.colorScheme.onPrimary 
                                                       else if (isSelected) MaterialTheme.colorScheme.primary
                                                       else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            isReady -> "Pronto all'uso"
                                            isInitializing -> "Inizializzazione in corso..."
                                            isDownloading -> "Scaricamento in corso..."
                                            isSelected -> "Richiesto per le funzioni AI"
                                            else -> model.info
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            isReady -> MaterialTheme.colorScheme.primary
                                            isInitializing -> MaterialTheme.colorScheme.tertiary
                                            isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        }
                                    )
                                }
                            }
                            
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (isDownloading) {
                                    LinearProgressIndicator(
                                        progress = { viewModel.modelDownloadProgress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${(viewModel.modelDownloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                } else if (viewModel.modelDownloadError != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Errore: ${viewModel.modelDownloadError}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.downloadCurrentModel() },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Riprova", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                } else if (!isInitializing) {
                                    Button(
                                        onClick = { viewModel.downloadCurrentModel() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isReady) MaterialTheme.colorScheme.secondary 
                                                             else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(if (isReady) Icons.Default.Refresh else Icons.Default.Download, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val sizeMb = model.sizeInBytes / 1_000_000
                                        Text(if (isReady) "Riscarica Modello" else "Scarica Modello (${sizeMb}MB)")
                                    }
                                    
                                    if (isReady) {
                                        TextButton(
                                            onClick = { viewModel.deleteSelectedModel() },
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Elimina dati modello")
                                        }
                                    } else {
                                        TextButton(
                                            onClick = { launcher.launch(arrayOf("*/*")) },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Importa manualmente (.task)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().liquidItem(AiModels.ALL_MODELS.size + 2, listState),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "L'AI elabora i dati localmente. Nessun dato del registro lascia mai il tuo telefono.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp), content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true) { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .graphicsLayer(alpha = alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun AiInitializationCard(modelName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Inizializzazione AI Locale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$modelName sta caricando i parametri...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun AiModelDownloadCard(modelName: String, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Configurazione AI Locale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scaricamento $modelName: ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}


@Composable
fun GradeItem(grade: GradeRemoteModel, showSubject: Boolean = true) {
    val doesNotCountForAverage = isNonContributing(grade)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = when {
                    doesNotCountForAverage -> Color(0xFF03A9F4) // Light Blue for non-contributing
                    (grade.decimalValue ?: 0.0) >= 6.0 -> Color(0xFF4CAF50) // Green
                    else -> Color(0xFFF44336) // Red
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = grade.displayValue ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                if (showSubject) Text(grade.subjectDesc ?: "Materia", fontWeight = FontWeight.Bold)
                Text(grade.evtDate ?: "", style = MaterialTheme.typography.bodySmall)
                if (doesNotCountForAverage) {
                    Text(
                        text = grade.componentDesc ?: "Non fa media",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!grade.notesForFamily.isNullOrBlank()) {
                    Text(grade.notesForFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardAgendaItem(event: AgendaEventRemoteModel, viewModel: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    event.subjectDesc ?: "Evento",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(event.notes ?: "", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = event.authorName ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(28.dp))
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val context = LocalContext.current
                DropdownMenuItem(
                    text = { Text("Copia") },
                    onClick = {
                        viewModel.copyToClipboard(event.notes ?: "")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                )
                DropdownMenuItem(
                    text = { Text("Condividi") },
                    onClick = {
                        viewModel.shareText(event.notes ?: "")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Share, null) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Spiega con Smarty",
                            color = if (viewModel.isExperimentalEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    onClick = {
                        if (viewModel.isExperimentalEnabled) {
                            viewModel.explainWithSmarty(event)
                            showMenu = false
                        } else {
                            Toast.makeText(
                                context,
                                "L'assistente locale Smarty è disabilitato, puoi attivarlo e impostarlo nelle impostazioni attivando le funzioni sperimentali",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Psychology,
                            null,
                            tint = if (viewModel.isExperimentalEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(
                            if (viewModel.isExperimentalEnabled) MaterialTheme.colorScheme.surfaceContainerLowest
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgendaItem(event: AgendaEventRemoteModel, viewModel: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showMenu = true }
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    event.subjectDesc ?: "Nota",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(event.notes ?: "", style = MaterialTheme.typography.bodyMedium)
                Text(event.authorName ?: "", style = MaterialTheme.typography.labelSmall)
            }
        }

        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(28.dp))
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val context = LocalContext.current
                DropdownMenuItem(
                    text = { Text("Copia") },
                    onClick = {
                        viewModel.copyToClipboard(event.notes ?: "")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                )
                DropdownMenuItem(
                    text = { Text("Condividi") },
                    onClick = {
                        viewModel.shareText(event.notes ?: "")
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Share, null) },
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Spiega con Smarty",
                            color = if (viewModel.isExperimentalEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    onClick = {
                        if (viewModel.isExperimentalEnabled) {
                            viewModel.explainWithSmarty(event)
                            showMenu = false
                        } else {
                            Toast.makeText(
                                context,
                                "L'assistente locale Smarty è disabilitato, puoi attivarlo e impostarlo nelle impostazioni attivando le funzioni sperimentali",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Psychology,
                            null,
                            tint = if (viewModel.isExperimentalEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(
                            if (viewModel.isExperimentalEnabled) MaterialTheme.colorScheme.surfaceContainerLowest
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
fun AbsencesTabSection(absences: List<AbsenceRemoteModel>) {
    val listState = rememberLazyListState()

    val totalAbsences = absences.count { it.evtCode == "ABA0" }
    val totalEarlyExits = absences.count { it.evtCode == "ABU0" }
    val totalDelays = absences.count { it.evtCode == "ABR0" || it.evtCode == "ABR1" }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AbsenceIndicator(totalAbsences, "Assenze", Color(0xFFF44336))
                AbsenceIndicator(totalEarlyExits, "Uscite ant.", Color(0xFFFFC107))
                AbsenceIndicator(totalDelays, "Ritardi", Color(0xFF2196F3))
            }
        }

        item {
            AbsencesChart(absences)
        }

        item {
            Text(
                text = "Giustificate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (absences.isEmpty()) {
            item { EmptyState("Nessun dato sulle assenze") }
        } else {
            itemsIndexed(absences) { index, absence ->
                Box(modifier = Modifier.liquidItem(index + 3, listState)) {
                    AbsenceCard(absence)
                }
            }
        }
    }
}

@Composable
fun AbsenceIndicator(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * (count.toFloat() / 20f).coerceIn(0.1f, 1f),
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AbsencesChart(absences: List<AbsenceRemoteModel>) {
    val months = listOf("SET", "OTT", "NOV", "DIC", "GEN", "FEB", "MAR", "APR", "MAG", "GIU")
    val monthMap = listOf(8, 9, 10, 11, 0, 1, 2, 3, 4, 5)

    val dataAbsences = FloatArray(10)
    val dataExits = FloatArray(10)
    val dataDelays = FloatArray(10)

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()

    absences.forEach { absence ->
        try {
            val date = sdf.parse(absence.evtDate ?: "")
            if (date != null) {
                cal.time = date
                val m = cal.get(Calendar.MONTH)
                val idx = monthMap.indexOf(m)
                if (idx != -1) {
                    when (absence.evtCode) {
                        "ABA0" -> dataAbsences[idx]++
                        "ABU0" -> dataExits[idx]++
                        "ABR0", "ABR1" -> dataDelays[idx]++
                    }
                }
            }
        } catch (e: Exception) {}
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val xStep = width / 9f
                val maxValue = (dataAbsences.maxOrNull() ?: 1f)
                    .coerceAtLeast(dataExits.maxOrNull() ?: 1f)
                    .coerceAtLeast(dataDelays.maxOrNull() ?: 1f)
                    .coerceAtLeast(5f)

                fun drawTypeLine(data: FloatArray, color: Color) {
                    val path = Path()
                    data.forEachIndexed { i, value ->
                        val x = i * xStep
                        val y = height - (value / maxValue * height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                drawTypeLine(dataAbsences, Color(0xFFF44336))
                drawTypeLine(dataExits, Color(0xFFFFC107))
                drawTypeLine(dataDelays, Color(0xFF2196F3))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { month ->
                Text(text = month, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AbsenceCard(absence: AbsenceRemoteModel) {
    val (typeChar, color, description) = when (absence.evtCode) {
        "ABA0" -> Triple("A", Color(0xFFF44336), absence.justifReasonDesc ?: "Assenza")
        "ABU0" -> Triple("E", Color(0xFFFFC107), "Uscita alla ${absence.evtHPos ?: "?"}ª ora")
        "ABR0", "ABR1" -> Triple("L", Color(0xFF2196F3), "Entrata alla ${absence.evtHPos ?: "?"}ª ora")
        else -> Triple("?", Color.Gray, "Evento sconosciuto")
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = typeChar,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = formatDate(absence.evtDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FinalGradesTabSection(reports: List<SchoolReportRemoteModel>, onReportClick: (SchoolReportRemoteModel) -> Unit) {
    val listState = rememberLazyListState()
    if (reports.isEmpty()) {
        EmptyState("Voti finali non ancora pubblicati")
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(reports) { index, report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidItem(index, listState)
                        .clickable { onReportClick(report) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.desc ?: "Documento", fontWeight = FontWeight.Bold)
                            Text("Scrutinio finale", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableTabSection(viewModel: MainViewModel) {
    val days = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab")
    val timetableData by viewModel.timetableData.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<TimetableEntry?>(null) }
    val maxPeriod = 9

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar with Refresh action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settimana Scolastica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.generateTimetableFromAgenda() }) {
                Icon(Icons.Default.AutoMode, "Rigenera", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Horizontal scrolling for the entire table
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                // Header Row (Days)
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 12.dp)
                ) {
                    // Spacer for the "Hour" column
                    Box(modifier = Modifier.width(50.dp))
                    
                    days.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.width(120.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Table Body (Hours)
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 140.dp)
                ) {
                    items(maxPeriod) { index ->
                        val period = index + 1
                        
                        Row(
                            modifier = Modifier
                                .drawWithContent {
                                    drawContent()
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1f
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Column (Fixed style)
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(80.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${period}ª",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Day Columns
                            days.forEachIndexed { dayIdx, _ ->
                                val dayOfWeek = dayIdx + 1
                                val entry = timetableData.entries.find { it.dayOfWeek == dayOfWeek && it.period == period }
                                
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(80.dp)
                                        .background(if (entry != null) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable {
                                            entryToEdit = entry ?: TimetableEntry(dayOfWeek = dayOfWeek, period = period, subjectName = "")
                                            isEditing = true
                                        }
                                        .padding(4.dp)
                                        .drawWithContent {
                                            drawContent()
                                            drawLine(
                                                color = Color.LightGray.copy(alpha = 0.3f),
                                                start = Offset(size.width, 0f),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1f
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (entry != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = entry.subjectName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!entry.room.isNullOrBlank()) {
                                                Text(
                                                    text = entry.room,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    } else {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isEditing && entryToEdit != null) {
        EditTimetableDialog(
            entry = entryToEdit!!,
            onDismiss = { isEditing = false },
            onConfirm = { updated ->
                viewModel.saveTimetableEntry(updated)
                isEditing = false
            }
        )
    }
}



@Composable
fun EditTimetableDialog(entry: TimetableEntry, onDismiss: () -> Unit, onConfirm: (TimetableEntry) -> Unit) {
    var subject by remember { mutableStateOf(entry.subjectName) }
    var room by remember { mutableStateOf(entry.room ?: "") }
    var period by remember { mutableStateOf(entry.period.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lezione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Materia") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Aula (opzionale)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = period, onValueChange = { period = it }, label = { Text("Ora") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = period.toIntOrNull() ?: entry.period
                onConfirm(entry.copy(subjectName = subject, room = room.takeIf { it.isNotBlank() }, period = p))
            }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@Composable
fun AiChatOverlay(viewModel: MainViewModel) {
    var messageText by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            selectedImage = bitmap
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Pulisci chat") },
            text = { Text("Sei sicuro di voler eliminare tutti i messaggi?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChat()
                    showClearConfirmation = false
                }) {
                    Text("Pulisci", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    BackHandler {
        viewModel.isChatOpen = false
    }

    LaunchedEffect(viewModel.chatMessages.size) {
        if (viewModel.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.chatMessages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.isChatOpen = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Assistente locale • ${viewModel.currentModel?.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { showClearConfirmation = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Pulisci chat", tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.chatMessages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chiedimi qualsiasi cosa!",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "L'esecuzione avviene interamente sul tuo dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    items(viewModel.chatMessages) { message ->
                        ChatBubble(message)
                    }
                }

                if (viewModel.isChatLoading) {
                    item {
                        Box(modifier = Modifier.padding(8.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Image Preview if selected
            selectedImage?.let {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp
                    ) {
                        Box {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Selected image",
                                modifier = Modifier.height(100.dp),
                                contentScale = ContentScale.FillHeight
                            )
                            IconButton(
                                onClick = { selectedImage = null },
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.currentModel?.llmSupportImage == true) {
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Aggiungi immagine",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Scrivi un messaggio...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() || selectedImage != null) {
                                viewModel.sendChatMessage(messageText, selectedImage)
                                messageText = ""
                                selectedImage = null
                            }
                        },
                        enabled = (messageText.isNotBlank() || selectedImage != null) && !viewModel.isChatLoading,
                        modifier = Modifier
                            .background(
                                color = if (messageText.isNotBlank() || selectedImage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Invia",
                            tint = if (messageText.isNotBlank() || selectedImage != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val haptic = LocalHapticFeedback.current
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    LaunchedEffect(message.text) {
        if (!message.isUser) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = horizontalAlignment) {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = shape
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    message.image?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Message image",
                            modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

fun formatDate(dateStr: String?): String {
    if (dateStr == null) return ""
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN)
        val date = inputFormat.parse(dateStr)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateStr
    }
}

fun isNonContributing(grade: GradeRemoteModel): Boolean {
    val nonContributingTerms = listOf("non fa media", "voto blu", "blu")
    val contentToCheck = listOfNotNull(
        grade.componentDesc?.lowercase(),
        grade.notesForFamily?.lowercase(),
        grade.displayValue?.lowercase(),
        grade.color?.lowercase()
    )
    return nonContributingTerms.any { term ->
        contentToCheck.any { content -> content.contains(term) }
    } || grade.decimalValue == null
}

fun calculateAverage(grades: List<GradeRemoteModel>): String {
    val validGrades = grades.filter { !isNonContributing(it) }
        .mapNotNull { it.decimalValue }
    if (validGrades.isEmpty()) return "0.0"
    return String.format(Locale.getDefault(), "%.2f", validGrades.average())
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.outline)
    }
}
