package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.viewmodel.CoachingUiState
import com.example.util.DateUtils
import com.example.util.Formatters
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    uiState: CoachingUiState,
    onChangeDate: (String) -> Unit,
    onMarkAttendance: (studentId: Long, batchId: Long, date: String, status: String) -> Unit,
    onMarkAllPresent: (batchId: Long, date: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val semantic = LocalSemanticColors.current
    val dateIso = uiState.selectedAttendanceDate

    var selectedBatchId by remember {
        mutableStateOf(uiState.batches.firstOrNull()?.batch?.id ?: 0L)
    }
    LaunchedEffect(uiState.batches) {
        if (selectedBatchId == 0L || uiState.batches.none { it.batch.id == selectedBatchId }) {
            selectedBatchId = uiState.batches.firstOrNull()?.batch?.id ?: 0L
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    val batchStudents = remember(uiState.students, selectedBatchId) {
        uiState.students.filter { it.student.isActive && it.student.batchId == selectedBatchId }
    }

    val statusByStudent = remember(uiState.allAttendance, dateIso) {
        uiState.allAttendance
            .filter { it.date == dateIso }
            .associate { it.studentId to it.status }
    }

    val presentCount = batchStudents.count { statusByStudent[it.student.id] == "P" }
    val absentCount = batchStudents.count { statusByStudent[it.student.id] == "A" }
    val leaveCount = batchStudents.count { statusByStudent[it.student.id] == "L" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Date Header + Bulk Mark All Present
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onChangeDate(DateUtils.shiftDateIso(dateIso, -1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Pick date",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = DateUtils.formatDateDisplay(dateIso),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (dateIso == DateUtils.todayIso()) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onChangeDate(DateUtils.shiftDateIso(dateIso, 1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AttCountBadge("P: $presentCount", semantic.successContainer, semantic.onSuccessContainer)
                            AttCountBadge("A: $absentCount", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                            AttCountBadge("L: $leaveCount", semantic.warningContainer, semantic.onWarningContainer)
                        }
                        Button(
                            onClick = {
                                if (selectedBatchId != 0L) {
                                    onMarkAllPresent(selectedBatchId, dateIso)
                                }
                            },
                            enabled = batchStudents.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("mark_all_present_button"),
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark All Present")
                        }
                    }
                }
            }
        }

        // Batch Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.batches.forEach { bs ->
                    FilterChip(
                        selected = selectedBatchId == bs.batch.id,
                        onClick = { selectedBatchId = bs.batch.id },
                        label = { Text("${bs.batch.name} (${bs.studentCount})") },
                    )
                }
            }
        }

        if (batchStudents.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.FactCheck,
                    title = "No Students in This Batch",
                    subtitle = "Enroll students into this batch to start taking daily attendance.",
                )
            }
        } else {
            items(batchStudents, key = { "att_${it.student.id}" }) { item ->
                val currentStatus = statusByStudent[item.student.id]
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_card_${item.student.id}"),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StudentAvatar(
                                name = item.student.name,
                                colorIndex = item.batch?.colorIndex ?: 0,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.student.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Month Att: ${Formatters.formatPercent(item.monthlyAttendancePercent)} (${item.totalPresentThisMonth}/${item.totalMarkedThisMonth})",
                                    style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusTogglePill(
                                    label = "P",
                                    selected = currentStatus == "P",
                                    selectedBg = semantic.success,
                                    selectedFg = Color.White,
                                    onClick = {
                                        onMarkAttendance(item.student.id, item.student.batchId, dateIso, "P")
                                    },
                                    modifier = Modifier.testTag("att_p_${item.student.id}"),
                                )
                                StatusTogglePill(
                                    label = "A",
                                    selected = currentStatus == "A",
                                    selectedBg = MaterialTheme.colorScheme.error,
                                    selectedFg = Color.White,
                                    onClick = {
                                        onMarkAttendance(item.student.id, item.student.batchId, dateIso, "A")
                                    },
                                    modifier = Modifier.testTag("att_a_${item.student.id}"),
                                )
                                StatusTogglePill(
                                    label = "L",
                                    selected = currentStatus == "L",
                                    selectedBg = semantic.warning,
                                    selectedFg = Color.White,
                                    onClick = {
                                        onMarkAttendance(item.student.id, item.student.batchId, dateIso, "L")
                                    },
                                    modifier = Modifier.testTag("att_l_${item.student.id}"),
                                )
                            }
                        }

                        if (currentStatus == "A") {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = {
                                    val msg = WhatsAppHelper.buildAbsentAlertMessage(
                                        studentName = item.student.name,
                                        batchName = item.batch?.name ?: "Batch",
                                        dateDisplay = DateUtils.formatDateDisplay(dateIso),
                                        instituteName = uiState.instituteProfile.name,
                                    )
                                    WhatsAppHelper.openWhatsApp(context, item.student.phone, msg)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("absent_alert_${item.student.id}"),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send WhatsApp Absent Alert to Parent")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(dateIso) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            runCatching { fmt.parse(dateIso)?.time }.getOrNull() ?: System.currentTimeMillis()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onChangeDate(fmt.format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun AttCountBadge(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            style = MonoNumberStyle.copy(
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun StatusTogglePill(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) selectedBg else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) selectedFg else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
