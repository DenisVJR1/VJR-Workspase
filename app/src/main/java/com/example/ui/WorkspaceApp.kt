package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Task
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkspaceApp(viewModel: WorkspaceViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val launcherWallpaper by viewModel.launcherWallpaper.collectAsState()

    // Determine wallpaper gradient based on theme
    val wallpaperGradient = when (launcherWallpaper) {
        "Lavender Mint" -> Brush.linearGradient(
            colors = listOf(Color(0xFF2E1065), Color(0xFF0F172A), Color(0xFF0D9488))
        )
        "Midnight Gold" -> Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFFF59E0B))
        )
        "Organic Emerald" -> Brush.linearGradient(
            colors = listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF10B981))
        )
        else -> Brush.linearGradient( // "Dynamic Slate"
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF3F3F46))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaperGradient)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { -it / 2 })
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                Screen.Dashboard -> LauncherDashboardScreen(viewModel)
                Screen.Calendar -> CalendarViewScreen(viewModel)
                Screen.Tasks -> TasksViewScreen(viewModel)
                Screen.FileViewer -> FileViewerScreen(viewModel)
                Screen.FileEditor -> FileEditorScreen(viewModel)
                Screen.CloudAuth -> CloudAuthScreen(viewModel)
                Screen.Pomodoro -> PomodoroScreen(viewModel)
            }
        }
    }
}

