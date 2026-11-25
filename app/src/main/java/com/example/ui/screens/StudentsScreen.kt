package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.FeePaymentEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.BatchPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeeStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.viewmodel.CoachingUiState
import com.example.ui.viewmodel.FeeStatus
import com.example.ui.viewmodel.StudentOverview
import com.example.util.DateUtils
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

@Composable
fun StudentsScreen(
    uiState: CoachingUiState,
    selectedProfileStudentId: Long?,
    onSelectProfileStudent: (Long?) -> Unit,
    onAddStudent: () -> Unit,
    onEditStudent: (StudentEntity) -> Unit,
    onDeleteStudent: (StudentEntity) -> Unit,
    onCollectFee: (StudentOverview) -> Unit,
    onViewReceipt: (FeePaymentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileStudent = remember(selectedProfileStudentId, uiState.students) {
        selectedProfileStudentId?.let { id -> uiState.students.find { it.student.id == id } }
    }

    if (profileStudent != null) {
        BackHandler { onSelectProfileStudent(null) }
        StudentProfileDetailView(
            overview = profileStudent,
            uiState = uiState,
            onBack = { onSelectProfileStudent(null) },
            onEdit = { onEditStudent(profileStudent.student) },
            onDelete = {
                onDeleteStudent(profileStudent.student)
                onSelectProfileStudent(null)
            },
            onCollectFee = { onCollectFee(profileStudent) },
            onViewReceipt = onViewReceipt,
            modifier = modifier,
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedBatchId by remember { mutableStateOf<Long?>(null) }
    var selectedFeeFilter by remember { mutableStateOf<FeeStatus?>(null) }

    val filteredStudents = remember(uiState.students, searchQuery, selectedBatchId, selectedFeeFilter) {
        uiState.students.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.student.name.contains(searchQuery, ignoreCase = true) ||
                item.student.fatherName.contains(searchQuery, ignoreCase = true) ||
                item.student.phone.contains(searchQuery, ignoreCase = true)
            val matchesBatch = selectedBatchId == null || item.student.batchId == selectedBatchId
            val matchesStatus = selectedFeeFilter == null || item.feeStatusCurrentMonth == selectedFeeFilter
            matchesQuery && matchesBatch && matchesStatus
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Students Directory",
                subtitle = "${filteredStudents.size} of ${uiState.students.size} students",
                trailing = {
                    Button(
                        onClick = onAddStudent,
                        modifier = Modifier.testTag("add_student_button"),
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Student")
                    }
                },
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by student, father's name, or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_search_input"),
            )
        }

        // Batch & Fee Status Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedBatchId == null,
                        onClick = { selectedBatchId = null },
                        label = { Text("All Batches") },
                    )
                    uiState.batches.forEach { bs ->
                        FilterChip(
                            selected = selectedBatchId == bs.batch.id,
                            onClick = {
                                selectedBatchId = if (selectedBatchId == bs.batch.id) null else bs.batch.id
                            },
                            label = { Text(bs.batch.name) },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedFeeFilter == null,
                        onClick = { selectedFeeFilter = null },
                        label = { Text("All Fees") },
                    )
                    FilterChip(
                        selected = selectedFeeFilter == FeeStatus.PAID,
                        onClick = {
                            selectedFeeFilter = if (selectedFeeFilter == FeeStatus.PAID) null else FeeStatus.PAID
                        },
                        label = { Text("Paid") },
                    )
                    FilterChip(
                        selected = selectedFeeFilter == FeeStatus.PARTIAL,
                        onClick = {
                            selectedFeeFilter = if (selectedFeeFilter == FeeStatus.PARTIAL) null else FeeStatus.PARTIAL
                        },
                        label = { Text("Partial") },
                    )
                    FilterChip(
                        selected = selectedFeeFilter == FeeStatus.UNPAID,
                        onClick = {
                            selectedFeeFilter = if (selectedFeeFilter == FeeStatus.UNPAID) null else FeeStatus.UNPAID
                        },
                        label = { Text("Pending") },
                    )
                }
            }
        }

        if (filteredStudents.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.PersonAdd,
                    title = "No Matching Students",
                    subtitle = "Adjust your filters or enroll a new student into a batch.",
                    actionLabel = "Add Student",
                    onAction = onAddStudent,
                )
            }
        } else {
            items(filteredStudents, key = { it.student.id }) { item ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProfileStudent(item.student.id) }
                        .testTag("student_card_${item.student.id}"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StudentAvatar(
                            name = item.student.name,
                            colorIndex = item.batch?.colorIndex ?: 0,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = item.student.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                FeeStatusBadge(status = item.feeStatusCurrentMonth)
                            }
                            Text(
                                text = "S/O ${item.student.fatherName} • ${item.student.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item.batch?.let { b ->
                                    BatchPill(name = b.name, colorIndex = b.colorIndex)
                                }
                                Text(
                                    text = "Att: ${Formatters.formatPercent(item.monthlyAttendancePercent)}",
                                    style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatInr(item.netMonthlyFee),
                                style = MonoNumberStyle.copy(
                                    fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "/month",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (item.student.monthlyDiscount > 0) {
                                Text(
                                    text = "-${Formatters.formatInr(item.student.monthlyDiscount)} disc",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
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
private fun StudentProfileDetailView(
    overview: StudentOverview,
    uiState: CoachingUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCollectFee: () -> Unit,
    onViewReceipt: (FeePaymentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val semantic = LocalSemanticColors.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val studentPayments = remember(uiState.allPayments, overview.student.id) {
        uiState.allPayments.filter { it.studentId == overview.student.id }
    }
    val studentAttendance = remember(uiState.allAttendance, overview.student.id) {
        uiState.allAttendance.filter { it.studentId == overview.student.id }.take(14)
    }
    val studentTests = remember(uiState.tests, overview.student.id) {
        uiState.tests.mapNotNull { t ->
            val entry = t.rankedEntries.find { it.student.id == overview.student.id }
            if (entry?.marksObtained != null) t to entry else null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to students")
                    }
                    Text(
                        text = "Student Profile",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit student", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete student", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Student Identity Card
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudentAvatar(
                            name = overview.student.name,
                            colorIndex = overview.batch?.colorIndex ?: 0,
                            size = 54.dp,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = overview.student.name,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Father: ${overview.student.fatherName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                overview.batch?.let { b ->
                                    BatchPill(name = b.name, colorIndex = b.colorIndex)
                                }
                                FeeStatusBadge(status = overview.feeStatusCurrentMonth)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ProfileStat("Joined", DateUtils.formatDateDisplay(overview.student.joiningDate))
                        ProfileStat("Net Fee", "${Formatters.formatInr(overview.netMonthlyFee)}/mo")
                        ProfileStat("Month Att.", Formatters.formatPercent(overview.monthlyAttendancePercent))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${overview.student.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call")
                        }
                        FilledTonalButton(
                            onClick = {
                                val msg = if (overview.pendingCurrentMonth > 0) {
                                    WhatsAppHelper.buildFeeReminderMessage(
                                        studentName = overview.student.name,
                                        batchName = overview.batch?.name ?: "Batch",
                                        monthDisplay = DateUtils.formatMonthDisplay(DateUtils.currentMonthKey()),
                                        pendingAmount = overview.pendingCurrentMonth,
                                        instituteName = uiState.instituteProfile.name,
                                    )
                                } else {
                                    "Namaste, update from ${uiState.instituteProfile.name} regarding ${overview.student.name}."
                                }
                                WhatsAppHelper.openWhatsApp(context, overview.student.phone, msg)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = semantic.successContainer,
                                contentColor = semantic.onSuccessContainer,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                        Button(
                            onClick = onCollectFee,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Collect Fee")
                        }
                    }
                }
            }
        }

        // Fee History
        item {
            SectionHeader(
                title = "Fee Payment History",
                subtitle = "${studentPayments.size} recorded payment(s)",
            )
        }
        if (studentPayments.isEmpty()) {
            item {
                Text(
                    text = "No fee payments recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(studentPayments, key = { "prof_pay_${it.id}" }) { payment ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateUtils.formatMonthDisplay(payment.monthKey),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${payment.paymentMode} • ${DateUtils.formatDateDisplay(payment.paidDate)} • ${payment.receiptNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = Formatters.formatInr(payment.amountPaid),
                            style = MonoNumberStyle.copy(fontWeight = FontWeight.Bold),
                            color = semantic.success,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { onViewReceipt(payment) }) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "View Receipt", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Attendance History
        item {
            SectionHeader(
                title = "Recent Attendance Log",
                subtitle = "${overview.totalPresentThisMonth}/${overview.totalMarkedThisMonth} present this month",
            )
        }
        if (studentAttendance.isEmpty()) {
            item {
                Text(
                    text = "No attendance records yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    studentAttendance.forEach { att ->
                        AttendanceChip(att)
                    }
                }
            }
        }

        // Test Performance History
        if (studentTests.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Test Performance",
                    subtitle = "${studentTests.size} test(s) attempted",
                )
            }
            items(studentTests, key = { "prof_test_${it.first.test.id}" }) { (testOverview, ranked) ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(testOverview.test.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                DateUtils.formatDateDisplay(testOverview.test.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${Formatters.formatNumber(ranked.marksObtained ?: 0.0)} / ${Formatters.formatNumber(testOverview.test.totalMarks)}",
                                style = MonoNumberStyle.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                "Rank #${ranked.rank} • ${Formatters.formatPercent(ranked.percentage ?: 0.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Student?") },
            text = { Text("Remove ${overview.student.name} and all their fee/attendance history?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun AttendanceChip(att: AttendanceEntity) {
    val semantic = LocalSemanticColors.current
    val (bg, fg) = when (att.status) {
        "P" -> semantic.successContainer to semantic.onSuccessContainer
        "A" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> semantic.warningContainer to semantic.onWarningContainer
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = att.status,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = fg,
            )
            Text(
                text = att.date.takeLast(5),
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.85f),
            )
        }
    }
}
