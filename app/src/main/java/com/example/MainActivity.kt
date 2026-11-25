package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ThemePreference
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.BatchFormDialog
import com.example.ui.components.CollectFeeDialog
import com.example.ui.components.FeeReceiptDialog
import com.example.ui.components.InstituteEmblemBadge
import com.example.ui.components.StudentAvatar
import com.example.ui.components.StudentFormDialog
import com.example.ui.components.TestFormDialog
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BatchesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FeesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.screens.TestsScreen
import com.example.ui.theme.GuruDeskTheme
import com.example.ui.theme.MonoNumberStyle
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.CoachingUiState
import com.example.ui.viewmodel.CoachingViewModel
import com.example.ui.viewmodel.StudentOverview
import com.example.ui.viewmodel.UiEvent
import com.example.util.DateUtils
import com.example.util.Formatters
import com.example.util.ReceiptData
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CoachingViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isDark = when (uiState.themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            GuruDeskTheme(darkTheme = isDark) {
                GuruDeskApp(
                    viewModel = viewModel,
                    uiState = uiState,
                    isDark = isDark,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuruDeskApp(
    viewModel: CoachingViewModel,
    uiState: CoachingUiState,
    isDark: Boolean,
) {
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var selectedStudentProfileId by remember { mutableStateOf<Long?>(null) }

    // Modal states
    var showBatchDialog by remember { mutableStateOf<BatchEntity?>(null) }
    var isCreatingBatch by remember { mutableStateOf(false) }

    var showStudentDialog by remember { mutableStateOf<StudentEntity?>(null) }
    var isCreatingStudent by remember { mutableStateOf(false) }

    var showFeePickerDialog by remember { mutableStateOf(false) }
    var collectFeeTarget by remember { mutableStateOf<Pair<StudentOverview, String>?>(null) }
    var activeReceipt by remember { mutableStateOf<ReceiptData?>(null) }
    var showCreateTestDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.OpenReceipt -> {
                    activeReceipt = event.receipt
                }
            }
        }
    }

    if (currentDestination != AppDestination.DASHBOARD) {
        BackHandler {
            currentDestination = AppDestination.DASHBOARD
            selectedStudentProfileId = null
        }
    }

    fun navigateTo(dest: AppDestination) {
        currentDestination = dest
        if (dest != AppDestination.STUDENTS) {
            selectedStudentProfileId = null
        }
        scope.launch { drawerState.close() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        val mainScaffold: @Composable () -> Unit = {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        navigationIcon = {
                            if (!isExpandedScreen) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_sidebar_button"),
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open navigation sidebar")
                                }
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (!isExpandedScreen) {
                                    InstituteEmblemBadge(
                                        logoText = uiState.instituteProfile.logoText,
                                        size = 32.dp,
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentDestination.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = uiState.instituteProfile.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleDarkLightMode() },
                                modifier = Modifier.testTag("toggle_theme_button"),
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
                                )
                            }
                            if (!isExpandedScreen && currentDestination != AppDestination.SETTINGS) {
                                IconButton(
                                    onClick = { navigateTo(AppDestination.SETTINGS) },
                                    modifier = Modifier.testTag("topbar_settings_button"),
                                ) {
                                    Icon(Icons.Outlined.Settings, contentDescription = "Settings & Backup")
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        val bottomItems = listOf(
                            AppDestination.DASHBOARD,
                            AppDestination.STUDENTS,
                            AppDestination.FEES,
                            AppDestination.ATTENDANCE,
                            AppDestination.TESTS,
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            bottomItems.forEach { dest ->
                                val selected = currentDestination == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest) },
                                    icon = {
                                        Icon(
                                            imageVector = destinationIcon(dest, selected),
                                            contentDescription = dest.title,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.shortLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    modifier = Modifier.testTag("bottom_nav_${dest.name.lowercase()}"),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Crossfade(
                        targetState = currentDestination,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1100.dp),
                        label = "destination_crossfade",
                    ) { dest ->
                        when (dest) {
                            AppDestination.DASHBOARD -> DashboardScreen(
                                uiState = uiState,
                                onAddStudentClick = { isCreatingStudent = true },
                                onRecordFeeClick = { showFeePickerDialog = true },
                                onMarkAttendanceClick = { navigateTo(AppDestination.ATTENDANCE) },
                                onCollectStudentFee = { student ->
                                    collectFeeTarget = student to DateUtils.currentMonthKey()
                                },
                                onOpenStudentProfile = { student ->
                                    selectedStudentProfileId = student.student.id
                                    currentDestination = AppDestination.STUDENTS
                                },
                                onViewAllFees = { navigateTo(AppDestination.FEES) },
                            )
                            AppDestination.BATCHES -> BatchesScreen(
                                batches = uiState.batches,
                                onAddBatch = { isCreatingBatch = true },
                                onEditBatch = { batch -> showBatchDialog = batch },
                                onDeleteBatch = { batch -> viewModel.deleteBatch(batch) },
                            )
                            AppDestination.STUDENTS -> StudentsScreen(
                                uiState = uiState,
                                selectedProfileStudentId = selectedStudentProfileId,
                                onSelectProfileStudent = { id -> selectedStudentProfileId = id },
                                onAddStudent = { isCreatingStudent = true },
                                onEditStudent = { student -> showStudentDialog = student },
                                onDeleteStudent = { student -> viewModel.deleteStudent(student) },
                                onCollectFee = { student ->
                                    collectFeeTarget = student to DateUtils.currentMonthKey()
                                },
                                onViewReceipt = { payment -> viewModel.openReceiptForPayment(payment) },
                            )
                            AppDestination.FEES -> FeesScreen(
                                uiState = uiState,
                                onChangeMonth = { mk -> viewModel.setFeeMonthKey(mk) },
                                onCollectFee = { student, mk -> collectFeeTarget = student to mk },
                                onViewReceipt = { payment -> viewModel.openReceiptForPayment(payment) },
                            )
                            AppDestination.ATTENDANCE -> AttendanceScreen(
                                uiState = uiState,
                                onChangeDate = { d -> viewModel.setAttendanceDate(d) },
                                onMarkAttendance = { sid, bid, d, status ->
                                    viewModel.markAttendance(sid, bid, d, status)
                                },
                                onMarkAllPresent = { bid, d ->
                                    viewModel.markAllPresent(bid, d)
                                },
                            )
                            AppDestination.TESTS -> TestsScreen(
                                uiState = uiState,
                                onCreateTest = { showCreateTestDialog = true },
                                onDeleteTest = { test -> viewModel.deleteTest(test) },
                                onSaveMarks = { tid, marks -> viewModel.saveTestMarks(tid, marks) },
                            )
                            AppDestination.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onSelectTheme = { theme -> viewModel.setThemePreference(theme) },
                                onSaveInstituteProfile = { profile -> viewModel.updateInstituteProfile(profile) },
                                onExportJson = { viewModel.generateJsonBackup() },
                                onExportCsv = { viewModel.generateCsvReport() },
                                onImportJson = { json -> viewModel.restoreJsonBackup(json) },
                                onResetDemoData = { viewModel.resetDemoData() },
                                onNotify = { msg -> viewModel.emitMessage(msg) },
                            )
                        }
                    }
                }
            }
        }

        if (isExpandedScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(268.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        SidebarDrawerContent(
                            uiState = uiState,
                            currentDestination = currentDestination,
                            onSelectDestination = { navigateTo(it) },
                        )
                    }
                },
                content = mainScaffold,
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(288.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        SidebarDrawerContent(
                            uiState = uiState,
                            currentDestination = currentDestination,
                            onSelectDestination = { navigateTo(it) },
                        )
                    }
                },
                content = mainScaffold,
            )
        }
    }

    // Dialogs
    if (isCreatingBatch || showBatchDialog != null) {
        BatchFormDialog(
            initialBatch = showBatchDialog,
            onDismiss = {
                isCreatingBatch = false
                showBatchDialog = null
            },
            onSave = { batch ->
                viewModel.saveBatch(batch)
                isCreatingBatch = false
                showBatchDialog = null
            },
        )
    }

    if (isCreatingStudent || showStudentDialog != null) {
        StudentFormDialog(
            batches = uiState.batches.map { it.batch },
            initialStudent = showStudentDialog,
            onDismiss = {
                isCreatingStudent = false
                showStudentDialog = null
            },
            onSave = { student ->
                viewModel.saveStudent(student)
                isCreatingStudent = false
                showStudentDialog = null
            },
        )
    }

    if (showFeePickerDialog) {
        QuickFeeStudentPickerDialog(
            students = uiState.students.filter { it.student.isActive },
            onDismiss = { showFeePickerDialog = false },
            onSelectStudent = { overview ->
                showFeePickerDialog = false
                collectFeeTarget = overview to DateUtils.currentMonthKey()
            },
        )
    }

    collectFeeTarget?.let { (studentOverview, monthKey) ->
        CollectFeeDialog(
            studentOverview = studentOverview,
            monthKey = monthKey,
            onDismiss = { collectFeeTarget = null },
            onConfirm = { amount, mode, paidDate, notes ->
                viewModel.collectFee(
                    studentOverview = studentOverview,
                    monthKey = monthKey,
                    amount = amount,
                    paymentMode = mode,
                    paidDate = paidDate,
                    notes = notes,
                )
                collectFeeTarget = null
            },
        )
    }

    activeReceipt?.let { receipt ->
        FeeReceiptDialog(
            receipt = receipt,
            onDismiss = { activeReceipt = null },
        )
    }

    if (showCreateTestDialog) {
        TestFormDialog(
            batches = uiState.batches.map { it.batch },
            onDismiss = { showCreateTestDialog = false },
            onSave = { test ->
                viewModel.saveTest(test)
                showCreateTestDialog = false
            },
        )
    }
}

