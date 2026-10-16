package com.example.ui.schedule

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.example.data.model.TaskItem
import com.example.util.CalendarExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    schedules: List<ScheduleBlock>,
    tasks: List<TaskItem>,
    onAddSchedule: (ScheduleBlock) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onAutoAdjust: (TaskItem) -> Unit
) {
    val context = LocalContext.current
    var selectedDay by remember { mutableIntStateOf(1) } // 1=Senin .. 7=Minggu
    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAutoAdjustDialog by remember { mutableStateOf(false) }

    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    val filteredSchedules = remember(schedules, selectedDay, selectedCategoryFilter) {
        schedules.filter { it.dayOfWeek == selectedDay }
            .filter { selectedCategoryFilter == "ALL" || it.category == selectedCategoryFilter }
            .sortedBy { it.startTime }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .testTag("add_schedule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Jadwal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kalender & Jadwal Harian",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Rencana seimbang belajar, istirahat, makan & olahraga",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = { showAutoAdjustDialog = true },
                        modifier = Modifier.testTag("open_auto_adjust_dialog")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Penyesuaian Otomatis",
                            tint = Color(0xFFA855F7)
                        )
                    }
                }
            }

            // Day Selector Chips (Senin - Minggu)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days.indices.toList()) { index ->
                    val dayNum = index + 1
                    val isSelected = selectedDay == dayNum
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { selectedDay = dayNum }
                            .testTag("day_chip_$dayNum")
                    ) {
                        Text(
                            text = days[index],
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryChip(
                        label = "Semua",
                        isSelected = selectedCategoryFilter == "ALL",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { selectedCategoryFilter = "ALL" }
                    )
                }
                items(ScheduleCategory.values()) { cat ->
                    val catColor = when (cat) {
                        ScheduleCategory.STUDY -> Color(0xFF3B82F6)
                        ScheduleCategory.BREAK -> Color(0xFF10B981)
                        ScheduleCategory.MEAL -> Color(0xFFF59E0B)
                        ScheduleCategory.EXERCISE -> Color(0xFFEF4444)
                        ScheduleCategory.LEISURE -> Color(0xFFA855F7)
                    }
                    CategoryChip(
                        label = cat.displayName,
                        isSelected = selectedCategoryFilter == cat.name,
                        color = catColor,
                        onClick = { selectedCategoryFilter = cat.name }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Schedule List
            if (filteredSchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada jadwal di ${days[selectedDay - 1]}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tambah Aktivitas")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSchedules) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onSyncGoogle = {
                                CalendarExporter.addToGoogleCalendar(context, schedule)
                                Toast.makeText(context, "Membuka Google Calendar...", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { onDeleteSchedule(schedule.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Schedule Dialog
    if (showAddDialog) {
        AddScheduleDialog(
            defaultDay = selectedDay,
            onDismiss = { showAddDialog = false },
            onConfirm = { newBlock ->
                onAddSchedule(newBlock)
                showAddDialog = false
            }
        )
    }

    // Auto-Adjust Schedule Dialog (Penyesuaian Otomatis)
    if (showAutoAdjustDialog) {
        AutoAdjustDialog(
            tasks = tasks,
            onDismiss = { showAutoAdjustDialog = false },
            onSelectUrgentTask = { urgentTask ->
                onAutoAdjust(urgentTask)
                showAutoAdjustDialog = false
                Toast.makeText(context, "Jadwal berhasil disesuaikan untuk tugas: ${urgentTask.title}", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ScheduleCard(
    schedule: ScheduleBlock,
    onSyncGoogle: () -> Unit,
    onDelete: () -> Unit
) {
    val (catColor, catIcon) = when (schedule.category) {
        ScheduleCategory.STUDY.name -> Pair(Color(0xFF3B82F6), Icons.Default.School)
        ScheduleCategory.BREAK.name -> Pair(Color(0xFF10B981), Icons.Default.Spa)
        ScheduleCategory.MEAL.name -> Pair(Color(0xFFF59E0B), Icons.Default.Restaurant)
        ScheduleCategory.EXERCISE.name -> Pair(Color(0xFFEF4444), Icons.Default.FitnessCenter)
        else -> Pair(Color(0xFFA855F7), Icons.Default.SelfImprovement)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = catColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime} • ${schedule.category}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = catColor
                )
                if (!schedule.locationName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = schedule.locationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onSyncGoogle) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sinkron Google Calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Jadwal",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    defaultDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleBlock) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ScheduleCategory.STUDY) }
    var dayOfWeek by remember { mutableIntStateOf(defaultDay) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:30") }
    var locationName by remember { mutableStateOf("") }
    var isAutoAdjustable by remember { mutableStateOf(true) }

    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jadwal Harian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Aktivitas / Mata Kuliah") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori Jadwal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ScheduleCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Mulai (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Selesai (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Lokasi (Kampus, Lab, Gym, Rumah)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isAutoAdjustable = !isAutoAdjustable }
                ) {
                    Checkbox(
                        checked = isAutoAdjustable,
                        onCheckedChange = { isAutoAdjustable = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Izinkan penyesuaian otomatis jika ada perubahan",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            ScheduleBlock(
                                title = title.trim(),
                                category = selectedCategory.name,
                                dayOfWeek = dayOfWeek,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                locationName = locationName.trim().ifEmpty { null },
                                isAutoAdjustable = isAutoAdjustable
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AutoAdjustDialog(
    tasks: List<TaskItem>,
    onDismiss: () -> Unit,
    onSelectUrgentTask: (TaskItem) -> Unit
) {
    val pendingTasks = tasks.filter { it.status != "COMPLETED" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Penyesuaian Jadwal Otomatis")
            }
        },
        text = {
            Column {
                Text(
                    text = "Jika ada perubahan jadwal kuliah atau tugas mendadak, StudySync akan otomatis mengoptimalkan waktu senggang & istirahat menjadi sesi fokus belajar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Pilih tugas yang mendesak untuk dijadwalkan:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (pendingTasks.isEmpty()) {
                    Text(
                        text = "Tidak ada tugas mendesak saat ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(pendingTasks) { task ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectUrgentTask(task) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = task.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = "${task.subject} • ${task.priority}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "Sesuaikan",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
