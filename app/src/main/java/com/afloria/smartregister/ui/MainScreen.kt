@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.afloria.smartregister.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.afloria.smartregister.data.remote.model.*
import kotlinx.coroutines.launch
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
                                1 -> AgendaTabSection(viewModel.agenda)
                                2 -> RegistryTabSection(viewModel)
                                3 -> SettingsSection(viewModel, onLogout)
                            }
                        }
                    }
                }
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
                    val itemWidth = maxWidth / 4

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
fun DashboardSection(viewModel: MainViewModel) {
    val tomorrowEvents = viewModel.getTomorrowEvents()
    val average = calculateAverage(viewModel.grades)
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Box {
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
            }
        }

        // Smart AI Brief Section
        item {
            when {
                viewModel.isModelDownloading -> {
                    AiModelDownloadCard(viewModel.selectedAiModel, viewModel.modelDownloadProgress)
                }
                viewModel.isLlmInitializing -> {
                    AiInitializationCard(viewModel.selectedAiModel)
                }
                viewModel.isAiBriefLoading -> {
                    AiBriefLoadingCard()
                }
                viewModel.aiBrief != null -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = viewModel.selectedAiModel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            )
                                        )
                                    )
                            ) {
                                Text(
                                    text = viewModel.aiBrief!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 22.sp
                                    ),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(topStart = 100.dp, bottomEnd = 100.dp, topEnd = 32.dp, bottomStart = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = average,
                            style = TextStyle(
                                fontSize = 92.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        )
                        Text(
                            text = "MEDIA GENERALE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
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
                        DashboardAgendaItem(event)
                    }
                }
            }
        }
    }
}

