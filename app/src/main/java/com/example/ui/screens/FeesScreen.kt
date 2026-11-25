package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.entity.FeePaymentEntity
import com.example.ui.components.BatchPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FeeStatusBadge
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
fun FeesScreen(
    uiState: CoachingUiState,
    onChangeMonth: (String) -> Unit,
    onCollectFee: (StudentOverview, String) -> Unit,
    onViewReceipt: (FeePaymentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val semantic = LocalSemanticColors.current
    val monthKey = uiState.selectedFeeMonthKey

    var statusFilter by remember { mutableStateOf<FeeStatus?>(null) }
    var batchFilterId by remember { mutableStateOf<Long?>(null) }

    val activeStudents = remember(uiState.students) {
        uiState.students.filter { it.student.isActive }
    }

    val expectedMonthTotal = remember(activeStudents, batchFilterId) {
        activeStudents
            .filter { batchFilterId == null || it.student.batchId == batchFilterId }
            .sumOf { it.netMonthlyFee }
    }
    val collectedMonthTotal = remember(activeStudents, batchFilterId, monthKey) {
        activeStudents
            .filter { batchFilterId == null || it.student.batchId == batchFilterId }
            .sumOf { it.paidSelectedMonth }
    }
    val pendingMonthTotal = remember(activeStudents, batchFilterId, monthKey) {
        activeStudents
            .filter { batchFilterId == null || it.student.batchId == batchFilterId }
            .sumOf { it.pendingSelectedMonth }
    }

    val filteredList = remember(activeStudents, statusFilter, batchFilterId, monthKey) {
        activeStudents
            .filter { item ->
                val matchesBatch = batchFilterId == null || item.student.batchId == batchFilterId
                val matchesStatus = when (statusFilter) {
                    null -> true
                    FeeStatus.PAID -> item.feeStatusSelectedMonth == FeeStatus.PAID
                    FeeStatus.PARTIAL -> item.feeStatusSelectedMonth == FeeStatus.PARTIAL
                    FeeStatus.UNPAID -> item.feeStatusSelectedMonth == FeeStatus.UNPAID
                }
                matchesBatch && matchesStatus
            }
            .sortedByDescending { it.pendingSelectedMonth }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Month Switcher + Summary Card
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
                        IconButton(
                            onClick = { onChangeMonth(DateUtils.shiftMonthKey(monthKey, -1)) },
                            modifier = Modifier.testTag("prev_fee_month_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = DateUtils.formatMonthDisplay(monthKey),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Monthly Fee Register",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { onChangeMonth(DateUtils.shiftMonthKey(monthKey, 1)) },
                            modifier = Modifier.testTag("next_fee_month_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val ratio = if (expectedMonthTotal > 0) {
                        (collectedMonthTotal / expectedMonthTotal).toFloat().coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = semantic.success,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        FeeStat("Expected", Formatters.formatInr(expectedMonthTotal), MaterialTheme.colorScheme.onSurface)
                        FeeStat("Collected", Formatters.formatInr(collectedMonthTotal), semantic.success)
                        FeeStat("Pending", Formatters.formatInr(pendingMonthTotal), MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All (${activeStudents.size})") },
                    )
                    FilterChip(
                        selected = statusFilter == FeeStatus.UNPAID,
                        onClick = {
                            statusFilter = if (statusFilter == FeeStatus.UNPAID) null else FeeStatus.UNPAID
                        },
                        label = { Text("Unpaid") },
                    )
                    FilterChip(
                        selected = statusFilter == FeeStatus.PARTIAL,
                        onClick = {
                            statusFilter = if (statusFilter == FeeStatus.PARTIAL) null else FeeStatus.PARTIAL
                        },
                        label = { Text("Partial") },
                    )
                    FilterChip(
                        selected = statusFilter == FeeStatus.PAID,
                        onClick = {
                            statusFilter = if (statusFilter == FeeStatus.PAID) null else FeeStatus.PAID
                        },
                        label = { Text("Paid") },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = batchFilterId == null,
                        onClick = { batchFilterId = null },
                        label = { Text("All Batches") },
                    )
                    uiState.batches.forEach { bs ->
                        FilterChip(
                            selected = batchFilterId == bs.batch.id,
                            onClick = {
                                batchFilterId = if (batchFilterId == bs.batch.id) null else bs.batch.id
                            },
                            label = { Text(bs.batch.name) },
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "No Fee Records Match Filter",
                    subtitle = "Switch filters or add students to track monthly fee collections.",
                )
            }
        } else {
            items(filteredList, key = { "fee_${it.student.id}" }) { item ->
                val latestPayment = remember(uiState.allPayments, item.student.id, monthKey) {
                    uiState.allPayments.firstOrNull {
                        it.studentId == item.student.id && it.monthKey == monthKey
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fee_card_${item.student.id}"),
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
                                    FeeStatusBadge(status = item.feeStatusSelectedMonth)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                item.batch?.let { b ->
                                    BatchPill(name = b.name, colorIndex = b.colorIndex)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatInr(item.netMonthlyFee),
                                    style = MonoNumberStyle.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "Paid: ${Formatters.formatInr(item.paidSelectedMonth)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.success,
                                )
                                if (item.pendingSelectedMonth > 0) {
                                    Text(
                                        text = "Due: ${Formatters.formatInr(item.pendingSelectedMonth)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (item.pendingSelectedMonth > 0) {
                                FilledTonalButton(
                                    onClick = {
                                        val msg = WhatsAppHelper.buildFeeReminderMessage(
                                            studentName = item.student.name,
                                            batchName = item.batch?.name ?: "Batch",
                                            monthDisplay = DateUtils.formatMonthDisplay(monthKey),
                                            pendingAmount = item.pendingSelectedMonth,
                                            instituteName = uiState.instituteProfile.name,
                                        )
                                        WhatsAppHelper.openWhatsApp(context, item.student.phone, msg)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = semantic.successContainer,
                                        contentColor = semantic.onSuccessContainer,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("whatsapp_reminder_${item.student.id}"),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp Reminder", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Button(
                                    onClick = { onCollectFee(item, monthKey) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("collect_fee_${item.student.id}"),
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Collect Fee", maxLines = 1)
                                }
                            } else {
                                if (latestPayment != null) {
                                    OutlinedButton(
                                        onClick = { onViewReceipt(latestPayment) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("view_receipt_${item.student.id}"),
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View / Print Receipt")
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { onCollectFee(item, monthKey) },
                                    modifier = Modifier.weight(0.8f),
                                ) {
                                    Text("Add Payment")
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
private fun FeeStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MonoNumberStyle.copy(
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = color,
        )
    }
}
