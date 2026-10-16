package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.example.data.model.TaskItem
import com.example.data.model.TaskStatus
import com.example.data.repository.StudySyncRepository
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.backup.BackupExportScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.group.GroupScheduleScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.tasks.TasksScreen
import com.example.ui.theme.StudySyncTheme
import com.example.ui.timer.FocusTimerScreen
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String) {
    DASHBOARD("Ringkasan"),
    SCHEDULE("Jadwal"),
    TASKS("Tugas"),
    TIMER("Fokus"),
    ANALYTICS("Analisis"),
    GROUPS("Grup"),
    SETTINGS("Pengaturan")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.init(this)

        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
            val coroutineScope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(this) }
            val systemDark = isSystemInDarkTheme()
            var userDarkTheme by remember { mutableStateOf(systemDark) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            // Attempt silent auto-sign in if session exists
            LaunchedEffect(Unit) {
                if (currentUser == null) {
                    attemptAutoSignIn(
                        context = this@MainActivity,
                        credentialManager = credentialManager,
                        coroutineScope = coroutineScope,
                        onSuccess = { currentUser = Firebase.auth.currentUser },
                        onUnauthenticated = {}
                    )
                }
            }

            StudySyncTheme(darkTheme = userDarkTheme) {
                if (currentUser == null) {
                    AuthScreen(
                        onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                    )
                } else {
                    StudySyncMainApp(
                        onSignOut = {
                            signOutUser(
                                context = this@MainActivity,
                                credentialManager = credentialManager,
                                coroutineScope = coroutineScope,
                                onSignOutComplete = { currentUser = null }
                            )
                        },
                        isDarkMode = userDarkTheme,
                        onToggleDarkMode = { userDarkTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
fun StudySyncMainApp(
    onSignOut: () -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { StudySyncRepository(context) }

    val userProfile by repository.observeUserProfile().collectAsStateWithLifecycle(initialValue = null)
    val schedules by repository.observeSchedules().collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by repository.observeTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val studySessions by repository.observeStudySessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val groups by repository.observeGroupSchedules().collectAsStateWithLifecycle(initialValue = emptyList())

    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var timerInitialSubject by remember { mutableStateOf<String?>(null) }

    // Seed default balanced student schedule if user database is newly opened and completely empty
    LaunchedEffect(schedules, tasks) {
        if (schedules.isEmpty() && tasks.isEmpty()) {
            val user = Firebase.auth.currentUser
            if (user != null) {
                repository.saveUserProfile(
                    displayName = user.displayName ?: "Mahasiswa",
                    email = user.email,
                    targetMinutes = 180,
                    darkMode = isDarkMode
                )

                // Add starter balanced schedule blocks (Study, Meal, Break, Exercise, Leisure)
                val starterBlocks = listOf(
                    ScheduleBlock(title = "Kuliah Algoritma & Pemrograman", category = ScheduleCategory.STUDY.name, dayOfWeek = 1, startTime = "08:00", endTime = "10:00", locationName = "Gedung A 301"),
                    ScheduleBlock(title = "Istirahat & Minum Air", category = ScheduleCategory.BREAK.name, dayOfWeek = 1, startTime = "10:00", endTime = "10:30", locationName = "Kantin / Selasar"),
                    ScheduleBlock(title = "Kuliah Kalkulus Terapan", category = ScheduleCategory.STUDY.name, dayOfWeek = 1, startTime = "10:30", endTime = "12:00", locationName = "Gedung B 202"),
                    ScheduleBlock(title = "Makan Siang & Ibadah", category = ScheduleCategory.MEAL.name, dayOfWeek = 1, startTime = "12:00", endTime = "13:00", locationName = "Kantin Kampus"),
                    ScheduleBlock(title = "Praktikum Lab Komputer", category = ScheduleCategory.STUDY.name, dayOfWeek = 1, startTime = "13:00", endTime = "15:00", locationName = "Lab Informatika 2"),
                    ScheduleBlock(title = "Olahraga & Peregangan", category = ScheduleCategory.EXERCISE.name, dayOfWeek = 1, startTime = "16:30", endTime = "17:30", locationName = "Lapangan Kampus"),
                    ScheduleBlock(title = "Waktu Senggang & Musik", category = ScheduleCategory.LEISURE.name, dayOfWeek = 1, startTime = "17:30", endTime = "19:00", locationName = "Kamar / Asrama", isAutoAdjustable = true),
                    ScheduleBlock(title = "Belajar Mandiri & Tugas", category = ScheduleCategory.STUDY.name, dayOfWeek = 1, startTime = "19:30", endTime = "21:30", locationName = "Meja Belajar")
                )
                starterBlocks.forEach { repository.createSchedule(it) }

                // Add sample assignments
                repository.createTask(
                    title = "Tugas Pemrograman Grafika",
                    subject = "Informatika",
                    description = "Membuat visualisasi transformasi matriks 3D",
                    dueDate = "2026-10-22",
                    dueTime = "23:59",
                    priority = "HIGH",
                    estimatedMinutes = 120
                )
                repository.createTask(
                    title = "Resume Jurnal Machine Learning",
                    subject = "Kecerdasan Buatan",
                    description = "Membaca dan merangkum jurnal arsitektur transformer",
                    dueDate = "2026-10-24",
                    dueTime = "18:00",
                    priority = "MEDIUM",
                    estimatedMinutes = 90
                )
            }
        }
    }

    // BackHandler: If on secondary tab, return to Dashboard
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        currentTab = ScreenTab.DASHBOARD
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.DASHBOARD,
                    onClick = { currentTab = ScreenTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Ringkasan") },
                    label = { Text("Ringkasan") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.SCHEDULE,
                    onClick = { currentTab = ScreenTab.SCHEDULE },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Jadwal") },
                    label = { Text("Jadwal") },
                    modifier = Modifier.testTag("nav_tab_schedule")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.TASKS,
                    onClick = { currentTab = ScreenTab.TASKS },
                    icon = {
                        val pendingCount = tasks.count { it.status != TaskStatus.COMPLETED.name }
                        if (pendingCount > 0) {
                            BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                Icon(Icons.Default.Assignment, contentDescription = "Tugas")
                            }
                        } else {
                            Icon(Icons.Default.Assignment, contentDescription = "Tugas")
                        }
                    },
                    label = { Text("Tugas") },
                    modifier = Modifier.testTag("nav_tab_tasks")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.TIMER,
                    onClick = { currentTab = ScreenTab.TIMER },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Fokus") },
                    label = { Text("Fokus") },
                    modifier = Modifier.testTag("nav_tab_timer")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.ANALYTICS,
                    onClick = { currentTab = ScreenTab.ANALYTICS },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Analisis") },
                    label = { Text("Analisis") },
                    modifier = Modifier.testTag("nav_tab_analytics")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.GROUPS,
                    onClick = { currentTab = ScreenTab.GROUPS },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Grup") },
                    label = { Text("Grup") },
                    modifier = Modifier.testTag("nav_tab_groups")
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.SETTINGS,
                    onClick = { currentTab = ScreenTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Cadangan") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> {
                    DashboardScreen(
                        userProfile = userProfile,
                        schedules = schedules,
                        tasks = tasks,
                        studySessions = studySessions,
                        onNavigateToTimer = { subj ->
                            timerInitialSubject = subj
                            currentTab = ScreenTab.TIMER
                        },
                        onNavigateToSchedule = { currentTab = ScreenTab.SCHEDULE },
                        onNavigateToTasks = { currentTab = ScreenTab.TASKS },
                        onNavigateToAnalytics = { currentTab = ScreenTab.ANALYTICS },
                        onToggleTaskStatus = { task ->
                            val nextStatus = if (task.status == TaskStatus.COMPLETED.name) TaskStatus.PENDING.name else TaskStatus.COMPLETED.name
                            coroutineScope.launch {
                                repository.updateTask(task.copy(status = nextStatus))
                            }
                        },
                        onTriggerAutoAdjust = {
                            currentTab = ScreenTab.SCHEDULE
                        }
                    )
                }

                ScreenTab.SCHEDULE -> {
                    ScheduleScreen(
                        schedules = schedules,
                        tasks = tasks,
                        onAddSchedule = { newBlock ->
                            coroutineScope.launch {
                                repository.createSchedule(newBlock)
                                Toast.makeText(context, "Jadwal ${newBlock.title} berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSchedule = { id ->
                            coroutineScope.launch {
                                repository.deleteSchedule(id)
                                Toast.makeText(context, "Jadwal dihapus", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAutoAdjust = { urgentTask ->
                            coroutineScope.launch {
                                repository.autoAdjustSchedulesForUrgentTask(urgentTask, schedules)
                            }
                        }
                    )
                }

                ScreenTab.TASKS -> {
                    TasksScreen(
                        tasks = tasks,
                        onAddTask = { newTask ->
                            coroutineScope.launch {
                                repository.createTask(
                                    title = newTask.title,
                                    subject = newTask.subject,
                                    description = newTask.description,
                                    dueDate = newTask.dueDate,
                                    dueTime = newTask.dueTime,
                                    priority = newTask.priority,
                                    estimatedMinutes = newTask.estimatedMinutes
                                )
                                Toast.makeText(context, "Tugas berhasil dibuat", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUpdateTask = { updated ->
                            coroutineScope.launch {
                                repository.updateTask(updated)
                            }
                        },
                        onDeleteTask = { id ->
                            coroutineScope.launch {
                                repository.deleteTask(id)
                                Toast.makeText(context, "Tugas dihapus", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStartFocusSession = { taskId, subject ->
                            timerInitialSubject = subject
                            currentTab = ScreenTab.TIMER
                        }
                    )
                }

                ScreenTab.TIMER -> {
                    FocusTimerScreen(
                        tasks = tasks,
                        initialSubject = timerInitialSubject,
                        onSaveSession = { subject, taskId, durationMinutes, rating, notes ->
                            coroutineScope.launch {
                                repository.logStudySession(
                                    subject = subject,
                                    taskId = taskId,
                                    durationMinutes = durationMinutes,
                                    qualityRating = rating,
                                    notes = notes
                                )
                                NotificationHelper.showNotification(
                                    context = context,
                                    id = 201,
                                    title = "Sesi Selesai!",
                                    message = "Selamat! Kamu menyelesaikan $durationMinutes menit sesi belajar untuk $subject."
                                )
                                Toast.makeText(context, "Sesi belajar berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                ScreenTab.ANALYTICS -> {
                    AnalyticsScreen(
                        studySessions = studySessions,
                        tasks = tasks,
                        schedules = schedules
                    )
                }

                ScreenTab.GROUPS -> {
                    GroupScheduleScreen(
                        groups = groups,
                        onCreateGroup = { name, desc ->
                            coroutineScope.launch {
                                val result = repository.createGroupSchedule(name, desc)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Grup dibuat! Kode: ${result.getOrNull()}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onJoinGroup = { code ->
                            coroutineScope.launch {
                                val result = repository.joinGroupByCode(code)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Berhasil bergabung ke grup: ${result.getOrNull()}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Gagal bergabung", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }

                ScreenTab.SETTINGS -> {
                    BackupExportScreen(
                        userProfile = userProfile,
                        schedules = schedules,
                        tasks = tasks,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onBackupEncrypted = { passphrase, onComplete ->
                            coroutineScope.launch {
                                val res = repository.backupDataEncrypted(passphrase, schedules, tasks)
                                if (res.isSuccess) {
                                    onComplete(true, "Data berhasil dicadangkan ke cloud dengan enkripsi AES-GCM!")
                                } else {
                                    onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "Pencadangan gagal")
                                }
                            }
                        },
                        onRestoreEncrypted = { passphrase, onComplete ->
                            coroutineScope.launch {
                                val res = repository.restoreDataFromEncryptedBackup(passphrase)
                                if (res.isSuccess) {
                                    val (sCount, tCount) = res.getOrThrow()
                                    onComplete(true, "Berhasil memulihkan $sCount jadwal dan $tCount tugas!")
                                } else {
                                    onComplete(false, res.exceptionOrNull()?.localizedMessage ?: "Pemulihan gagal")
                                }
                            }
                        },
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
