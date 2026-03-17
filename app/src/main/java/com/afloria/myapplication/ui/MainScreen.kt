package com.afloria.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afloria.myapplication.data.remote.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Voti", "Agenda", "Bacheca", "Didattica", "Note")
    val icons = listOf(
        Icons.Default.Star,
        Icons.Default.DateRange,
        Icons.Default.Notifications,
        Icons.Default.MenuBook,
        Icons.Default.EditNote
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(items[selectedItem])
                        Text(
                            text = "ClasseViva Expressive",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.surface
        ) {
            when (selectedItem) {
                0 -> GradesSection(viewModel.grades)
                1 -> AgendaSection(viewModel.agenda)
                2 -> NoticeboardSection(viewModel.notices)
                3 -> DidacticsSection(viewModel.teachersMaterials)
                4 -> NotesSection(viewModel.notes)
            }
        }
    }
}

@Composable
fun GradesSection(grades: List<GradeRemoteModel>) {
    if (grades.isEmpty()) EmptyState("Nessun voto presente")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(grades) { grade ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if ((grade.decimalValue ?: 0.0) >= 6.0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = grade.displayValue ?: "?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(grade.subjectDesc ?: "Materia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(grade.evtDate ?: "", style = MaterialTheme.typography.bodySmall)
                        if (!grade.notesForFamily.isNullOrBlank()) {
                            Text(text = grade.notesForFamily, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaSection(events: List<AgendaEventRemoteModel>) {
    if (events.isEmpty()) EmptyState("Agenda vuota")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(events) { event ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = event.evtDatetimeBegin?.take(10) ?: "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(event.notes ?: "Nessuna nota", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(text = " ${event.authorName} • ${event.subjectDesc}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeboardSection(notices: List<NoticeRemoteModel>) {
    if (notices.isEmpty()) EmptyState("Nessun avviso")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(notices) { notice ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(notice.cntTitle ?: "", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(notice.pubDT ?: "") },
                    overlineContent = { Text(notice.cntCategory ?: "CIRCOLARE") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Circolare", tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}

@Composable
fun DidacticsSection(teachers: List<TeacherRemoteModel>) {
    if (teachers.isEmpty()) EmptyState("Nessun materiale dai docenti")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(teachers) { teacher ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(teacher.teacherName ?: "Docente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    teacher.folders.forEach { folder ->
                        ListItem(
                            headlineContent = { Text(folder.folderName ?: "Cartella", fontSize = 14.sp) },
                            supportingContent = { Text("${folder.contents.size} file", fontSize = 12.sp) },
                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFFFC107)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotesSection(notesResponse: NotesResponse?) {
    val allNotes = (notesResponse?.notesNTTE ?: emptyList())
        .plus(notesResponse?.notesNTCL ?: emptyList())
        .plus(notesResponse?.notesNTWN ?: emptyList())
        .plus(notesResponse?.notesNTST ?: emptyList())

    if (allNotes.isEmpty()) EmptyState("Nessuna nota disciplinare")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(allNotes) { note ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Nota", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(note.authorName ?: "Docente", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(note.evtDate ?: "", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(note.getDisplayNote(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Text(text = message, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outline)
        }
    }
}
