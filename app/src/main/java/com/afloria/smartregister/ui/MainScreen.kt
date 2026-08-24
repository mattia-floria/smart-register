package com.afloria.smartregister.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afloria.smartregister.R
import com.afloria.smartregister.data.local.*
import com.afloria.smartregister.data.remote.model.*
import com.afloria.smartregister.ui.components.*
import com.afloria.smartregister.ui.screens.DashboardScreen
import com.afloria.smartregister.ui.theme.*
import kotlin.math.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var currentMoreDetail by remember { mutableStateOf<String?>(null) }

    val selectedTab by remember { derivedStateOf { pagerState.currentPage } }

    BackHandler(enabled = selectedTab != 0 || currentMoreDetail != null) {
        if (currentMoreDetail != null) {
            currentMoreDetail = null
        } else {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ModernBackground(
            seedColor = viewModel.selectedSeedColor ?: MaterialTheme.colorScheme.primary,
            isPureBlack = viewModel.themeMode == ThemeMode.PURE_BLACK
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                ExpressiveNavigationBar {
                    val tabs = listOf(
                        Triple(Icons.Rounded.GridView, "Home", 0),
                        Triple(Icons.Rounded.EventNote, "Agenda", 1),
                        Triple(Icons.AutoMirrored.Rounded.ListAlt, "Voti", 2),
                        Triple(Icons.Rounded.MoreHoriz, "Altro", 3),
                        Triple(Icons.Rounded.Settings, "Impostazioni", 4)
                    )
                    
                    tabs.forEach { (icon, label, index) ->
                        ExpressiveNavigationItem(
                            selected = selectedTab == index,
                            onClick = { 
                                currentMoreDetail = null
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = icon,
                            label = label
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                val bottomPadding = innerPadding.calculateBottomPadding()
                
                AnimatedContent(
                    targetState = currentMoreDetail,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it } + fadeIn())
                                .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) { -it / 2 } + fadeOut())
                        } else {
                            (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) { -it / 2 } + fadeIn())
                                .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessLow)) { it } + fadeOut())
                        }
                    },
                    label = "MainContentTransition",
                    modifier = Modifier.fillMaxSize()
                ) { detail ->
                    if (detail == null) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = true
                        ) { page ->
                            when (page) {
                                0 -> DashboardScreen(viewModel)
                                1 -> AgendaSection(viewModel, isRefreshing, bottomPadding) { viewModel.refreshData() }
                                2 -> RegistrySection(viewModel, isRefreshing, bottomPadding) { viewModel.refreshData() }
                                3 -> MoreSection(viewModel, bottomPadding) { currentMoreDetail = it }
                                4 -> SettingsSection(viewModel, bottomPadding, onLogout)
                            }
                        }
                    } else {
                        when (detail) {
                            "Note e Sanzioni" -> NotesSection(viewModel, bottomPadding) { currentMoreDetail = null }
                            "Circolari" -> NoticesSection(viewModel, bottomPadding) { currentMoreDetail = null }
                            "Materiale Didattico" -> DidacticsSection(viewModel, bottomPadding) { currentMoreDetail = null }
                            "Assenze e Ritardi" -> AbsencesSection(viewModel, bottomPadding) { currentMoreDetail = null }
                            "Scrutini" -> FinalGradesSection(viewModel, bottomPadding) { currentMoreDetail = null }
                            "Orario Lezioni" -> TimetableSection(viewModel, bottomPadding) { currentMoreDetail = null }
                        }
                    }
                }
                
                // Beautiful bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(innerPadding.calculateBottomPadding() + 40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }
        }
        
        // AI Chat Overlay
        if (viewModel.isChatEnabled) {
            FloatingActionButton(
                onClick = { viewModel.isChatOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 24.dp),
                shape = ExpressiveShapes.Squircle,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Rounded.AutoAwesome, "Smarty AI")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrySection(viewModel: MainViewModel, isRefreshing: Boolean, bottomPadding: androidx.compose.ui.unit.Dp, onRefresh: () -> Unit) {
    val grades = viewModel.grades
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
            Text(
                text = "I tuoi Voti",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            
            if (grades.isEmpty()) {
                EmptyState(Icons.Rounded.Inbox, "Nessun voto caricato")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = bottomPadding + 80.dp)
                ) {
                    items(grades, key = { it.evtId ?: it.hashCode() }) { grade ->
                        GradeCard(
                            modifier = Modifier.animateItem(),
                            subject = grade.subjectDesc ?: "Materia",
                            grade = grade.displayValue ?: "?",
                            date = grade.evtDate ?: "",
                            category = grade.componentDesc,
                            backgroundColor = when {
                                (grade.decimalValue ?: 0.0) >= 8.0 -> GradeExcellent
                                (grade.decimalValue ?: 0.0) >= 6.0 -> GradeGood
                                (grade.decimalValue ?: 0.0) >= 5.0 -> GradePass
                                (grade.decimalValue ?: 0.0) >= 4.0 -> GradeFail
                                else -> GradeSevereFail
                            },
                            onClick = { /* Implement detailed view */ }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaSection(viewModel: MainViewModel, isRefreshing: Boolean, bottomPadding: androidx.compose.ui.unit.Dp, onRefresh: () -> Unit) {
    val events = viewModel.agenda
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
            Text(
                text = "La tua Agenda",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            
            if (events.isEmpty()) {
                EmptyState(Icons.Rounded.CalendarToday, "Nulla in programma")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = bottomPadding + 80.dp)
                ) {
                    items(events, key = { it.evtId ?: it.hashCode() }) { event ->
                        ExpressiveCard(
                            modifier = Modifier.animateItem(),
                            shape = ExpressiveShapes.Squircle,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = event.subjectDesc ?: "Evento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.notes ?: "Dettagli non specificati",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = event.evtDatetimeBegin ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun MoreSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Altre Funzioni",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        val notesCount = (viewModel.notes?.notesNTTE?.size ?: 0) +
                        (viewModel.notes?.notesNTCL?.size ?: 0) +
                        (viewModel.notes?.notesNTWN?.size ?: 0) +
                        (viewModel.notes?.notesNTST?.size ?: 0)

        val timetable by viewModel.timetableData.collectAsState()
        val items = listOf(
            Triple("Note e Sanzioni", Icons.Rounded.Description, notesCount),
            Triple("Circolari", Icons.Rounded.Assignment, viewModel.notices.size),
            Triple("Materiale Didattico", Icons.Rounded.School, viewModel.teachersMaterials.size),
            Triple("Assenze e Ritardi", Icons.Rounded.EventBusy, viewModel.absences.size),
            Triple("Scrutini", Icons.Rounded.Newspaper, viewModel.finalGrades.size),
            Triple("Orario Lezioni", Icons.Rounded.Schedule, timetable.entries.size)
        )

        items.forEach { (title, icon, count) ->
            ExpressiveCard(
                onClick = { onNavigate(title) },
                modifier = Modifier.padding(bottom = 12.dp),
                shape = ExpressiveShapes.Squircle,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (count > 0) "$count elementi disponibili" else "Nessun dato caricato", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = if (count > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(bottomPadding + 80.dp))
    }
}







enum class SettingsMenu {
    Main, Appearance, Colors, Fonts, AI, Info, Updates, UpdaterSettings
}

@Composable
fun SettingsSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onLogout: () -> Unit) {
    var currentMenu by remember { mutableStateOf(SettingsMenu.Main) }
    val onBack = { currentMenu = SettingsMenu.Main }

    BackHandler(enabled = currentMenu != SettingsMenu.Main) {
        onBack()
    }

    AnimatedContent(
        targetState = currentMenu,
        transitionSpec = {
            if (targetState != SettingsMenu.Main) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "SettingsMenuTransition"
    ) { menu ->
        when (menu) {
            SettingsMenu.Main -> SettingsMainMenu(viewModel, onLogout, onNavigate = { currentMenu = it })
            SettingsMenu.Appearance -> AppearanceMenu(viewModel, onBack, onNavigate = { currentMenu = it })
            SettingsMenu.Colors -> ColorsMenu(viewModel, onBack)
            SettingsMenu.Fonts -> FontsMenu(viewModel, onBack)
            SettingsMenu.AI -> AiSettingsMenu(viewModel, onBack)
            SettingsMenu.Info -> InfoMenu(onBack, onNavigate = { currentMenu = it })
            SettingsMenu.Updates -> UpdatesMenu(viewModel, bottomPadding, onBack, onNavigate = { currentMenu = it })
            SettingsMenu.UpdaterSettings -> UpdaterSettingsMenu(viewModel, onBack = { currentMenu = SettingsMenu.Updates })
        }
    }
}

@Composable
fun SettingsHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 0.dp)
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SettingsMainMenu(viewModel: MainViewModel, onLogout: () -> Unit, onNavigate: (SettingsMenu) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHeader("Impostazioni")
        
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            ExpressiveCard(
                shape = ExpressiveShapes.ExtraLargeSquircle,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = ExpressiveShapes.Squircle,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = viewModel.studentName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(viewModel.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Profilo Studente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsNavCard("Aspetto", "Temi, colori e font", Icons.Rounded.Palette, onClick = { onNavigate(SettingsMenu.Appearance) })
        SettingsNavCard("Intelligenza Artificiale", "Smarty AI e modelli", Icons.Rounded.AutoAwesome, onClick = { onNavigate(SettingsMenu.AI) })
        SettingsNavCard("Informazioni", "App e sviluppatore", Icons.Rounded.Info, onClick = { onNavigate(SettingsMenu.Info) })

        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = ExpressiveShapes.Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Esci dal Registro", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}







@Composable
fun SettingsNavCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
        ExpressiveCard(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = ExpressiveShapes.Squircle,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun AppearanceMenu(viewModel: MainViewModel, onBack: () -> Unit, onNavigate: (SettingsMenu) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsHeader("Aspetto", onBack)
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            ExpressiveCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modes = listOf(
                        Triple(ThemeMode.SYSTEM, "Sistema", Icons.Rounded.BrightnessAuto),
                        Triple(ThemeMode.LIGHT, "Chiaro", Icons.Rounded.LightMode),
                        Triple(ThemeMode.DARK, "Scuro", Icons.Rounded.DarkMode),
                        Triple(ThemeMode.PURE_BLACK, "Nero", Icons.Rounded.BrightnessLow)
                    )
                    modes.forEach { (mode, label, icon) ->
                        Column(
                            modifier = Modifier.weight(1f).clip(ExpressiveShapes.Squircle)
                                .background(if (viewModel.themeMode == mode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { viewModel.updateTheme(mode) }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, null, tint = if (viewModel.themeMode == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (viewModel.themeMode == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            SettingsNavCard("Palette Colori", "Personalizza i colori", Icons.Rounded.ColorLens, onClick = { onNavigate(SettingsMenu.Colors) })
            SettingsNavCard("Font e Tipografia", "Cambia stile del testo", Icons.Rounded.TextFields, onClick = { onNavigate(SettingsMenu.Fonts) })
        }
    }
}

@Composable
fun ColorsMenu(viewModel: MainViewModel, onBack: () -> Unit) {
    val palettes = listOf(
        ColorPalette("Material", Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260)),
        ColorPalette("Oceano", Color(0xFF0061A4), Color(0xFF535F70), Color(0xFF6B5778)),
        ColorPalette("Smeraldo", Color(0xFF006D3B), Color(0xFF4F6354), Color(0xFF3B6470)),
        ColorPalette("Fuoco", Color(0xFFBF0031), Color(0xFF775652), Color(0xFF715B29)),
        ColorPalette("Zaffiro", Color(0xFF3F51B5), Color(0xFF303F9F), Color(0xFFC5CAE9)),
        ColorPalette("Lavanda", Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFFE1BEE7))
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsHeader("Colori", onBack)
        
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ExpressiveCard(
                onClick = { viewModel.updatePalette(null, null, null) },
                containerColor = if (viewModel.selectedSeedColor == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = ExpressiveShapes.Squircle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Colori Dinamici", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (viewModel.selectedSeedColor == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            Text("Sincronizza con lo sfondo", style = MaterialTheme.typography.bodySmall, color = if (viewModel.selectedSeedColor == null) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (viewModel.selectedSeedColor == null) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

            palettes.chunked(2).forEach { rowPalettes ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowPalettes.forEach { palette ->
                        val isSelected = viewModel.selectedSeedColor == palette.primary
                        ExpressiveCard(
                            onClick = { viewModel.updatePalette(palette.primary, palette.secondary, palette.tertiary) },
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.primary))
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.secondary))
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.tertiary))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(palette.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}







@Composable
fun FontsMenu(viewModel: MainViewModel, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsHeader("Font", onBack)
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpressiveChoiceChip(selected = viewModel.selectedFontFamily == "DEFAULT", label = "Default", onClick = { viewModel.updateFont("DEFAULT") })
                ExpressiveChoiceChip(selected = viewModel.selectedFontFamily == "GOOGLE_SANS", label = "Google Sans Flex", onClick = { viewModel.updateFont("GOOGLE_SANS") })
            }
            if (viewModel.selectedFontFamily == "GOOGLE_SANS") {
                ExpressiveCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text("Configurazione Flex", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    FontSlider("Spessore (Weight)", viewModel.selectedFontWeight, 100f..1000f, 18) { viewModel.updateFontWeight(it) }
                    FontSlider("Larghezza (Width)", viewModel.selectedFontWidth, 50f..150f, 20) { viewModel.updateFontWidth(it) }
                    FontSlider("Arrotondamento (Roundness)", viewModel.selectedFontRond, 0f..100f, 20) { viewModel.updateFontRond(it) }
                    FontSlider("Ottimizzazione (Opsz)", viewModel.selectedFontOpsz, 8f..144f, 34) { viewModel.updateFontOpsz(it) }
                    FontSlider("Grado (Grade)", viewModel.selectedFontGrad, -200f..150f, 35) { viewModel.updateFontGrad(it) }
                }
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}







@Composable
fun InfoMenu(onBack: () -> Unit, onNavigate: (SettingsMenu) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsHeader("Informazioni", onBack)
        
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Card (Exact Copy of Metrolist layout)
            ExpressiveCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                shape = ExpressiveShapes.ExtraLargeSquircle
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                    ) {
                        AppLogo(modifier = Modifier.fillMaxSize().padding(10.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = "Smart Register",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                                Text("1.0.1-experimental", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)) {
                                Text("UNIVERSAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Updates Card
            ExpressiveCard(
                onClick = { onNavigate(SettingsMenu.Updates) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                shape = ExpressiveShapes.ExtraLargeSquircle
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = ExpressiveShapes.Squircle,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aggiornamenti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Controlla nuove versioni", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Developer Card (Mo Agamy style)
            ExpressiveCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                shape = ExpressiveShapes.ExtraLargeSquircle
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Profile Picture (Squircle shape like reference)
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = ExpressiveShapes.ExtraLargeSquircle,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.profile_pic),
                                contentDescription = "Mattia Floria",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Column {
                            Text(
                                text = "Mattia Floria",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Sviluppatore",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Social Row (White Icons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialIconButton(
                            painter = painterResource(id = R.drawable.ic_github),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        ) { 
                            uriHandler.openUri("https://github.com/mattia-floria") 
                        }
                        SocialIconButton(
                            painter = painterResource(id = R.drawable.ic_instagram),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        ) { 
                            uriHandler.openUri("https://instagram.com/mattia_floria") 
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Buy me a coffee button (Copy reference style)
                    Button(
                        onClick = { /* Link */ },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = ExpressiveShapes.Pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Coffee, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Offrimi un caffè!", fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Collaborators Section hidden for now
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun SocialIconButton(painter: androidx.compose.ui.graphics.painter.Painter, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = ExpressiveShapes.Squircle,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painter, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
fun FontSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    }
}

@Composable
fun AiSettingsMenu(viewModel: MainViewModel, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader("AI", onBack)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "prossimamente",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpressiveChoiceChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val font = if (label.contains("Google")) GoogleSansFlex else androidx.compose.ui.text.font.FontFamily.Default
    Surface(
        onClick = onClick,
        shape = ExpressiveShapes.Pill,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = font),
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
        )
    }
}

@Composable
fun NotesSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    val notes = viewModel.notes
    val allNotes = remember(notes) {
        mutableListOf<Pair<String, NoteRemoteModel>>().apply {
            notes?.notesNTTE?.forEach { add("Nota Disciplinare" to it) }
            notes?.notesNTCL?.forEach { add("Nota di Classe" to it) }
            notes?.notesNTWN?.forEach { add("Avviso" to it) }
            notes?.notesNTST?.forEach { add("Annotazione" to it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        SettingsHeader("Note e Sanzioni", onBack)
        if (allNotes.isEmpty()) {
            EmptyState(Icons.Rounded.Description, "Nessuna nota presente")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = bottomPadding + 80.dp)
            ) {
                items(allNotes) { (type, note) ->
                    ExpressiveCard(
                        modifier = Modifier.animateItem(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExpressiveBadge(type, MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(note.evtDate ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(note.getDisplayNote(), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Da: ${note.authorName ?: "Docente"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun NoticesSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    val notices = viewModel.notices
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotices = remember(notices, searchQuery) {
        if (searchQuery.isBlank()) notices
        else notices.filter { 
            it.cntTitle?.contains(searchQuery, ignoreCase = true) == true ||
            it.cntCategory?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    var showDownloadDialog by remember { mutableStateOf<DownloadEvent.Success?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.downloadEvents.collect { event ->
            if (event is DownloadEvent.Success) {
                showDownloadDialog = event
            }
        }
    }

    if (showDownloadDialog != null) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = null },
            shape = ExpressiveShapes.ExtraLargeSquircle,
            icon = { Icon(Icons.Rounded.FileDownloadDone, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Download completato", fontWeight = FontWeight.Black) },
            text = { 
                Text("Il file \"${showDownloadDialog?.fileName}\" è stato salvato nella cartella Download.\n\nVuoi aprirlo ora?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog?.let { viewModel.openFile(it.uri, it.mimeType) }
                        showDownloadDialog = null
                    },
                    shape = ExpressiveShapes.Pill
                ) {
                    Text("Apri file")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = null }) {
                    Text("Chiudi")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (filteredNotices.isEmpty()) {
            EmptyState(Icons.Rounded.Assignment, if (searchQuery.isEmpty()) "Nessuna circolare" else "Nessun risultato trovato")
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 20.dp, 
                    end = 20.dp, 
                    top = 130.dp, // Moved cards higher up
                    bottom = bottomPadding + 100.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        // Transparency mask right above the search bar
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.04f to Color.Transparent, // Fades out exactly behind the header
                                0.12f to Color.Black,       // Fully visible quickly
                                0.92f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                        )
                    }
            ) {
                items(filteredNotices, key = { it.pubId ?: it.hashCode() }) { notice ->
                    // High-end perspective transformation
                    val transformationState = remember(listState) {
                        derivedStateOf {
                            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == (notice.pubId ?: notice.hashCode()) } ?: return@derivedStateOf 1f to 0f
                            val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                            val itemCenter = info.offset + (info.size / 2)
                            val viewportCenter = viewportHeight / 2
                            val distanceFromCenter = (itemCenter - viewportCenter).toFloat()
                            val normalizedDistance = (distanceFromCenter / viewportCenter).coerceIn(-1f, 1f)
                            
                            // Effect kicks in "higher" (more flat area in the middle)
                            val powerDistance = abs(normalizedDistance).pow(2f)
                            val scale = 1f - (powerDistance * 0.03f)
                            val rotation = -sign(normalizedDistance) * powerDistance * 10f 
                            
                            scale to rotation
                        }
                    }
                    val transformation = transformationState.value

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = transformation.first
                                scaleY = transformation.first
                                rotationX = transformation.second
                                cameraDistance = 12f * density
                            }
                    ) {
                        ExpressiveCard(
                            modifier = Modifier.animateItem(),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    notice.cntCategory?.let { 
                                        ExpressiveBadge(it.uppercase(), MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        notice.cntTitle ?: "Senza titolo", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        notice.pubDT ?: "", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (notice.readStatus == false) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 8.dp, top = 4.dp)
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                            
                            if (notice.cntHasAttach == true && !notice.attachments.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                notice.attachments.forEach { attach ->
                                    Surface(
                                        onClick = { viewModel.downloadNoticeAttachment(notice, attach) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = ExpressiveShapes.Squircle,
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.PictureAsPdf, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                attach.fileName ?: "Allegato", 
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                Icons.Rounded.FileDownload, 
                                                null, 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
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

        // Floating Header Overlay (Fully Transparent Area)
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            SettingsHeader("Circolari", onBack)
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp),
                shape = ExpressiveShapes.Pill,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                ),
                tonalElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Rounded.Search, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text(
                                "Cerca circolari...", 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close, 
                                null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun DidacticsSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    val teachers = viewModel.teachersMaterials
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        SettingsHeader("Materiale Didattico", onBack)
        if (teachers.isEmpty()) {
            EmptyState(Icons.Rounded.School, "Nessun materiale")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = bottomPadding + 80.dp)
            ) {
                items(teachers) { teacher ->
                    Text(teacher.teacherName ?: "Docente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
                    teacher.folders.forEach { folder ->
                        folder.contents.forEach { content ->
                            ExpressiveCard(
                                onClick = { viewModel.downloadDidacticFile(content) },
                                modifier = Modifier.padding(vertical = 4.dp).animateItem(),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(content.contentName ?: "File", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(folder.folderName ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
fun AbsencesSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    val absences = viewModel.absences
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        SettingsHeader("Assenze e Ritardi", onBack)
        if (absences.isEmpty()) {
            EmptyState(Icons.Rounded.EventBusy, "Nessun evento registrato")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = bottomPadding + 80.dp)
            ) {
                items(absences) { abs ->
                    ExpressiveCard(
                        modifier = Modifier.animateItem(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val color = if (abs.isJustified == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            ExpressiveBadge(abs.evtCode ?: "EVENTO", color)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(abs.evtDate ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(abs.justifReasonDesc ?: "Motivazione non specificata", style = MaterialTheme.typography.bodyLarge)
                        if (abs.isJustified != true) {
                            Text("DA GIUSTIFICARE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinalGradesSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    val reports = viewModel.finalGrades
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding()) {
        SettingsHeader("Scrutini", onBack)
        if (reports.isEmpty()) {
            EmptyState(Icons.Rounded.Newspaper, "Nessun documento")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = bottomPadding + 80.dp)
            ) {
                items(reports) { report ->
                    ExpressiveCard(
                        onClick = { viewModel.viewFinalReport(report) },
                        modifier = Modifier.animateItem(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(report.desc ?: "Scrutinio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.FileDownload, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableSection(viewModel: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onBack: () -> Unit) {
    val timetable = viewModel.timetableData.collectAsState().value
    val days = listOf("Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).statusBarsPadding().verticalScroll(rememberScrollState())) {
        SettingsHeader("Orario Lezioni", onBack)
        
        days.forEachIndexed { index, day ->
            val dayEntries = timetable.entries.filter { it.dayOfWeek == index + 1 }.sortedBy { it.period }
            Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 12.dp))
            if (dayEntries.isEmpty()) {
                Text("Nessuna lezione", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 8.dp))
            } else {
                dayEntries.forEach { entry ->
                    ExpressiveCard(
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(entry.period.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(entry.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(bottomPadding + 80.dp))
    }
}

data class ColorPalette(val name: String, val primary: Color, val secondary: Color, val tertiary: Color)

@Composable
fun FlappyGameView(viewModel: MainViewModel) {
    val game = viewModel.flappyGame
    val colorOnSurface = MaterialTheme.colorScheme.onSurface
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    val bookEmojis = listOf("📚", "📖", "📕", "📗", "📘")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { viewModel.flapBird() })
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw pipes (Books)
            game.pipes.forEach { pipe ->
                val emojiSize = 40.dp.toPx()
                val pipesCountTop = (pipe.gapY / emojiSize).toInt()
                val pipesCountBottom = ((size.height - (pipe.gapY + pipe.gapHeight)) / emojiSize).toInt()
                
                // Top stack
                for (i in 0 until pipesCountTop) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            textSize = emojiSize
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            bookEmojis[i % bookEmojis.size],
                            pipe.x + pipe.width / 2,
                            pipe.gapY - (i * emojiSize),
                            paint
                        )
                    }
                }
                
                // Bottom stack
                for (i in 0 until pipesCountBottom) {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            textSize = emojiSize
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            bookEmojis[i % bookEmojis.size],
                            pipe.x + pipe.width / 2,
                            (pipe.gapY + pipe.gapHeight + emojiSize) + (i * emojiSize),
                            paint
                        )
                    }
                }
            }
        }
        
        // AppLogo as the bird (Larger)
        val birdX = 150f
        val birdSize = 64f
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { birdX.toDp() },
                    y = with(density) { game.birdY.toDp() }
                )
                .size(with(density) { birdSize.toDp() })
        ) {
            AppLogo(modifier = Modifier.fillMaxSize())
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = game.score.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = colorOnSurface.copy(alpha = 0.8f)
            )
            
            if (game.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .pointerInput(Unit) { detectTapGestures { } }, // Block taps
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "GAME OVER",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.startFlappyGame() },
                            modifier = Modifier.fillMaxWidth(0.6f).height(60.dp),
                            shape = ExpressiveShapes.Pill
                        ) {
                            Text("RIPROVA", fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { viewModel.resetEasterEgg() }
                        ) {
                            Text("Torna a studiare", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesMenu(
    viewModel: MainViewModel, 
    bottomPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit, 
    onNavigate: (SettingsMenu) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!viewModel.isEasterEggActive) {
                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    SettingsHeader("Aggiornamenti", onBack)
                    IconButton(
                        onClick = { onNavigate(SettingsMenu.UpdaterSettings) },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    ) {
                        Icon(Icons.Rounded.MoreVert, null)
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Background Blobs that expand/blur
                AnimatedCircles(
                    isSearching = viewModel.isSearchingForUpdates,
                    isError = viewModel.updateCheckResult == UpdateResult.NO_UPDATES,
                    isEasterEgg = viewModel.isEasterEggActive
                )

                if (!viewModel.isEasterEggActive) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(300.dp)) // Centering compensation
                        
                        if (viewModel.updateCheckResult == UpdateResult.NO_UPDATES) {
                            Text(
                                viewModel.dynamicUpdateMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Black,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                }
            }
            
            // Spacer to keep content away from button, accounting for navigation bar
            if (!viewModel.isEasterEggActive) {
                Spacer(modifier = Modifier.height(bottomPadding + 100.dp))
            }
        }

        if (viewModel.isEasterEggActive) {
            // Fullscreen Game Overlay
            LaunchedEffect(Unit) {
                if (!viewModel.flappyGame.isPlaying) {
                    viewModel.startFlappyGame()
                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = viewModel.flappyGame.isPlaying,
                    enter = fadeIn(tween(1000)),
                    exit = fadeOut()
                ) {
                    FlappyGameView(viewModel)
                }
            }
        }

        // Fixed Button at bottom with proper padding
        if (!viewModel.isEasterEggActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = bottomPadding + 32.dp) 
            ) {
                Button(
                    onClick = { viewModel.checkForUpdates() },
                    enabled = !viewModel.isSearchingForUpdates,
                    shape = ExpressiveShapes.Pill,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    if (viewModel.isSearchingForUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Controlla aggiornamenti", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun UpdaterSettingsMenu(viewModel: MainViewModel, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsHeader("Impostazioni updater", onBack)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpressiveCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nightly builds", fontWeight = FontWeight.Bold)
                        Text("Ricevi versioni sperimentali", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = viewModel.isNightlyEnabled, onCheckedChange = { viewModel.toggleNightly(it) })
                }
            }
            
            ExpressiveCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifiche aggiornamenti", fontWeight = FontWeight.Bold)
                        Text("Avvisami quando ci sono novità", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = viewModel.isUpdateNotificationsEnabled, onCheckedChange = { viewModel.toggleUpdateNotifications(it) })
                }
            }
        }
    }
}

@Composable
fun AnimatedCircles(isSearching: Boolean, isError: Boolean, isEasterEgg: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "CircleTransition")
    
    val searchIntensity by animateFloatAsState(
        targetValue = if (isSearching) 1f else 0f,
        animationSpec = tween(2000, easing = FastOutSlowInEasing),
        label = "SearchIntensity"
    )

    val expansion by animateFloatAsState(
        targetValue = if (isEasterEgg) 4f else 1f,
        animationSpec = tween(2500, easing = FastOutSlowInEasing),
        label = "Expansion"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    val colorBase by animateColorAsState(
        targetValue = when {
            isEasterEgg -> MaterialTheme.colorScheme.primary
            isError -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(1500),
        label = "ColorBase"
    )
    
    val color2Base by animateColorAsState(
        targetValue = when {
            isEasterEgg -> MaterialTheme.colorScheme.tertiary
            isError -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = tween(1500),
        label = "Color2Base"
    )
    
    val color3Base by animateColorAsState(
        targetValue = when {
            isEasterEgg -> MaterialTheme.colorScheme.secondary
            isError -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.tertiary
        },
        animationSpec = tween(1500),
        label = "Color3Base"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .scale(expansion)
            .then(if (isEasterEgg) Modifier.blur(25.dp) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
            rotationZ = (rotation * 0.05f) + (rotation * 0.95f * searchIntensity)
        }) {
            val baseRadius = 85.dp.toPx()
            
            drawSquigglyBlob(
                color = colorBase.copy(alpha = if (isEasterEgg) 0.9f else 0.7f),
                radius = baseRadius * 0.9f,
                centerOffset = Offset(-40.dp.toPx(), -20.dp.toPx()),
                intensity = searchIntensity,
                phase = phase,
                wiggles = 8,
                isEasterEgg = isEasterEgg
            )
            
            drawSquigglyBlob(
                color = color2Base.copy(alpha = if (isEasterEgg) 0.8f else 0.6f),
                radius = baseRadius * 1.1f,
                centerOffset = Offset(45.dp.toPx(), 25.dp.toPx()),
                intensity = searchIntensity,
                phase = -phase * 0.8f,
                wiggles = 10,
                isEasterEgg = isEasterEgg
            )
            
            drawSquigglyBlob(
                color = color3Base.copy(alpha = if (isEasterEgg) 0.7f else 0.5f),
                radius = baseRadius * 0.8f,
                centerOffset = Offset(10.dp.toPx(), 60.dp.toPx()),
                intensity = searchIntensity,
                phase = phase * 1.2f,
                wiggles = 6,
                isEasterEgg = isEasterEgg
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSquigglyBlob(
    color: Color,
    radius: Float,
    centerOffset: Offset,
    intensity: Float,
    phase: Float,
    wiggles: Int,
    isEasterEgg: Boolean = false
) {
    val path = Path()
    val blobCenter = center + centerOffset
    val segments = 120
    
    // Calm down the base amplitude significantly
    val baseAmplitude = if (isEasterEgg) 0f else 3.dp.toPx()
    val searchAmplitude = 12.dp.toPx() * intensity

    for (i in 0 until segments) {
        val angle = (i.toFloat() / segments) * 2f * kotlin.math.PI.toFloat()
        
        val wiggle1 = kotlin.math.sin(angle * wiggles + phase) * baseAmplitude
        val wiggle3 = kotlin.math.sin(angle * (wiggles * 1.5f) + phase * 1.5f) * searchAmplitude
        
        val r = radius + wiggle1 + wiggle3
        
        val x = blobCenter.x + r * kotlin.math.cos(angle)
        val y = blobCenter.y + r * kotlin.math.sin(angle)
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}




