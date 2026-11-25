package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.InstitutePreferences
import com.example.data.local.InstituteProfile
import com.example.data.local.ThemePreference
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.FeePaymentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.TestMarkEntity
import com.example.data.repository.CoachingRepository
import com.example.util.DateUtils
import com.example.util.ReceiptData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination(val title: String, val shortLabel: String) {
    DASHBOARD("Dashboard", "Home"),
    BATCHES("Batches", "Batches"),
    STUDENTS("Students", "Students"),
    FEES("Fees & Dues", "Fees"),
    ATTENDANCE("Attendance", "Attend"),
    TESTS("Tests & Marks", "Tests"),
    SETTINGS("Settings & Backup", "Settings"),
}

enum class FeeStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    UNPAID("Pending"),
}

data class BatchSummary(
    val batch: BatchEntity,
    val studentCount: Int,
    val expectedMonthlyRevenue: Double,
)

data class StudentOverview(
    val student: StudentEntity,
    val batch: BatchEntity?,
    val baseFee: Double,
    val netMonthlyFee: Double,
    val paidSelectedMonth: Double,
    val pendingSelectedMonth: Double,
    val feeStatusSelectedMonth: FeeStatus,
    val paidCurrentMonth: Double,
    val pendingCurrentMonth: Double,
    val feeStatusCurrentMonth: FeeStatus,
    val monthlyAttendancePercent: Double,
    val totalPresentThisMonth: Int,
    val totalMarkedThisMonth: Int,
)

data class MonthlyCollectionPoint(
    val monthKey: String,
    val label: String,
    val amount: Double,
)

data class BatchDistributionSlice(
    val batchId: Long,
    val batchName: String,
    val studentCount: Int,
    val fraction: Float,
    val colorIndex: Int,
)

data class RankedStudentMark(
    val rank: Int,
    val student: StudentEntity,
    val marksObtained: Double?,
    val percentage: Double?,
)

data class TestOverview(
    val test: TestEntity,
    val batch: BatchEntity?,
    val enteredCount: Int,
    val totalStudentsInBatch: Int,
    val averagePercent: Double,
    val rankedEntries: List<RankedStudentMark>,
)

data class CoachingUiState(
    val isLoading: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val instituteProfile: InstituteProfile = InstituteProfile(),
    val batches: List<BatchSummary> = emptyList(),
    val students: List<StudentOverview> = emptyList(),
    val allPayments: List<FeePaymentEntity> = emptyList(),
    val allAttendance: List<AttendanceEntity> = emptyList(),
    val tests: List<TestOverview> = emptyList(),
    val selectedFeeMonthKey: String = DateUtils.currentMonthKey(),
    val selectedAttendanceDate: String = DateUtils.todayIso(),
    val totalActiveStudents: Int = 0,
    val collectedThisMonth: Double = 0.0,
    val pendingDuesThisMonth: Double = 0.0,
    val pendingStudentsCount: Int = 0,
    val todayAttendancePercent: Double? = null,
    val todayPresentCount: Int = 0,
    val todayTotalMarked: Int = 0,
    val monthlyTrend: List<MonthlyCollectionPoint> = emptyList(),
    val batchDistribution: List<BatchDistributionSlice> = emptyList(),
)

sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class OpenReceipt(val receipt: ReceiptData) : UiEvent()
}

class CoachingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoachingRepository(
        dao = AppDatabase.getInstance(application).coachingDao(),
        preferences = InstitutePreferences(application),
    )

    private val selectedFeeMonthKey = MutableStateFlow(DateUtils.currentMonthKey())
    private val selectedAttendanceDate = MutableStateFlow(DateUtils.todayIso())

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    private data class RawDataBundle(
        val batches: List<BatchEntity>,
        val students: List<StudentEntity>,
        val payments: List<FeePaymentEntity>,
        val attendance: List<AttendanceEntity>,
        val tests: List<TestEntity>,
        val marks: List<TestMarkEntity>,
    )

    private val rawDataFlow = combine(
        repository.batches,
        repository.students,
        repository.feePayments,
        repository.attendance,
        repository.tests,
        repository.testMarks,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        RawDataBundle(
            batches = arr[0] as List<BatchEntity>,
            students = arr[1] as List<StudentEntity>,
            payments = arr[2] as List<FeePaymentEntity>,
            attendance = arr[3] as List<AttendanceEntity>,
            tests = arr[4] as List<TestEntity>,
            marks = arr[5] as List<TestMarkEntity>,
        )
    }

    val uiState: StateFlow<CoachingUiState> = combine(
        rawDataFlow,
        repository.appPreferences,
        selectedFeeMonthKey,
        selectedAttendanceDate,
    ) { raw, prefs, feeMonth, attDate ->
        val currentMonth = DateUtils.currentMonthKey()
        val today = DateUtils.todayIso()
        val batchMap = raw.batches.associateBy { it.id }
        val activeStudents = raw.students.filter { it.isActive }

        val studentOverviews = raw.students.map { s ->
            val batch = batchMap[s.batchId]
            val baseFee = batch?.monthlyFee ?: 0.0
            val netFee = (baseFee - s.monthlyDiscount).coerceAtLeast(0.0)

            val paidSelected = raw.payments
                .filter { it.studentId == s.id && it.monthKey == feeMonth }
                .sumOf { it.amountPaid }
            val pendingSelected = (netFee - paidSelected).coerceAtLeast(0.0)
            val statusSelected = when {
                netFee <= 0.0 || pendingSelected <= 0.0 -> FeeStatus.PAID
                paidSelected > 0.0 -> FeeStatus.PARTIAL
                else -> FeeStatus.UNPAID
            }

            val paidCurrent = raw.payments
                .filter { it.studentId == s.id && it.monthKey == currentMonth }
                .sumOf { it.amountPaid }
            val pendingCurrent = (netFee - paidCurrent).coerceAtLeast(0.0)
            val statusCurrent = when {
                netFee <= 0.0 || pendingCurrent <= 0.0 -> FeeStatus.PAID
                paidCurrent > 0.0 -> FeeStatus.PARTIAL
                else -> FeeStatus.UNPAID
            }

            val monthAtt = raw.attendance.filter {
                it.studentId == s.id && it.date.startsWith(currentMonth)
            }
            val presentCount = monthAtt.count { it.status == "P" }
            val attPct = if (monthAtt.isEmpty()) 0.0 else presentCount * 100.0 / monthAtt.size

            StudentOverview(
                student = s,
                batch = batch,
                baseFee = baseFee,
                netMonthlyFee = netFee,
                paidSelectedMonth = paidSelected,
                pendingSelectedMonth = pendingSelected,
                feeStatusSelectedMonth = statusSelected,
                paidCurrentMonth = paidCurrent,
                pendingCurrentMonth = pendingCurrent,
                feeStatusCurrentMonth = statusCurrent,
                monthlyAttendancePercent = attPct,
                totalPresentThisMonth = presentCount,
                totalMarkedThisMonth = monthAtt.size,
            )
        }

        val batchSummaries = raw.batches.map { b ->
            val batchStudents = activeStudents.filter { it.batchId == b.id }
            val expected = batchStudents.sumOf { (b.monthlyFee - it.monthlyDiscount).coerceAtLeast(0.0) }
            BatchSummary(
                batch = b,
                studentCount = batchStudents.size,
                expectedMonthlyRevenue = expected,
            )
        }

        val collectedThisMonth = raw.payments
            .filter { it.monthKey == currentMonth }
            .sumOf { it.amountPaid }

        val activeOverviews = studentOverviews.filter { it.student.isActive }
        val pendingDuesThisMonth = activeOverviews.sumOf { it.pendingCurrentMonth }
        val pendingCount = activeOverviews.count { it.pendingCurrentMonth > 0.0 }

        val todayAtt = raw.attendance.filter { it.date == today }
        val todayPresent = todayAtt.count { it.status == "P" }
        val todayPct = if (todayAtt.isEmpty()) null else todayPresent * 100.0 / todayAtt.size

        val recentMonths = DateUtils.recentMonthKeys(6, currentMonth)
        val trend = recentMonths.map { mk ->
            MonthlyCollectionPoint(
                monthKey = mk,
                label = DateUtils.formatMonthShort(mk),
                amount = raw.payments.filter { it.monthKey == mk }.sumOf { it.amountPaid },
            )
        }

        val totalActive = activeStudents.size.coerceAtLeast(1)
        val distribution = batchSummaries.map { bs ->
            BatchDistributionSlice(
                batchId = bs.batch.id,
                batchName = bs.batch.name,
                studentCount = bs.studentCount,
                fraction = bs.studentCount.toFloat() / totalActive.toFloat(),
                colorIndex = bs.batch.colorIndex,
            )
        }

        val testOverviews = raw.tests.map { test ->
            val batch = batchMap[test.batchId]
            val batchStudents = activeStudents.filter { it.batchId == test.batchId }
            val marksForTest = raw.marks.filter { it.testId == test.id }.associateBy { it.studentId }

            val scored = batchStudents
                .map { s ->
                    val obtained = marksForTest[s.id]?.marksObtained
                    val pct = if (obtained != null && test.totalMarks > 0) {
                        (obtained * 100.0 / test.totalMarks).coerceIn(0.0, 100.0)
                    } else null
                    Triple(s, obtained, pct)
                }
                .sortedWith(
                    compareByDescending<Triple<StudentEntity, Double?, Double?>> { it.second != null }
                        .thenByDescending { it.second ?: -1.0 }
                        .thenBy { it.first.name }
                )

            var currentRank = 0
            var lastScore: Double? = null
            val ranked = scored.mapIndexed { idx, (student, obtained, pct) ->
                val rank = if (obtained == null) {
                    0
                } else {
                    if (lastScore == null || obtained < lastScore!!) {
                        currentRank = idx + 1
                        lastScore = obtained
                    }
                    currentRank
                }
                RankedStudentMark(
                    rank = rank,
                    student = student,
                    marksObtained = obtained,
                    percentage = pct,
                )
            }

            val validPcts = ranked.mapNotNull { it.percentage }
            val avgPct = if (validPcts.isEmpty()) 0.0 else validPcts.average()

            TestOverview(
                test = test,
                batch = batch,
                enteredCount = validPcts.size,
                totalStudentsInBatch = batchStudents.size,
                averagePercent = avgPct,
                rankedEntries = ranked,
            )
        }

        CoachingUiState(
            isLoading = false,
            themePreference = prefs.themePreference,
            instituteProfile = prefs.instituteProfile,
            batches = batchSummaries,
            students = studentOverviews,
            allPayments = raw.payments,
            allAttendance = raw.attendance,
            tests = testOverviews,
            selectedFeeMonthKey = feeMonth,
            selectedAttendanceDate = attDate,
            totalActiveStudents = activeStudents.size,
            collectedThisMonth = collectedThisMonth,
            pendingDuesThisMonth = pendingDuesThisMonth,
            pendingStudentsCount = pendingCount,
            todayAttendancePercent = todayPct,
            todayPresentCount = todayPresent,
            todayTotalMarked = todayAtt.size,
            monthlyTrend = trend,
            batchDistribution = distribution,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CoachingUiState(),
    )

    fun setFeeMonthKey(monthKey: String) {
        selectedFeeMonthKey.value = monthKey
    }

    fun setAttendanceDate(dateIso: String) {
        selectedAttendanceDate.value = dateIso
    }

    fun emitMessage(message: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage(message))
        }
    }

    fun toggleDarkLightMode() {
        viewModelScope.launch {
            val current = uiState.value.themePreference
            val next = if (current == ThemePreference.DARK) ThemePreference.LIGHT else ThemePreference.DARK
            repository.setThemePreference(next)
            _events.emit(
                UiEvent.ShowMessage(
                    if (next == ThemePreference.DARK) "Dark mode enabled" else "Light mode enabled"
                )
            )
        }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            repository.setThemePreference(theme)
            _events.emit(UiEvent.ShowMessage("Theme updated"))
        }
    }

    fun saveBatch(batch: BatchEntity) {
        viewModelScope.launch {
            val isNew = batch.id == 0L
            repository.saveBatch(batch)
            _events.emit(
                UiEvent.ShowMessage(if (isNew) "Batch \"${batch.name}\" created" else "Batch updated")
            )
        }
    }

    fun deleteBatch(batch: BatchEntity) {
        viewModelScope.launch {
            repository.deleteBatch(batch)
            _events.emit(UiEvent.ShowMessage("Batch \"${batch.name}\" deleted"))
        }
    }

    fun saveStudent(student: StudentEntity) {
        viewModelScope.launch {
            val isNew = student.id == 0L
            repository.saveStudent(student)
            _events.emit(
                UiEvent.ShowMessage(if (isNew) "${student.name} enrolled" else "${student.name} updated")
            )
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _events.emit(UiEvent.ShowMessage("${student.name} removed"))
        }
    }

    fun collectFee(
        studentOverview: StudentOverview,
        monthKey: String,
        amount: Double,
        paymentMode: String,
        paidDate: String,
        notes: String,
    ) {
        viewModelScope.launch {
            val payment = repository.recordFeePayment(
                studentId = studentOverview.student.id,
                monthKey = monthKey,
                amountPaid = amount,
                paymentMode = paymentMode,
                paidDate = paidDate,
                notes = notes,
            )
            val totalPaidForMonth = uiState.value.allPayments
                .filter { it.studentId == studentOverview.student.id && it.monthKey == monthKey }
                .sumOf { it.amountPaid } + amount
            val remaining = (studentOverview.netMonthlyFee - totalPaidForMonth).coerceAtLeast(0.0)
            val receipt = ReceiptData(
                receiptNumber = payment.receiptNumber,
                paidDate = payment.paidDate,
                monthKey = monthKey,
                studentName = studentOverview.student.name,
                fatherName = studentOverview.student.fatherName,
                phone = studentOverview.student.phone,
                batchName = studentOverview.batch?.name ?: "General Batch",
                baseFee = studentOverview.baseFee,
                discount = studentOverview.student.monthlyDiscount,
                netMonthlyFee = studentOverview.netMonthlyFee,
                amountPaidThisReceipt = amount,
                totalPaidForMonth = totalPaidForMonth,
                balanceRemaining = remaining,
                paymentMode = paymentMode,
                notes = notes,
                institute = uiState.value.instituteProfile,
            )
            _events.emit(UiEvent.ShowMessage("Fee recorded • Receipt ${payment.receiptNumber}"))
            _events.emit(UiEvent.OpenReceipt(receipt))
        }
    }

    fun openReceiptForPayment(payment: FeePaymentEntity) {
        val state = uiState.value
        val studentOverview = state.students.find { it.student.id == payment.studentId } ?: return
        val totalPaidForMonth = state.allPayments
            .filter { it.studentId == payment.studentId && it.monthKey == payment.monthKey }
            .sumOf { it.amountPaid }
        val remaining = (studentOverview.netMonthlyFee - totalPaidForMonth).coerceAtLeast(0.0)
        val receipt = ReceiptData(
            receiptNumber = payment.receiptNumber,
            paidDate = payment.paidDate,
            monthKey = payment.monthKey,
            studentName = studentOverview.student.name,
            fatherName = studentOverview.student.fatherName,
            phone = studentOverview.student.phone,
            batchName = studentOverview.batch?.name ?: "General Batch",
            baseFee = studentOverview.baseFee,
            discount = studentOverview.student.monthlyDiscount,
            netMonthlyFee = studentOverview.netMonthlyFee,
            amountPaidThisReceipt = payment.amountPaid,
            totalPaidForMonth = totalPaidForMonth,
            balanceRemaining = remaining,
            paymentMode = payment.paymentMode,
            notes = payment.notes,
            institute = state.instituteProfile,
        )
        viewModelScope.launch {
            _events.emit(UiEvent.OpenReceipt(receipt))
        }
    }

    fun markAttendance(studentId: Long, batchId: Long, date: String, status: String) {
        viewModelScope.launch {
            repository.markAttendance(studentId, batchId, date, status)
        }
    }

    fun markAllPresent(batchId: Long, date: String) {
        viewModelScope.launch {
            val ids = uiState.value.students
                .filter { it.student.isActive && it.student.batchId == batchId }
                .map { it.student.id }
            if (ids.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("No active students in this batch"))
                return@launch
            }
            repository.markBatchAllPresent(batchId, ids, date)
            _events.emit(UiEvent.ShowMessage("Marked ${ids.size} students Present"))
        }
    }

    fun saveTest(test: TestEntity) {
        viewModelScope.launch {
            val isNew = test.id == 0L
            repository.saveTest(test)
            _events.emit(
                UiEvent.ShowMessage(if (isNew) "Test \"${test.name}\" created" else "Test updated")
            )
        }
    }

    fun deleteTest(test: TestEntity) {
        viewModelScope.launch {
            repository.deleteTest(test)
            _events.emit(UiEvent.ShowMessage("Test deleted"))
        }
    }

    fun saveTestMarks(testId: Long, marks: Map<Long, Double>) {
        viewModelScope.launch {
            repository.saveTestMarks(testId, marks)
            _events.emit(UiEvent.ShowMessage("Marks saved & ranks updated"))
        }
    }

    fun updateInstituteProfile(profile: InstituteProfile) {
        viewModelScope.launch {
            repository.updateInstituteProfile(profile)
            _events.emit(UiEvent.ShowMessage("Institute settings saved"))
        }
    }

    suspend fun generateJsonBackup(): String = repository.exportJsonBackup()

    suspend fun generateCsvReport(): String = repository.exportCsvReport()

    fun restoreJsonBackup(jsonString: String) {
        viewModelScope.launch {
            try {
                repository.importJsonBackup(jsonString)
                _events.emit(UiEvent.ShowMessage("Backup restored successfully"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("Backup restore failed: ${e.message ?: "Invalid file"}"))
            }
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetToSampleData()
            _events.emit(UiEvent.ShowMessage("Sample coaching data restored"))
        }
    }
}