enum class RegistrySection {
    MENU, GRADES, NOTES, NOTICEBOARD, TEACHER_FILES, ABSENCES, FINAL_GRADES, TIMETABLE
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
        RegistryMenuItem("Orario", Icons.Default.Schedule, RegistrySection.TIMETABLE, Color(0xFF607D8B))
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
            }
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

    if (selectedSubject != null) {
        val subjectGrades = grades.filter { it.subjectDesc == selectedSubject }.sortedBy { it.evtDate }
        SubjectDetailView(
            subjectName = selectedSubject!!,
            grades = subjectGrades,
            onBack = { selectedSubject = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
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

            val filteredGrades = when (selectedGradeTab) {
                1 -> grades.filter {
                    val desc = it.periodDesc?.lowercase() ?: ""
                    desc.contains("1") || desc.contains("primo") || desc.contains("trimestre") || desc.contains("quadrimestre")
                }
                2 -> grades.filter {
                    val desc = it.periodDesc?.lowercase() ?: ""
                    desc.contains("2") || desc.contains("secondo") || desc.contains("pentamestre") || desc.contains("quadrimestre")
                }
                else -> grades
            }.sortedByDescending { it.evtDate }

            if (selectedGradeTab == 3) {
                SubjectsSummaryList(grades) { selectedSubject = it }
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
    if (notices.isEmpty()) {
        EmptyState("Nessuna comunicazione")
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(notices) { index, notice ->
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
}

@Composable
fun DidacticsTabSection(viewModel: MainViewModel, teachers: List<TeacherRemoteModel>) {
    var selectedTeacher by remember { mutableStateOf<TeacherRemoteModel?>(null) }
    var selectedFolder by remember { mutableStateOf<FolderRemoteModel?>(null) }
    val listState = rememberLazyListState()

    BackHandler(enabled = selectedTeacher != null) {
        if (selectedFolder != null) selectedFolder = null
        else selectedTeacher = null
    }

    AnimatedContent(
        targetState = Triple(selectedTeacher, selectedFolder, teachers),
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        },
        label = "DidacticsContentTransition"
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
                    EmptyState("Nessun materiale disponibile")
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 140.dp),
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
}

@Composable
fun SubjectsSummaryList(grades: List<GradeRemoteModel>, onSubjectClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    val grouped = grades.groupBy { it.subjectDesc ?: "Altro" }.toList()
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
fun AgendaTabSection(agenda: List<AgendaEventRemoteModel>) {
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

    val activeIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(activeIndex) {
        if (listState.isScrollInProgress) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pagerState.scrollToPage(activeIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (!listState.isScrollInProgress) {
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
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
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
                                AgendaItem(event)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isCalendarVisible,
            enter = expandIn(expandFrom = Alignment.TopCenter, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkOut(shrinkTowards = Alignment.TopCenter, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
        ) {
            CustomCalendarOverlay(
                initialDate = days[pagerState.currentPage],
                onDateSelected = { calendar ->
                    val targetDateStr = sdfFull.format(calendar.time)
                    val targetIndex = days.indexOfFirst { sdfFull.format(it.time) == targetDateStr }
                    if (targetIndex != -1) {
                        coroutineScope.launch {
                            pagerState.scrollToPage(targetIndex)
                        }
                    }
                    isCalendarVisible = false
                },
                onClose = { isCalendarVisible = false }
            )
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scegli una data",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val next = viewMonth.clone() as Calendar
                            next.add(Calendar.MONTH, -1)
                            viewMonth = next
                        }) {
                            Icon(Icons.Default.ChevronLeft, null)
                        }

                        Text(
                            text = monthName.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )

                        IconButton(onClick = {
                            val next = viewMonth.clone() as Calendar
                            next.add(Calendar.MONTH, 1)
                            viewMonth = next
                        }) {
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(300.dp),
                        userScrollEnabled = false
                    ) {
                        items(daysInMonth) { date ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (date != null) {
                                            Modifier
                                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                                                .clickable { onDateSelected(date) }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (date != null) {
                                    val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date.time) ==
                                            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().time)

                                    Text(
                                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

@Composable
fun SettingsSection(viewModel: MainViewModel, onLogout: () -> Unit) {
    var isAppearanceOpen by remember { mutableStateOf(false) }
    var isAiModelOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = isAppearanceOpen || isAiModelOpen) {
        isAppearanceOpen = false
        isAiModelOpen = false
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
                SettingsCategory("AI & Modelli") {
                    SettingsItem(
                        icon = Icons.Default.Psychology,
                        title = "Modello AI",
                        subtitle = viewModel.selectedAiModel,
                        onClick = { isAiModelOpen = true }
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
fun AiModelSelectionPage(viewModel: MainViewModel, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    
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
                title = { Text("Motori AI", fontWeight = FontWeight.Bold) },
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
                            text = "Scegli il motore dell'assistente",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personalizza l'esperienza di analisi del registro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val models = listOf(
                    "Xiaomi MiMo-V2-Flash" to "Estremamente veloce e leggero. Ottimo per riassunti rapidi dei voti.",
                    "Google Gemma-3 1B" to "Più intelligente e preciso. Eccelle nel ragionamento logico e nel supporto compiti."
                )

                models.forEachIndexed { index, (model, description) ->
                    item {
                        val isSelected = viewModel.selectedAiModel == model
                        val animatedScale by animateFloatAsState(if (isSelected) 1.02f else 1f, label = "cardScale")
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(animatedScale)
                                .liquidItem(index + 1, listState)
                                .clickable {
                                    if (isSelected && viewModel.isModelDownloading) {
                                        // Already downloading feedback
                                    } else {
                                        viewModel.switchAiModel(model)
                                    }
                                },
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                                 else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                )
                                            )
                                        } else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .padding(24.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (model.contains("Gemma")) Icons.Default.Lightbulb else Icons.Default.FlashOn,
                                                    null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = model,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (viewModel.isModelDownloading) "Scaricamento..." else "Motore attivo",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        if (isSelected && !viewModel.isModelDownloading) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 22.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isSelected && viewModel.isModelDownloading) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Column {
                                            LinearProgressIndicator(
                                                progress = { viewModel.modelDownloadProgress },
                                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "${(viewModel.modelDownloadProgress * 100).toInt()}% completato",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    } else if (isSelected) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = { viewModel.switchAiModel(model) },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Riscarica", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().liquidItem(models.size + 1, listState),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "L'esecuzione avviene interamente sul tuo dispositivo per garantire la massima privacy dei tuoi dati scolastici.",
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
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
                    text = "Configurazione Smarty (AI Locale)",
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
fun AiBriefLoadingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                )
            }
        }
    }
}

@Composable
fun GradeItem(grade: GradeRemoteModel, showSubject: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if ((grade.decimalValue ?: 0.0) >= 6.0) Color(0xFF4CAF50) else Color(0xFFF44336),
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
                if (!grade.notesForFamily.isNullOrBlank()) {
                    Text(grade.notesForFamily, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun DashboardAgendaItem(event: AgendaEventRemoteModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
}

@Composable
fun AgendaItem(event: AgendaEventRemoteModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.subjectDesc ?: "Nota", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(event.notes ?: "", style = MaterialTheme.typography.bodyMedium)
            Text(event.authorName ?: "", style = MaterialTheme.typography.labelSmall)
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
    var selectedDay by remember { mutableIntStateOf(1) }
    val days = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab")
    val timetableData by viewModel.timetableData.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<TimetableEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedDay - 1,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                modifier = Modifier.weight(1f)
            ) {
                days.forEachIndexed { index, day ->
                    Tab(
                        selected = selectedDay == index + 1,
                        onClick = { selectedDay = index + 1 },
                        text = { Text(day) }
                    )
                }
            }
            
            IconButton(onClick = { viewModel.generateTimetableFromAgenda() }) {
                Icon(Icons.Default.Refresh, "Rigenera orario", tint = MaterialTheme.colorScheme.primary)
            }
        }

        val dayEntries = timetableData.entries.filter { it.dayOfWeek == selectedDay }.sortedBy { it.period }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (dayEntries.isEmpty()) {
                EmptyState("Nessuna lezione inserita")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(dayEntries) { entry ->
                        TimetableEntryCard(entry, onEdit = {
                            entryToEdit = it
                            isEditing = true
                        }, onDelete = {
                            viewModel.deleteTimetableEntry(it)
                        })
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    entryToEdit = TimetableEntry(dayOfWeek = selectedDay, period = (dayEntries.maxOfOrNull { it.period } ?: 0) + 1, subjectName = "")
                    isEditing = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 140.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, "Aggiungi")
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
fun TimetableEntryCard(entry: TimetableEntry, onEdit: (TimetableEntry) -> Unit, onDelete: (TimetableEntry) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "${entry.period}ª", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!entry.room.isNullOrBlank()) {
                    Text(entry.room, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = { onEdit(entry) }) {
                Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = { onDelete(entry) }) {
                Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
            }
        }
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

private fun formatDate(dateStr: String?): String {
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

private fun calculateAverage(grades: List<GradeRemoteModel>): String {
    val validGrades = grades.mapNotNull { it.decimalValue }
    if (validGrades.isEmpty()) return "0.0"
    return String.format(Locale.getDefault(), "%.2f", validGrades.average())
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.outline)
    }
}