// ----------------------------------------------------
// 1. MATERIAL YOU LAUNCHER DASHBOARD
// ----------------------------------------------------
@Composable
fun LauncherDashboardScreen(viewModel: WorkspaceViewModel) {
    val allTasks by viewModel.allTasks.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val launcherWallpaper by viewModel.launcherWallpaper.collectAsState()

    var showWallpaperDialog by remember { mutableStateOf(false) }

    // Live ticking clock in Ukrainian
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadFolderItems() // Refresh file list
        while (true) {
            val cal = Calendar.getInstance()
            val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateSdf = SimpleDateFormat("EEEE, d MMMM", Locale("uk"))
            currentTimeString = timeSdf.format(cal.time)
            currentDateString = dateSdf.format(cal.time).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    val todayTasks = remember(allTasks) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        allTasks.filter { it.date == todayStr }
    }
    val todayCompletedCount = todayTasks.count { it.isCompleted }
    val todayTotalCount = todayTasks.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. BRAND HEADER ROW
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (viewModel.cloudUserEmail.collectAsState().value != null) Color(0xFF38BDF8) else Color(0xFF4ADE80),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VJR WORKSPACE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Cloud Auth & Sync Button
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.CloudAuth) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("launcher_cloud_button")
                    ) {
                        Icon(
                            imageVector = if (viewModel.cloudUserEmail.collectAsState().value != null) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = "Cloud Sync & Auth",
                            tint = if (viewModel.cloudUserEmail.collectAsState().value != null) Color(0xFF38BDF8) else Color.White.copy(0.7f)
                        )
                    }

                    // Launcher settings gear to change wallpaper gradients
                    IconButton(
                        onClick = { showWallpaperDialog = true },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("launcher_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change Theme Wallpaper",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // 2. TICKING TIME AND DATE WIDGET
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = currentTimeString,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = currentDateString,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Global Search Widget
        item {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Пошук завдань та файлів у кабінеті...", color = Color.White.copy(0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(0.25f), RoundedCornerShape(24.dp))
                    .testTag("launcher_search_bar"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search icon", tint = Color.White)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color.White)
                        }
                    }
                }
            )
        }

        // Live Search Results Drop-down
        if (searchQuery.isNotBlank()) {
            val matchingTasks = allTasks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
            val matchingFiles = folderItems.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }

            if (matchingTasks.isNotEmpty() || matchingFiles.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Результати пошуку:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Found Files block
                            if (matchingFiles.isNotEmpty()) {
                                Text(
                                    "Файли (${matchingFiles.size}):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold
                                )
                                matchingFiles.take(4).forEach { file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openFileForEditing(file) }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            file.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (matchingFiles.isNotEmpty() && matchingTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Found Tasks block
                            if (matchingTasks.isNotEmpty()) {
                                Text(
                                    "Завдання (${matchingTasks.size}):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Green,
                                    fontWeight = FontWeight.Bold
                                )
                                matchingTasks.take(4).forEach { task ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.navigateTo(Screen.Tasks) }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = if (task.isCompleted) Color.Green else Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                task.title,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            task.date,
                                            color = Color.White.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Нічого не знайдено за запитом \"$searchQuery\"",
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }

        // Widget 1: Tasks Progress card Widget
        item {
            Card(
                onClick = { viewModel.navigateTo(Screen.Tasks) },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(24.dp))
                    .testTag("tasks_widget_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Task circular ring indicator (Material Design)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        val progress = if (todayTotalCount > 0) todayCompletedCount.toFloat() / todayTotalCount else 0f
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                style = Stroke(width = 6.dp.toPx())
                            )
                            drawArc(
                                color = Color(0xFF4ADE80),
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx())
                            )
                        }
                        Text(
                            text = if (todayTotalCount > 0) "${(progress * 100).toInt()}%" else "0%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Завдання на сьогодні",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (todayTotalCount > 0) {
                                "Виконано $todayCompletedCount з $todayTotalCount завдань"
                            } else {
                                "Сьогодні немає запланованих справ"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Додати завдання",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Widget 2: Embedded Calendar Grid Widget
        item {
            Card(
                onClick = { viewModel.navigateTo(Screen.Calendar) },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(24.dp))
                    .testTag("calendar_widget_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.Yellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Вбудований календар",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            "Дивитися всі >",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // mini calendar grid represent current days
                    val days = remember(allTasks) {
                        val cal = Calendar.getInstance()
                        val currDay = cal.get(Calendar.DAY_OF_MONTH)
                        // list surrounding +/- 3 days
                        val items = mutableListOf<Pair<Int, Boolean>>() // Day to HasTasks
                        val todayStrYmd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        for (offset in -3..3) {
                            val temp = Calendar.getInstance()
                            temp.add(Calendar.DAY_OF_YEAR, offset)
                            val day = temp.get(Calendar.DAY_OF_MONTH)
                            val dayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(temp.time)
                            val hasTasks = allTasks.any { it.date == dayStr && !it.isCompleted }
                            items.add(Pair(day, hasTasks))
                        }
                        items
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        days.forEachIndexed { idx, pair ->
                            val isCenter = idx == 3 // index of today
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCenter) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = pair.first.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCenter) Color.Yellow else Color.White,
                                    fontWeight = if (isCenter) FontWeight.ExtraBold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (pair.second) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Widget 3: File Storage Sandbox Widget
        item {
            Card(
                onClick = { viewModel.navigateTo(Screen.FileViewer) },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(24.dp))
                    .testTag("file_explorer_widget_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFFFFC107))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Кабінет файлів",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Фізичний провідник уfilesDir. Переглядайте та створюйте робочі тексти.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Знайдено файлів: ${folderItems.size}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF60A5FA)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Відкрити Провідник",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // App Shortcuts Grid representing the "Material U Launcher Dock"
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "Робочий док",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LauncherDockItem(
                        icon = Icons.Filled.CalendarMonth,
                        label = "Календар",
                        tint = Color(0xFFF0ABFC),
                        onClick = { viewModel.navigateTo(Screen.Calendar) }
                    )
                    LauncherDockItem(
                        icon = Icons.Filled.Assignment,
                        label = "Завдання",
                        tint = Color(0xFF86EFAC),
                        onClick = { viewModel.navigateTo(Screen.Tasks) }
                    )
                    LauncherDockItem(
                        icon = Icons.Filled.Folder,
                        label = "Файли",
                        tint = Color(0xFF93C5FD),
                        onClick = { viewModel.navigateTo(Screen.FileViewer) }
                    )
                    LauncherDockItem(
                        icon = Icons.Filled.Timer,
                        label = "Pomodoro",
                        tint = Color(0xFFFF8A8A),
                        onClick = { viewModel.navigateTo(Screen.Pomodoro) }
                    )
                    LauncherDockItem(
                        icon = Icons.Filled.CloudQueue,
                        label = "VJR Хмара",
                        tint = Color(0xFF7DD3FC),
                        onClick = { viewModel.navigateTo(Screen.CloudAuth) }
                    )
                }
            }
        }
    }

    // Dialog for Theme Gradient Wallpapers
    if (showWallpaperDialog) {
        Dialog(onDismissRequest = { showWallpaperDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Оберіть тему Material You",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val options = listOf("Dynamic Slate", "Lavender Mint", "Midnight Gold", "Organic Emerald")
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setWallpaper(option)
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(option, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            if (launcherWallpaper == option) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.Green)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LauncherDockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}


// ----------------------------------------------------
// 2. EMBEDDED CALENDAR SCREEN
// ----------------------------------------------------
@Composable
fun CalendarViewScreen(viewModel: WorkspaceViewModel) {
    val calendarYear by viewModel.calendarYear.collectAsState()
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var inputTaskTitle by remember { mutableStateOf("") }
    var inputTaskPriority by remember { mutableStateOf("Medium") }

    val daysGrid = remember(calendarYear, calendarMonth) {
        viewModel.getDaysGrid()
    }

    val UA_WEEK_DAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")

    // Filter tasks for the selected date
    val selectedDateTasks = remember(selectedDate, allTasks) {
        allTasks.filter { it.date == selectedDate }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App header back-bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад до Кабінету", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Календар Справ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Calendar selector row
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.prevMonth() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Попередній місяць", tint = Color.White)
                }

                Text(
                    text = "${getUkrainianMonthName(calendarMonth)} $calendarYear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Наступний місяць", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid weekday labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            UA_WEEK_DAYS.forEach { dayName ->
                Text(
                    text = dayName,
                    color = if (dayName == "Сб" || dayName == "Нд") Color.Red.copy(0.8f) else Color.White.copy(0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days Grid Calendar
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(daysGrid) { dateItem ->
                    val isSelected = (selectedDate == dateItem.dateString)

                    // Check if this date has any pending tasks to display status dots
                    val dateTasks = allTasks.filter { it.date == dateItem.dateString }
                    val hasCompleted = dateTasks.any { it.isCompleted }
                    val hasActive = dateTasks.any { !it.isCompleted }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1.1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> Color.White.copy(alpha = 0.28f)
                                    dateItem.isToday -> Color.Yellow.copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.05f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isSelected -> Color.Yellow
                                    dateItem.isToday -> Color.Yellow.copy(alpha = 0.5f)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectDate(dateItem.dateString) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dateItem.day.toString(),
                                fontWeight = if (dateItem.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 15.sp,
                                color = when {
                                    !dateItem.isCurrentMonth -> Color.White.copy(alpha = 0.35f)
                                    dateItem.isToday -> Color.Yellow
                                    else -> Color.White
                                }
                            )

                            // Unified dots indicator under date number
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (dateTasks.isNotEmpty()) {
                                    val topPriority = dateTasks.maxByOrNull {
                                        when (it.priority) {
                                            "High" -> 3
                                            "Medium" -> 2
                                            else -> 1
                                        }
                                    }?.priority

                                    val dotColor = when (topPriority) {
                                        "High" -> Color.Red
                                        "Medium" -> Color.Yellow
                                        else -> Color.Green
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(dotColor, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Daily task schedule detail box
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Справи на: ${getFullUkrainianDateString(selectedDate)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Add task shortcut button
                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Додати", tint = Color.Green)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedDateTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Assignment,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "На цей день планів немає",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedDateTasks) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = when (task.priority) {
                                            "High" -> Color.Red.copy(0.4f)
                                            "Medium" -> Color.Yellow.copy(0.4f)
                                            else -> Color.Green.copy(0.4f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.toggleTaskCompletion(task) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "Виконати",
                                    tint = if (task.isCompleted) Color.Green else Color.White.copy(0.4f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = if (task.isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                                    )
                                    if (task.description.isNotBlank()) {
                                        Text(
                                            text = task.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteTask(task) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Task addition Dialog
    if (showAddTaskDialog) {
        Dialog(onDismissRequest = { showAddTaskDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Створити нове завдання",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputTaskTitle,
                        onValueChange = { inputTaskTitle = it },
                        label = { Text("Назва", color = Color.White.copy(0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Yellow,
                            unfocusedBorderColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_task_title_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Пріоритет:", color = Color.White, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Low" to "Низький", "Medium" to "Середній", "High" to "Високий").forEach { (priCode, priLabel) ->
                            Row(
                                modifier = Modifier.clickable { inputTaskPriority = priCode },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (inputTaskPriority == priCode),
                                    onClick = { inputTaskPriority = priCode },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color.Yellow)
                                )
                                Text(priLabel, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddTaskDialog = false }) {
                            Text("Скасувати", color = Color.White.copy(0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (inputTaskTitle.isNotBlank()) {
                                    viewModel.addTask(
                                        title = inputTaskTitle,
                                        description = "",
                                        date = selectedDate,
                                        priority = inputTaskPriority
                                    )
                                    inputTaskTitle = ""
                                    showAddTaskDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                        ) {
                            Text("Зберегти")
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. TASK PLANNER HUB SCREEN
// ----------------------------------------------------
@Composable
fun TasksViewScreen(viewModel: WorkspaceViewModel) {
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedYmdDate by viewModel.selectedDate.collectAsState()

    var activeTab by remember { mutableStateOf("Active") } // "Active", "Completed", "All"

    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("Medium") }

    // Task completion statistic
    val completedCount = allTasks.count { it.isCompleted }
    val totalCount = allTasks.size
    val completionFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0F

    val filteredTasks = remember(allTasks, activeTab) {
        when (activeTab) {
            "Active" -> allTasks.filter { !it.isCompleted }
            "Completed" -> allTasks.filter { it.isCompleted }
            else -> allTasks
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App header back-bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад до Кабінету", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Організатор Завдань",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Circular dynamic gauge indicator card (Visual Craft)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Загальний прогрес",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Виконано $completedCount з $totalCount справ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White.copy(0.12f),
                            style = Stroke(width = 5.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = -90f,
                            sweepAngle = completionFraction * 360f,
                            useCenter = false,
                            style = Stroke(width = 5.dp.toPx())
                        )
                    }
                    Text(
                        text = "${(completionFraction * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Fast creation expander
        var isCreaterExpanded by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCreaterExpanded = !isCreaterExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, tint = Color.Green, contentDescription = "Add symbol")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Створити Справу швидко",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = if (isCreaterExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        tint = Color.White,
                        contentDescription = "Toggle add"
                    )
                }

                if (isCreaterExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Заголовок справи", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Yellow,
                            unfocusedBorderColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("task_input_title")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        label = { Text("Опис/Примітка", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Yellow,
                            unfocusedBorderColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Priority row selectors
                    Text("Пріоритет плану:", color = Color.White.copy(0.8f), fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Low" to "Низький", "Medium" to "Середній", "High" to "Високий").forEach { (code, label) ->
                            Row(
                                modifier = Modifier.clickable { taskPriority = code },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (taskPriority == code),
                                    onClick = { taskPriority = code },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color.Yellow)
                                )
                                Text(label, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Due date notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Дата виконання: $selectedYmdDate",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "(Натисніть на Календарі день)",
                            color = Color.Yellow.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                viewModel.addTask(
                                    title = taskTitle,
                                    description = taskDesc,
                                    date = selectedYmdDate,
                                    priority = taskPriority
                                )
                                taskTitle = ""
                                taskDesc = ""
                                isCreaterExpanded = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().testTag("task_save_button")
                    ) {
                        Text("Зберегти завдання")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs Sorting Header
        TabRow(
            selectedTabIndex = when (activeTab) {
                "Active" -> 0
                "Completed" -> 1
                else -> 2
            },
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.Yellow,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(12.dp))
        ) {
            Tab(selected = activeTab == "Active", onClick = { activeTab = "Active" }) {
                Text("Активні", modifier = Modifier.padding(12.dp), color = Color.White)
            }
            Tab(selected = activeTab == "Completed", onClick = { activeTab = "Completed" }) {
                Text("Виконані", modifier = Modifier.padding(12.dp), color = Color.White)
            }
            Tab(selected = activeTab == "All", onClick = { activeTab = "All" }) {
                Text("Всі", modifier = Modifier.padding(12.dp), color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Task contents lists
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Assignment,
                        contentDescription = null,
                        tint = Color.White.copy(0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Список порожній",
                        color = Color.White.copy(0.5f),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTasks) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = when (task.priority) {
                                    "High" -> Color.Red.copy(0.4f)
                                    "Medium" -> Color.Yellow.copy(0.4f)
                                    else -> Color.Green.copy(0.4f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.toggleTaskCompletion(task) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = "Toggle Complete",
                            tint = if (task.isCompleted) Color(0xFF10B981) else Color.White.copy(0.4f),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = if (task.isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Термін: ${getFullUkrainianDateString(task.date)}",
                                fontSize = 11.sp,
                                color = Color.White.copy(0.5f)
                            )
                        }

                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete from list", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// 4. SANDBOX FILE VIEWER
// ----------------------------------------------------
@Composable
fun FileViewerScreen(viewModel: WorkspaceViewModel) {
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()

    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }

    var inputDirectoryName by remember { mutableStateOf("") }
    var inputFileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад до Кабінету", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Файловий Кабінет",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Current Directory Header Visual
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color(0xFFFFB300))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.getCurrentDirectoryRelativePath(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Navigate Up button
                if (!viewModel.isAtRoot()) {
                    TextButton(
                        onClick = { viewModel.navigateUp() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Yellow)
                    ) {
                        Text("^ Вгору")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Create folder / Create document action row buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showCreateDirDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.18f), contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "Create dir")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Нова папка", fontSize = 13.sp)
            }

            Button(
                onClick = { showCreateFileDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.18f), contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(10.dp)
            ) {
                Icon(Icons.Filled.NoteAdd, contentDescription = "Create file")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Новий файл", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Listing workspace details cards
        if (folderItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = Color.White.copy(0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Папка завершена або порожня",
                        color = Color.White.copy(0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(folderItems) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .clickable {
                                if (file.isDirectory) {
                                    viewModel.changeDirectory(file)
                                } else {
                                    viewModel.openFileForEditing(file)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                contentDescription = null,
                                tint = if (file.isDirectory) Color(0xFFFFB300) else Color(0xFF64B5F6),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (file.isDirectory) "Папка" else "${(file.length() / 1024.0).let { "%.2f".format(it) }} KB",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteFileOnDisk(file) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete File", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    // New folder dialog
    if (showCreateDirDialog) {
        Dialog(onDismissRequest = { showCreateDirDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Створити нову папку",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputDirectoryName,
                        onValueChange = { inputDirectoryName = it },
                        label = { Text("Назва папки", color = Color.White.copy(0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Yellow,
                            unfocusedBorderColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_folder_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateDirDialog = false }) {
                            Text("Скасувати", color = Color.White.copy(0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (inputDirectoryName.isNotBlank()) {
                                    viewModel.createFolder(inputDirectoryName)
                                    inputDirectoryName = ""
                                    showCreateDirDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                        ) {
                            Text("Створити")
                        }
                    }
                }
            }
        }
    }

    // New file dialog
    if (showCreateFileDialog) {
        Dialog(onDismissRequest = { showCreateFileDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Створити новий документ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputFileName,
                        onValueChange = { inputFileName = it },
                        label = { Text("Назва файлу (напр. План.txt)", color = Color.White.copy(0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Yellow,
                            unfocusedBorderColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_file_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateFileDialog = false }) {
                            Text("Скасувати", color = Color.White.copy(0.6f))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (inputFileName.isNotBlank()) {
                                    viewModel.createTextFile(inputFileName, "")
                                    inputFileName = ""
                                    showCreateFileDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                        ) {
                            Text("Створити")
                        }
                    }
                }
            }
        }
    }
}


// ----------------------------------------------------
// 5. TEXT FILE EDITOR & WRITER
// ----------------------------------------------------
@Composable
fun FileEditorScreen(viewModel: WorkspaceViewModel) {
    val activeFile by viewModel.activeFile.collectAsState()
    val activeFileContent by viewModel.activeFileContent.collectAsState()

    var textBuffer by remember(activeFileContent) { mutableStateOf(activeFileContent) }
    var changeNotedIndicator by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // App header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = {
                        viewModel.loadFolderItems() // Reload and navigate
                        viewModel.navigateTo(Screen.FileViewer)
                    },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад до Кабінету", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = activeFile?.name ?: "Редактор",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick save symbol feedback
            IconButton(
                onClick = {
                    viewModel.saveActiveFileContent(textBuffer)
                    changeNotedIndicator = true
                },
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape).testTag("editor_save_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Зберегти файл",
                    tint = if (changeNotedIndicator) Color.Green else Color.White
                )
            }
        }

        // Warning or helpful editing prompt helper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(Color.White.copy(0.06f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Тексти зберігаються фізично на пристрої.",
                fontSize = 11.sp,
                color = Color.White.copy(0.7f)
            )
        }

        // Fullscreen editor field
        OutlinedTextField(
            value = textBuffer,
            onValueChange = {
                textBuffer = it
                changeNotedIndicator = false
            },
            placeholder = { Text("Введіть текст роботи...", color = Color.White.copy(0.5f)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("editor_text_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}


// Local Month Names Translation Helper
private fun getUkrainianMonthName(month: Int): String {
    return when (month) {
        1 -> "Січень"
        2 -> "Лютий"
        3 -> "Березень"
        4 -> "Квітень"
        5 -> "Травень"
        6 -> "Червень"
        7 -> "Липень"
        8 -> "Серпень"
        9 -> "Вересень"
        10 -> "Жовтень"
        11 -> "Листопад"
        12 -> "Грудень"
        else -> ""
    }
}

// Local Helper to Translate date into Ukrainian phrase
private fun getFullUkrainianDateString(dateStr: String): String {
    try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthInt = parts[1].toInt()
            val dayInt = parts[2].toInt()
            val monthName = when (monthInt) {
                1 -> "січня"
                2 -> "лютого"
                3 -> "березня"
                4 -> "квітня"
                5 -> "травня"
                6 -> "червня"
                7 -> "липня"
                8 -> "серпня"
                9 -> "вересня"
                10 -> "жовтня"
                11 -> "листопада"
                12 -> "грудня"
                else -> ""
            }
            return "$dayInt $monthName $year р."
        }
    } catch (e: Exception) {
        // Fail silently
    }
    return dateStr
}


// ----------------------------------------------------
// 6. EXCLUSIVE VJR CLOUD SERVICES SCREEN
// ----------------------------------------------------
@Composable
fun CloudAuthScreen(viewModel: WorkspaceViewModel) {
    val cloudUserEmail by viewModel.cloudUserEmail.collectAsState()
    val firebaseApiKey by viewModel.firebaseApiKey.collectAsState()
    val firebaseDbUrl by viewModel.firebaseDbUrl.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf(firebaseApiKey) }
    var dbUrlInput by remember { mutableStateOf(firebaseDbUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Dashboard) },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "VJR Cloud Синхронізація",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Card 1: Cloud Credentials Session Control
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.11f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Хмарний Кабінет",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (cloudUserEmail == null) {
                        // Logout View -> Show Login/Register Forms
                        Text(
                            "Увійдіть у захищений кабінет VJR, щоб автоматично копіювати завдання, розклади та текстові файли на зашифрований клієнт-диск.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Електронна пошта", color = Color.White.copy(0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color.White.copy(0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Пароль", color = Color.White.copy(0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color.White.copy(0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { 
                                    val ok = viewModel.userLogin(emailInput, passwordInput, false)
                                    if (ok) {
                                        emailInput = ""
                                        passwordInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Увійти")
                            }

                            OutlinedButton(
                                onClick = {
                                    val ok = viewModel.userLogin(emailInput, passwordInput, true)
                                    if (ok) {
                                        emailInput = ""
                                        passwordInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Створити")
                            }
                        }
                    } else {
                        // Login Active State
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF38BDF8), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Авторизовано як:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = cloudUserEmail ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Остання синхронізація:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                                Text(lastSyncTime, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }

                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { viewModel.triggerCloudSync() },
                                    modifier = Modifier.background(Color(0xFF0EA5E9), CircleShape)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Синхронізувати", tint = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.White.copy(0.12f))

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.userLogout() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.15f), contentColor = Color.Red),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Вийти з профілю VJR")
                        }
                    }
                }
            }
        }

        // Card 2: Custom Cloud Coordinates config
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                var isExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsInputComponent, contentDescription = null, tint = Color(0xFFC084FC))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Налаштування Firebase",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Розгорнути",
                            tint = Color.White
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ви можете вказати координати вашої власної бази Firebase, щоб додаток зберігав дані прямо у вашу хмару, минаючи сторонні сервери.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = dbUrlInput,
                            onValueChange = { dbUrlInput = it },
                            label = { Text("Firebase Database URL", color = Color.White.copy(0.5f)) },
                            placeholder = { Text("https://your-db-instance.firebaseio.com") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC084FC),
                                unfocusedBorderColor = Color.White.copy(0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Web API Key (необов'язково)", color = Color.White.copy(0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC084FC),
                                unfocusedBorderColor = Color.White.copy(0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { 
                                viewModel.saveFirebaseConfig(apiKeyInput, dbUrlInput)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Зберегти Ключі", color = Color.White)
                        }
                    }
                }
            }
        }

        // Card 3: Live Sync Console Logs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = Color(0xFF4ADE80))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Консоль Зв'язку Cloud VJR",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Black.copy(0.3f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        syncLogs.takeLast(5).forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (log.contains("Помилка")) Color.Red else Color(0xFFADF0C0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 7. EXCLUSIVE POMODORO RETREAT SCREEN
// ----------------------------------------------------
@Composable
fun PomodoroScreen(viewModel: WorkspaceViewModel) {
    val timeLeft by viewModel.pomodoroTimeLeftSec.collectAsState()
    val isRunning by viewModel.pomodoroIsRunning.collectAsState()
    val mode by viewModel.pomodoroMode.collectAsState()
    val totalSessions by viewModel.pomodoroTotalSessions.collectAsState()

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val progress = remember(timeLeft, mode) {
        val totalSec = if (mode == "Focus") 1500f else 300f
        timeLeft.toFloat() / totalSec
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header back-bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Фокусний Таймер Pomodoro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Circular Clock Canvas Widget
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .background(Color.White.copy(0.04f), CircleShape)
                .border(2.dp, Color.White.copy(0.1f), CircleShape)
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    style = Stroke(width = 8.dp.toPx())
                )
                drawArc(
                    color = if (mode == "Focus") Color(0xFFFF6B6B) else Color(0xFF4ADE80),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mode.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (mode == "Focus") Color(0xFFFF6B6B) else Color(0xFF4ADE80),
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Control Buttons (Play/Pause, Reset)
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.resetPomodoro() },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(0.12f), CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Скинути", tint = Color.White, modifier = Modifier.size(28.dp))
            }

            IconButton(
                onClick = { viewModel.togglePomodoroTimer() },
                modifier = Modifier
                    .size(72.dp)
                    .background(if (isRunning) Color(0xFFFF6B6B) else Color(0xFF4ADE80), CircleShape)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Пауза" else "Старт",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Time Presets Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Швидкі пресети",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "Бліц", 25 to "Робота", 5 to "Перерва").forEach { (min, label) ->
                        Button(
                            onClick = { viewModel.setCustomPomodoroTime(min) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("$min хв\n$label", textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workspace Goal Stats Widget
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.Yellow)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Усього фокус-сесій завершено: $totalSessions",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.9f)
            )
        }
    }
}