@Composable
private fun SidebarDrawerContent(
    uiState: CoachingUiState,
    currentDestination: AppDestination,
    onSelectDestination: (AppDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp, horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InstituteEmblemBadge(
                logoText = uiState.instituteProfile.logoText,
                size = 44.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GuruDesk",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = uiState.instituteProfile.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        AppDestination.entries.forEach { dest ->
            val selected = currentDestination == dest
            NavigationDrawerItem(
                selected = selected,
                onClick = { onSelectDestination(dest) },
                icon = {
                    Icon(
                        imageVector = destinationIcon(dest, selected),
                        contentDescription = null,
                    )
                },
                label = { Text(dest.title) },
                badge = {
                    if (dest == AppDestination.FEES && uiState.pendingStudentsCount > 0) {
                        Badge {
                            Text(uiState.pendingStudentsCount.toString())
                        }
                    } else if (dest == AppDestination.BATCHES) {
                        Text(
                            text = uiState.batches.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                ),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("drawer_nav_${dest.name.lowercase()}"),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Offline Local Storage",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "All records saved safely on device. Use Settings to export JSON/CSV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickFeeStudentPickerDialog(
    students: List<StudentOverview>,
    onDismiss: () -> Unit,
    onSelectStudent: (StudentOverview) -> Unit,
) {
    val sorted = remember(students) {
        students.sortedByDescending { it.pendingCurrentMonth }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Student to Collect Fee") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sorted, key = { "pick_${it.student.id}" }) { item ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStudent(item) },
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StudentAvatar(
                                name = item.student.name,
                                colorIndex = item.batch?.colorIndex ?: 0,
                                size = 36.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.student.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    item.batch?.name ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = if (item.pendingCurrentMonth > 0) {
                                    "Due ${Formatters.formatInr(item.pendingCurrentMonth)}"
                                } else {
                                    "Paid"
                                },
                                style = MonoNumberStyle.copy(fontWeight = FontWeight.Bold),
                                color = if (item.pendingCurrentMonth > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun destinationIcon(dest: AppDestination, selected: Boolean): ImageVector {
    return when (dest) {
        AppDestination.DASHBOARD -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
        AppDestination.BATCHES -> if (selected) Icons.Filled.School else Icons.Outlined.School
        AppDestination.STUDENTS -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
        AppDestination.FEES -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
        AppDestination.ATTENDANCE -> if (selected) Icons.Filled.FactCheck else Icons.Outlined.FactCheck
        AppDestination.TESTS -> if (selected) Icons.Filled.Quiz else Icons.Outlined.Quiz
        AppDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}
