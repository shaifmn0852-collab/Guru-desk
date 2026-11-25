package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BatchPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudentAvatar
import com.example.ui.components.batchAccentColor
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.viewmodel.BatchDistributionSlice
import com.example.ui.viewmodel.CoachingUiState
import com.example.ui.viewmodel.MonthlyCollectionPoint
import com.example.ui.viewmodel.StudentOverview
import com.example.util.DateUtils
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

@Composable
fun DashboardScreen(
    uiState: CoachingUiState,
    onAddStudentClick: () -> Unit,
    onRecordFeeClick: () -> Unit,
    onMarkAttendanceClick: () -> Unit,
    onCollectStudentFee: (StudentOverview) -> Unit,
    onOpenStudentProfile: (StudentOverview) -> Unit,
    onViewAllFees: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val semantic = LocalSemanticColors.current
    val pendingStudents = remember(uiState.students) {
        uiState.students
            .filter { it.student.isActive && it.pendingCurrentMonth > 0.0 }
            .sortedByDescending { it.pendingCurrentMonth }
            .take(4)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Editorial Hero Card
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_coaching_banner),
                        contentDescription = "Coaching institute banner illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(if (semantic.isDark) 0.14f else 0.28f),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                    )
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = DateUtils.formatDateDisplay(DateUtils.todayIso()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.instituteProfile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.batches.size} Active Batches • ${DateUtils.formatMonthDisplay(DateUtils.currentMonthKey())} Cycle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onAddStudentClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_add_student_button"),
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Student", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FilledTonalButton(
                                onClick = onRecordFeeClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_record_fee_button"),
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Fee", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = onMarkAttendanceClick,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_mark_attendance_button"),
                            ) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attend", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // 4 KPI Summary Cards (Adaptive 2x2 or 4-col)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp
                val attLabel = uiState.todayAttendancePercent?.let { Formatters.formatPercent(it) } ?: "Not Marked"
                val attSub = if (uiState.todayTotalMarked > 0) {
                    "${uiState.todayPresentCount}/${uiState.todayTotalMarked} present today"
                } else {
                    "Tap Attend to mark today"
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        KpiSummaryCard(
                            title = "Active Students",
                            value = uiState.totalActiveStudents.toString(),
                            subtitle = "Across ${uiState.batches.size} batches",
                            icon = Icons.Default.Groups,
                            accentColor = MaterialTheme.colorScheme.primary,
                            isAlert = false,
                            modifier = Modifier.weight(1f),
                        )
                        KpiSummaryCard(
                            title = "Collected (${DateUtils.formatMonthShort(DateUtils.currentMonthKey())})",
                            value = Formatters.formatInr(uiState.collectedThisMonth),
                            subtitle = "Monthly fee inflow",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = semantic.success,
                            isAlert = false,
                            modifier = Modifier.weight(1f),
                        )
                        KpiSummaryCard(
                            title = "Pending Fee Dues",
                            value = Formatters.formatInr(uiState.pendingDuesThisMonth),
                            subtitle = "${uiState.pendingStudentsCount} students due",
                            icon = Icons.Default.WarningAmber,
                            accentColor = MaterialTheme.colorScheme.error,
                            isAlert = uiState.pendingDuesThisMonth > 0.0,
                            onClick = onViewAllFees,
                            modifier = Modifier.weight(1f),
                        )
                        KpiSummaryCard(
                            title = "Today's Attendance",
                            value = attLabel,
                            subtitle = attSub,
                            icon = Icons.Default.CheckCircle,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            isAlert = false,
                            onClick = onMarkAttendanceClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            KpiSummaryCard(
                                title = "Active Students",
                                value = uiState.totalActiveStudents.toString(),
                                subtitle = "Across ${uiState.batches.size} batches",
                                icon = Icons.Default.Groups,
                                accentColor = MaterialTheme.colorScheme.primary,
                                isAlert = false,
                                modifier = Modifier.weight(1f),
                            )
                            KpiSummaryCard(
                                title = "Collected (${DateUtils.formatMonthShort(DateUtils.currentMonthKey())})",
                                value = Formatters.formatInr(uiState.collectedThisMonth),
                                subtitle = "Monthly fee inflow",
                                icon = Icons.Default.AccountBalanceWallet,
                                accentColor = semantic.success,
                                isAlert = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            KpiSummaryCard(
                                title = "Pending Fee Dues",
                                value = Formatters.formatInr(uiState.pendingDuesThisMonth),
                                subtitle = "${uiState.pendingStudentsCount} students due",
                                icon = Icons.Default.WarningAmber,
                                accentColor = MaterialTheme.colorScheme.error,
                                isAlert = uiState.pendingDuesThisMonth > 0.0,
                                onClick = onViewAllFees,
                                modifier = Modifier.weight(1f),
                            )
                            KpiSummaryCard(
                                title = "Today's Attendance",
                                value = attLabel,
                                subtitle = attSub,
                                icon = Icons.Default.CheckCircle,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                isAlert = false,
                                onClick = onMarkAttendanceClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // Charts Section (Adaptive side-by-side on wide screens)
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        MonthlyCollectionChartCard(
                            points = uiState.monthlyTrend,
                            modifier = Modifier.weight(1.25f),
                        )
                        BatchDistributionCard(
                            slices = uiState.batchDistribution,
                            totalStudents = uiState.totalActiveStudents,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MonthlyCollectionChartCard(points = uiState.monthlyTrend)
                        BatchDistributionCard(
                            slices = uiState.batchDistribution,
                            totalStudents = uiState.totalActiveStudents,
                        )
                    }
                }
            }
        }

        // Pending Fee Dues Action List
        if (pendingStudents.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Pending Fee Alerts",
                    subtitle = "Send a 1-tap WhatsApp reminder or record payment",
                    trailing = {
                        TextButton(onClick = onViewAllFees) {
                            Text("View All")
                        }
                    },
                )
            }
            items(pendingStudents, key = { "dash_pending_${it.student.id}" }) { item ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenStudentProfile(item) },
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
                            Text(
                                text = item.student.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            item.batch?.let { b ->
                                BatchPill(name = b.name, colorIndex = b.colorIndex)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Due: ${Formatters.formatInr(item.pendingCurrentMonth)}",
                                style = MonoNumberStyle.copy(
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val msg = WhatsAppHelper.buildFeeReminderMessage(
                                        studentName = item.student.name,
                                        batchName = item.batch?.name ?: "Coaching Batch",
                                        monthDisplay = DateUtils.formatMonthDisplay(DateUtils.currentMonthKey()),
                                        pendingAmount = item.pendingCurrentMonth,
                                        instituteName = uiState.instituteProfile.name,
                                    )
                                    WhatsAppHelper.openWhatsApp(context, item.student.phone, msg)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = semantic.successContainer,
                                    contentColor = semantic.onSuccessContainer,
                                ),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "WhatsApp reminder",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Button(
                                onClick = { onCollectStudentFee(item) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text("Collect")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isAlert: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isAlert) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isAlert) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (isAlert) 0.dp else 1.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MonoNumberStyle.copy(
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonthlyCollectionChartCard(
    points: List<MonthlyCollectionPoint>,
    modifier: Modifier = Modifier,
) {
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(points) { animateTrigger = true }
    val progress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chart_anim",
    )

    val maxVal = (points.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1000.0)
    val barColor = MaterialTheme.colorScheme.primary
    val activeBarColor = MaterialTheme.colorScheme.secondary
    val lineColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Monthly Fee Collection",
                subtitle = "Last 6 months trend (₹)",
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (points.isEmpty()) {
                Text(
                    text = "No fee data yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    val count = points.size
                    val topPad = 14.dp.toPx()
                    val bottomPad = 8.dp.toPx()
                    val chartH = size.height - topPad - bottomPad
                    val slotW = size.width / count
                    val barW = (slotW * 0.48f).coerceAtMost(36.dp.toPx())

                    // Subtle horizontal grid lines
                    for (step in 0..2) {
                        val y = topPad + chartH * (step / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val linePath = Path()
                    points.forEachIndexed { index, pt ->
                        val ratio = ((pt.amount / maxVal).toFloat()).coerceIn(0f, 1f) * progress
                        val barH = (chartH * ratio).coerceAtLeast(4.dp.toPx())
                        val centerX = slotW * index + slotW / 2f
                        val left = centerX - barW / 2f
                        val top = topPad + (chartH - barH)

                        drawRoundRect(
                            color = if (index == count - 1) activeBarColor else barColor.copy(alpha = 0.82f),
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        )

                        if (index == 0) {
                            linePath.moveTo(centerX, top)
                        } else {
                            linePath.lineTo(centerX, top)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    points.forEach { pt ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = Formatters.formatInrCompact(pt.amount),
                                style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchDistributionCard(
    slices: List<BatchDistributionSlice>,
    totalStudents: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Batch-wise Distribution",
                subtitle = "$totalStudents active students",
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (slices.isEmpty() || totalStudents == 0) {
                Text(
                    text = "Add batches and students to see distribution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val colors = slices.map { batchAccentColor(it.colorIndex) }
                // Segmented Stacked Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    var startX = 0f
                    val gap = 2.dp.toPx()
                    val totalGap = gap * (slices.count { it.studentCount > 0 } - 1).coerceAtLeast(0)
                    val usableWidth = (size.width - totalGap).coerceAtLeast(1f)
                    slices.forEachIndexed { idx, slice ->
                        if (slice.studentCount > 0) {
                            val w = usableWidth * slice.fraction
                            drawRect(
                                color = colors[idx],
                                topLeft = Offset(startX, 0f),
                                size = Size(w, size.height),
                            )
                            startX += w + gap
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slices.forEachIndexed { idx, slice ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors[idx])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = slice.batchName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${slice.studentCount} (${Formatters.formatPercent(slice.fraction * 100.0)})",
                                style = MonoNumberStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
