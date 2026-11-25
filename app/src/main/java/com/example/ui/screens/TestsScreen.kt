package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TestEntity
import com.example.ui.components.BatchPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.LocalSemanticColors
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.viewmodel.CoachingUiState
import com.example.ui.viewmodel.TestOverview
import com.example.util.DateUtils
import com.example.util.Formatters

@Composable
fun TestsScreen(
    uiState: CoachingUiState,
    onCreateTest: () -> Unit,
    onDeleteTest: (TestEntity) -> Unit,
    onSaveMarks: (testId: Long, marksByStudent: Map<Long, Double>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTestId by remember { mutableStateOf<Long?>(null) }
    var batchFilterId by remember { mutableStateOf<Long?>(null) }
    var testToDelete by remember { mutableStateOf<TestOverview?>(null) }

    val selectedTest = remember(selectedTestId, uiState.tests) {
        selectedTestId?.let { id -> uiState.tests.find { it.test.id == id } }
    }

    if (selectedTest != null) {
        BackHandler { selectedTestId = null }
        TestMarksDetailView(
            testOverview = selectedTest,
            onBack = { selectedTestId = null },
            onSaveMarks = { map -> onSaveMarks(selectedTest.test.id, map) },
            modifier = modifier,
        )
        return
    }

    val filteredTests = remember(uiState.tests, batchFilterId) {
        uiState.tests.filter { batchFilterId == null || it.test.batchId == batchFilterId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "Tests & Marks Record",
                subtitle = "${filteredTests.size} test(s) recorded",
                trailing = {
                    Button(
                        onClick = onCreateTest,
                        modifier = Modifier.testTag("create_test_button"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Test")
                    }
                },
            )
        }

        item {
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

        if (filteredTests.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Quiz,
                    title = "No Tests Scheduled",
                    subtitle = "Create a test for any batch to record student marks, percentages, and ranks.",
                    actionLabel = "Create Test",
                    onAction = onCreateTest,
                )
            }
        } else {
            items(filteredTests, key = { it.test.id }) { item ->
                val topPerformer = item.rankedEntries.firstOrNull { it.marksObtained != null }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTestId = item.test.id }
                        .testTag("test_card_${item.test.id}"),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.test.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    item.batch?.let { b ->
                                        BatchPill(name = b.name, colorIndex = b.colorIndex)
                                    }
                                    Text(
                                        text = DateUtils.formatDateDisplay(item.test.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { testToDelete = item }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete test",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Max Marks: ${Formatters.formatNumber(item.test.totalMarks)} • Graded: ${item.enteredCount}/${item.totalStudentsInBatch}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Avg: ${Formatters.formatPercent(item.averagePercent)}",
                                style = MonoNumberStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (topPerformer != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Topper: ${topPerformer.student.name} (${Formatters.formatNumber(topPerformer.marksObtained ?: 0.0)}/${Formatters.formatNumber(item.test.totalMarks)} • ${Formatters.formatPercent(topPerformer.percentage ?: 0.0)})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    testToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { testToDelete = null },
            title = { Text("Delete Test?") },
            text = { Text("Delete \"${target.test.name}\" and all recorded marks?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTest(target.test)
                        testToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { testToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TestMarksDetailView(
    testOverview: TestOverview,
    onBack: () -> Unit,
    onSaveMarks: (Map<Long, Double>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalSemanticColors.current
    val maxMarks = testOverview.test.totalMarks

    val editableMarks = remember(testOverview.test.id, testOverview.rankedEntries) {
        mutableStateMapOf<Long, String>().apply {
            testOverview.rankedEntries.forEach { entry ->
                put(
                    entry.student.id,
                    entry.marksObtained?.let { Formatters.formatNumber(it) } ?: ""
                )
            }
        }
    }

    val topThree = remember(testOverview.rankedEntries) {
        testOverview.rankedEntries.filter { it.marksObtained != null }.take(3)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to tests")
                    }
                    Column {
                        Text(
                            text = testOverview.test.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${testOverview.batch?.name ?: ""} • Max ${Formatters.formatNumber(maxMarks)} • ${DateUtils.formatDateDisplay(testOverview.test.date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = {
                        val parsed = mutableMapOf<Long, Double>()
                        editableMarks.forEach { (sid, text) ->
                            val v = text.toDoubleOrNull()
                            if (v != null && v in 0.0..maxMarks) {
                                parsed[sid] = v
                            }
                        }
                        onSaveMarks(parsed)
                    },
                    modifier = Modifier.testTag("save_test_marks_button"),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }

        // Top Performers Podium
        if (topThree.isNotEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SectionHeader(
                            title = "Top Performers",
                            subtitle = "Auto-ranked by marks obtained",
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            topThree.forEach { entry ->
                                val medalColor = when (entry.rank) {
                                    1 -> Color(0xFFD97706)
                                    2 -> Color(0xFF64748B)
                                    else -> Color(0xFFB45309)
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = medalColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, medalColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = medalColor,
                                            modifier = Modifier.size(26.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${entry.rank}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = entry.student.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = Formatters.formatPercent(entry.percentage ?: 0.0),
                                            style = MonoNumberStyle.copy(
                                                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = medalColor,
                                        )
                                        Text(
                                            text = "${Formatters.formatNumber(entry.marksObtained ?: 0.0)}/${Formatters.formatNumber(maxMarks)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Enter / Update Marks",
                subtitle = "Enter scores out of ${Formatters.formatNumber(maxMarks)} and tap Save",
            )
        }

        items(testOverview.rankedEntries, key = { "mark_${it.student.id}" }) { entry ->
            val currentText = editableMarks[entry.student.id] ?: ""
            val liveVal = currentText.toDoubleOrNull()
            val livePct = if (liveVal != null && maxMarks > 0 && liveVal <= maxMarks) {
                (liveVal * 100.0 / maxMarks)
            } else null

            Surface(
                shape = MaterialTheme.shapes.medium,
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
                    StudentAvatar(
                        name = entry.student.name,
                        colorIndex = testOverview.batch?.colorIndex ?: 0,
                        size = 38.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.student.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val rankLabel = if (entry.rank > 0) "Rank #${entry.rank}" else "Not graded"
                        val pctLabel = livePct?.let { Formatters.formatPercent(it) } ?: "--"
                        Text(
                            text = "$rankLabel • $pctLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = if ((livePct ?: 0.0) >= 75.0) semantic.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            val num = filtered.toDoubleOrNull()
                            if (num == null || num <= maxMarks) {
                                editableMarks[entry.student.id] = filtered
                            }
                        },
                        label = { Text("Marks") },
                        suffix = { Text("/${Formatters.formatNumber(maxMarks)}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .width(126.dp)
                            .testTag("marks_input_${entry.student.id}"),
                    )
                }
            }
        }
    }
}
