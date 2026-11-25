package com.example.data.repository

import com.example.data.local.InstitutePreferences
import com.example.data.local.InstituteProfile
import com.example.data.local.dao.CoachingDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.FeePaymentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.TestMarkEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class CoachingRepository(
    private val dao: CoachingDao,
    private val preferences: InstitutePreferences,
) {
    val batches: Flow<List<BatchEntity>> = dao.observeBatches()
    val students: Flow<List<StudentEntity>> = dao.observeStudents()
    val feePayments: Flow<List<FeePaymentEntity>> = dao.observeFeePayments()
    val attendance: Flow<List<AttendanceEntity>> = dao.observeAttendance()
    val tests: Flow<List<TestEntity>> = dao.observeTests()
    val testMarks: Flow<List<TestMarkEntity>> = dao.observeTestMarks()
    val appPreferences = preferences.preferencesFlow

    suspend fun ensureSeeded() {
        val prefs = preferences.preferencesFlow.first()
        if (!prefs.hasSeeded) {
            seedSampleData()
            preferences.markSeeded()
        }
    }

    suspend fun resetToSampleData() {
        seedSampleData()
        preferences.markSeeded()
    }

    private suspend fun seedSampleData() {
        val now = System.currentTimeMillis()
        val b1 = BatchEntity(
            id = 1L,
            name = "Class 10th Science",
            subject = "Physics & Chemistry",
            timing = "4:00 PM – 5:30 PM",
            monthlyFee = 2500.0,
            assignedTeacher = "Prof. Rajesh Sharma",
            colorIndex = 0,
            createdAt = now - 600000L,
        )
        val b2 = BatchEntity(
            id = 2L,
            name = "JEE / NEET Foundation",
            subject = "Physics, Chem & Maths",
            timing = "6:00 PM – 8:00 PM",
            monthlyFee = 4500.0,
            assignedTeacher = "Dr. Ananya Verma",
            colorIndex = 1,
            createdAt = now - 500000L,
        )
        val b3 = BatchEntity(
            id = 3L,
            name = "English Speaking Batch",
            subject = "Spoken English & Grammar",
            timing = "10:00 AM – 11:30 AM",
            monthlyFee = 1800.0,
            assignedTeacher = "Ms. Priya Nair",
            colorIndex = 2,
            createdAt = now - 400000L,
        )
        val sampleBatches = listOf(b1, b2, b3)

        val currentMonth = DateUtils.currentMonthKey()
        val m1 = DateUtils.shiftMonthKey(currentMonth, -1)
        val m2 = DateUtils.shiftMonthKey(currentMonth, -2)
        val m3 = DateUtils.shiftMonthKey(currentMonth, -3)
        val m4 = DateUtils.shiftMonthKey(currentMonth, -4)
        val m5 = DateUtils.shiftMonthKey(currentMonth, -5)

        val sampleStudents = listOf(
            StudentEntity(1L, "Aarav Sharma", "Suresh Sharma", "9820112233", 1L, "$m4-05", 0.0, true),
            StudentEntity(2L, "Diya Patel", "Mahesh Patel", "9820223344", 1L, "$m5-10", 300.0, true),
            StudentEntity(3L, "Rohan Kulkarni", "Vinayak Kulkarni", "9820334455", 1L, "$m3-01", 0.0, true),
            StudentEntity(4L, "Sneha Iyer", "Subramanian Iyer", "9820445566", 1L, "$m4-14", 0.0, true),
            StudentEntity(5L, "Kabir Malhotra", "Rajeev Malhotra", "9820556677", 2L, "$m5-02", 500.0, true),
            StudentEntity(6L, "Anika Deshmukh", "Prakash Deshmukh", "9820667788", 2L, "$m5-05", 0.0, true),
            StudentEntity(7L, "Vivaan Joshi", "Nitin Joshi", "9820778899", 2L, "$m2-12", 0.0, true),
            StudentEntity(8L, "Meera Nair", "Gopal Nair", "9820889900", 2L, "$m3-18", 500.0, true),
            StudentEntity(9L, "Aditya Gupta", "Ramesh Gupta", "9820990011", 3L, "$m4-01", 0.0, true),
            StudentEntity(10L, "Ishita Sen", "Debashish Sen", "9821001122", 3L, "$m2-03", 200.0, true),
            StudentEntity(11L, "Pranav Tiwari", "Alok Tiwari", "9821112233", 1L, "$m1-04", 0.0, true),
            StudentEntity(12L, "Kavya Reddy", "Srinivas Reddy", "9821223344", 2L, "$m1-08", 0.0, true),
        )

        val payments = mutableListOf<FeePaymentEntity>()
        var pid = 1L
        val pastMonths = listOf(m5, m4, m3, m2, m1)
        val modes = listOf("UPI", "Cash", "Bank Transfer")

        pastMonths.forEachIndexed { idx, mk ->
            sampleStudents.forEach { s ->
                if (s.joiningDate.substring(0, 7) <= mk) {
                    val batchFee = sampleBatches.first { it.id == s.batchId }.monthlyFee
                    val net = (batchFee - s.monthlyDiscount).coerceAtLeast(0.0)
                    // Leave a small portion unpaid in older months? No, keep past mostly paid, varied cleanly
                    if (!(idx == pastMonths.lastIndex && s.id in listOf(4L, 8L))) {
                        payments.add(
                            FeePaymentEntity(
                                id = pid++,
                                studentId = s.id,
                                monthKey = mk,
                                amountPaid = net,
                                paymentMode = modes[(s.id.toInt() + idx) % modes.size],
                                receiptNumber = "GD-${mk.replace("-", "")}-${(100 + pid).toString()}",
                                paidDate = "$mk-${(4 + (s.id.toInt() % 12)).toString().padStart(2, '0')}",
                                notes = "Monthly tuition fee",
                            )
                        )
                    }
                }
            }
        }

        // Current month: mix of Paid, Partial, and Unpaid
        val paidFullIds = listOf(1L, 2L, 5L, 6L, 9L, 12L)
        val partialIds = listOf(3L to 1500.0, 7L to 2500.0, 10L to 1000.0)
        // Unpaid: 4L (Sneha), 8L (Meera), 11L (Pranav)
        paidFullIds.forEach { sid ->
            val s = sampleStudents.first { it.id == sid }
            val net = (sampleBatches.first { it.id == s.batchId }.monthlyFee - s.monthlyDiscount).coerceAtLeast(0.0)
            payments.add(
                FeePaymentEntity(
                    id = pid++,
                    studentId = sid,
                    monthKey = currentMonth,
                    amountPaid = net,
                    paymentMode = modes[sid.toInt() % modes.size],
                    receiptNumber = "GD-${currentMonth.replace("-", "")}-${(100 + pid)}",
                    paidDate = DateUtils.daysAgoIso((sid.toInt() % 6) + 1),
                    notes = "Paid in full",
                )
            )
        }
        partialIds.forEach { (sid, amt) ->
            payments.add(
                FeePaymentEntity(
                    id = pid++,
                    studentId = sid,
                    monthKey = currentMonth,
                    amountPaid = amt,
                    paymentMode = "UPI",
                    receiptNumber = "GD-${currentMonth.replace("-", "")}-${(100 + pid)}",
                    paidDate = DateUtils.daysAgoIso(2),
                    notes = "First installment",
                )
            )
        }

        // Attendance for past 10 days including today
        val attendanceRecords = mutableListOf<AttendanceEntity>()
        var aid = 1L
        for (daysAgo in 9 downTo 0) {
            val d = DateUtils.daysAgoIso(daysAgo)
            sampleStudents.forEach { s ->
                val status = when {
                    daysAgo == 0 && s.id in listOf(4L, 10L) -> "A"
                    daysAgo == 0 && s.id == 7L -> "L"
                    daysAgo > 0 && ((s.id.toInt() + daysAgo) % 9 == 0) -> "A"
                    daysAgo > 0 && ((s.id.toInt() + daysAgo) % 13 == 0) -> "L"
                    else -> "P"
                }
                attendanceRecords.add(
                    AttendanceEntity(
                        id = aid++,
                        studentId = s.id,
                        batchId = s.batchId,
                        date = d,
                        status = status,
                    )
                )
            }
        }

        // Sample Tests & Marks
        val sampleTests = listOf(
            TestEntity(1L, 1L, "Light & Electricity Unit Test", DateUtils.daysAgoIso(5), 100.0),
            TestEntity(2L, 2L, "JEE Kinematics & Calculus Mock", DateUtils.daysAgoIso(3), 150.0),
            TestEntity(3L, 3L, "Fluency & Grammar Assessment", DateUtils.daysAgoIso(2), 50.0),
        )
        val sampleMarks = listOf(
            // Test 1 (Batch 1: 1, 2, 3, 4, 11)
            TestMarkEntity(1L, 1L, 1L, 92.0),
            TestMarkEntity(2L, 1L, 2L, 88.0),
            TestMarkEntity(3L, 1L, 3L, 76.0),
            TestMarkEntity(4L, 1L, 4L, 95.0),
            TestMarkEntity(5L, 1L, 11L, 81.0),
            // Test 2 (Batch 2: 5, 6, 7, 8, 12)
            TestMarkEntity(6L, 2L, 5L, 134.0),
            TestMarkEntity(7L, 2L, 6L, 141.0),
            TestMarkEntity(8L, 2L, 7L, 118.0),
            TestMarkEntity(9L, 2L, 8L, 129.0),
            TestMarkEntity(10L, 2L, 12L, 141.0),
            // Test 3 (Batch 3: 9, 10)
            TestMarkEntity(11L, 3L, 9L, 44.0),
            TestMarkEntity(12L, 3L, 10L, 47.0),
        )

        dao.replaceAllData(
            batches = sampleBatches,
            students = sampleStudents,
            payments = payments,
            attendance = attendanceRecords,
            tests = sampleTests,
            testMarks = sampleMarks,
        )
    }

    // Batch operations
    suspend fun saveBatch(batch: BatchEntity): Long {
        return if (batch.id == 0L) {
            dao.insertBatch(batch)
        } else {
            dao.updateBatch(batch)
            batch.id
        }
    }

    suspend fun deleteBatch(batch: BatchEntity) = dao.deleteBatch(batch)

    // Student operations
    suspend fun saveStudent(student: StudentEntity): Long {
        return if (student.id == 0L) {
            dao.insertStudent(student)
        } else {
            dao.updateStudent(student)
            student.id
        }
    }

    suspend fun deleteStudent(student: StudentEntity) = dao.deleteStudent(student)

    // Fee operations
    suspend fun recordFeePayment(
        studentId: Long,
        monthKey: String,
        amountPaid: Double,
        paymentMode: String,
        paidDate: String,
        notes: String,
    ): FeePaymentEntity {
        val rand = Random.nextInt(1000, 9999)
        val receiptNo = "GD-${monthKey.replace("-", "")}-$rand"
        val entity = FeePaymentEntity(
            studentId = studentId,
            monthKey = monthKey,
            amountPaid = amountPaid,
            paymentMode = paymentMode,
            receiptNumber = receiptNo,
            paidDate = paidDate,
            notes = notes,
        )
        val newId = dao.insertFeePayment(entity)
        return entity.copy(id = newId)
    }

    suspend fun deleteFeePayment(payment: FeePaymentEntity) = dao.deleteFeePayment(payment)

    // Attendance operations
    suspend fun markAttendance(studentId: Long, batchId: Long, date: String, status: String) {
        dao.upsertAttendance(
            AttendanceEntity(
                studentId = studentId,
                batchId = batchId,
                date = date,
                status = status,
            )
        )
    }

    suspend fun markBatchAllPresent(batchId: Long, studentIds: List<Long>, date: String) {
        val records = studentIds.map { sid ->
            AttendanceEntity(
                studentId = sid,
                batchId = batchId,
                date = date,
                status = "P",
            )
        }
        dao.upsertAttendanceList(records)
    }

    // Test & Marks operations
    suspend fun saveTest(test: TestEntity): Long {
        return if (test.id == 0L) {
            dao.insertTest(test)
        } else {
            dao.updateTest(test)
            test.id
        }
    }

    suspend fun deleteTest(test: TestEntity) = dao.deleteTest(test)

    suspend fun saveTestMarks(testId: Long, marksByStudent: Map<Long, Double>) {
        val entities = marksByStudent.map { (studentId, marks) ->
            TestMarkEntity(
                testId = testId,
                studentId = studentId,
                marksObtained = marks,
            )
        }
        dao.upsertTestMarks(entities)
    }

    // Preferences
    suspend fun setThemePreference(theme: com.example.data.local.ThemePreference) {
        preferences.setThemePreference(theme)
    }

    suspend fun updateInstituteProfile(profile: InstituteProfile) {
        preferences.updateInstituteProfile(profile)
    }

    // JSON Export / Import
    suspend fun exportJsonBackup(): String {
        val prefs = preferences.preferencesFlow.first()
        val batches = dao.getAllBatches()
        val students = dao.getAllStudents()
        val payments = dao.getAllFeePayments()
        val attendanceList = dao.getAllAttendance()
        val tests = dao.getAllTests()
        val marks = dao.getAllTestMarks()

        val root = JSONObject().apply {
            put("app", "GuruDesk")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put(
                "institute",
                JSONObject().apply {
                    put("name", prefs.instituteProfile.name)
                    put("address", prefs.instituteProfile.address)
                    put("logoText", prefs.instituteProfile.logoText)
                    put("contactNumber", prefs.instituteProfile.contactNumber)
                }
            )
            put("batches", JSONArray().apply {
                batches.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("subject", b.subject)
                        put("timing", b.timing)
                        put("monthlyFee", b.monthlyFee)
                        put("assignedTeacher", b.assignedTeacher)
                        put("colorIndex", b.colorIndex)
                        put("createdAt", b.createdAt)
                    })
                }
            })
            put("students", JSONArray().apply {
                students.forEach { s ->
                    put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("fatherName", s.fatherName)
                        put("phone", s.phone)
                        put("batchId", s.batchId)
                        put("joiningDate", s.joiningDate)
                        put("monthlyDiscount", s.monthlyDiscount)
                        put("isActive", s.isActive)
                        put("createdAt", s.createdAt)
                    })
                }
            })
            put("feePayments", JSONArray().apply {
                payments.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("studentId", p.studentId)
                        put("monthKey", p.monthKey)
                        put("amountPaid", p.amountPaid)
                        put("paymentMode", p.paymentMode)
                        put("receiptNumber", p.receiptNumber)
                        put("paidDate", p.paidDate)
                        put("notes", p.notes)
                        put("createdAt", p.createdAt)
                    })
                }
            })
            put("attendance", JSONArray().apply {
                attendanceList.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("studentId", a.studentId)
                        put("batchId", a.batchId)
                        put("date", a.date)
                        put("status", a.status)
                    })
                }
            })
            put("tests", JSONArray().apply {
                tests.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("batchId", t.batchId)
                        put("name", t.name)
                        put("date", t.date)
                        put("totalMarks", t.totalMarks)
                        put("createdAt", t.createdAt)
                    })
                }
            })
            put("testMarks", JSONArray().apply {
                marks.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("testId", m.testId)
                        put("studentId", m.studentId)
                        put("marksObtained", m.marksObtained)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun importJsonBackup(jsonString: String) {
        val root = JSONObject(jsonString)
        if (root.optString("app") != "GuruDesk") {
            throw IllegalArgumentException("Invalid GuruDesk backup file")
        }

        root.optJSONObject("institute")?.let { inst ->
            preferences.updateInstituteProfile(
                InstituteProfile(
                    name = inst.optString("name", "VidyaSagar Coaching Academy"),
                    address = inst.optString("address", ""),
                    logoText = inst.optString("logoText", "GD"),
                    contactNumber = inst.optString("contactNumber", ""),
                )
            )
        }

        val batches = mutableListOf<BatchEntity>()
        root.optJSONArray("batches")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                batches.add(
                    BatchEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        subject = o.optString("subject", ""),
                        timing = o.optString("timing", ""),
                        monthlyFee = o.getDouble("monthlyFee"),
                        assignedTeacher = o.optString("assignedTeacher", ""),
                        colorIndex = o.optInt("colorIndex", 0),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val students = mutableListOf<StudentEntity>()
        root.optJSONArray("students")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                students.add(
                    StudentEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        fatherName = o.optString("fatherName", ""),
                        phone = o.optString("phone", ""),
                        batchId = o.getLong("batchId"),
                        joiningDate = o.optString("joiningDate", DateUtils.todayIso()),
                        monthlyDiscount = o.optDouble("monthlyDiscount", 0.0),
                        isActive = o.optBoolean("isActive", true),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val payments = mutableListOf<FeePaymentEntity>()
        root.optJSONArray("feePayments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                payments.add(
                    FeePaymentEntity(
                        id = o.getLong("id"),
                        studentId = o.getLong("studentId"),
                        monthKey = o.getString("monthKey"),
                        amountPaid = o.getDouble("amountPaid"),
                        paymentMode = o.optString("paymentMode", "Cash"),
                        receiptNumber = o.optString("receiptNumber", "GD-IMPORT"),
                        paidDate = o.optString("paidDate", DateUtils.todayIso()),
                        notes = o.optString("notes", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val attendanceList = mutableListOf<AttendanceEntity>()
        root.optJSONArray("attendance")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                attendanceList.add(
                    AttendanceEntity(
                        id = o.getLong("id"),
                        studentId = o.getLong("studentId"),
                        batchId = o.getLong("batchId"),
                        date = o.getString("date"),
                        status = o.getString("status"),
                    )
                )
            }
        }

        val tests = mutableListOf<TestEntity>()
        root.optJSONArray("tests")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tests.add(
                    TestEntity(
                        id = o.getLong("id"),
                        batchId = o.getLong("batchId"),
                        name = o.getString("name"),
                        date = o.getString("date"),
                        totalMarks = o.getDouble("totalMarks"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    )
                )
            }
        }

        val marks = mutableListOf<TestMarkEntity>()
        root.optJSONArray("testMarks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                marks.add(
                    TestMarkEntity(
                        id = o.getLong("id"),
                        testId = o.getLong("testId"),
                        studentId = o.getLong("studentId"),
                        marksObtained = o.getDouble("marksObtained"),
                    )
                )
            }
        }

        dao.replaceAllData(
            batches = batches,
            students = students,
            payments = payments,
            attendance = attendanceList,
            tests = tests,
            testMarks = marks,
        )
        preferences.markSeeded()
    }

    suspend fun exportCsvReport(): String {
        val batches = dao.getAllBatches().associateBy { it.id }
        val students = dao.getAllStudents()
        val payments = dao.getAllFeePayments()
        val attendanceList = dao.getAllAttendance()
        val currentMonth = DateUtils.currentMonthKey()

        val sb = StringBuilder()
        sb.appendLine("Student ID,Name,Father Name,Phone,Batch,Joining Date,Monthly Fee (INR),Discount (INR),Net Fee (INR),Paid This Month (INR),Pending This Month (INR),Fee Status,Monthly Attendance %")
        students.forEach { s ->
            val batch = batches[s.batchId]
            val baseFee = batch?.monthlyFee ?: 0.0
            val netFee = (baseFee - s.monthlyDiscount).coerceAtLeast(0.0)
            val paidThisMonth = payments
                .filter { it.studentId == s.id && it.monthKey == currentMonth }
                .sumOf { it.amountPaid }
            val pending = (netFee - paidThisMonth).coerceAtLeast(0.0)
            val status = when {
                pending <= 0.0 -> "Paid"
                paidThisMonth > 0.0 -> "Partial"
                else -> "Unpaid"
            }
            val monthAtt = attendanceList.filter { it.studentId == s.id && it.date.startsWith(currentMonth) }
            val attPct = if (monthAtt.isEmpty()) 0.0 else {
                monthAtt.count { it.status == "P" } * 100.0 / monthAtt.size
            }
            fun esc(v: String) = "\"${v.replace("\"", "\"\"")}\""
            sb.appendLine(
                "${s.id},${esc(s.name)},${esc(s.fatherName)},${esc(s.phone)},${esc(batch?.name ?: "")},${s.joiningDate},${baseFee.toLong()},${s.monthlyDiscount.toLong()},${netFee.toLong()},${paidThisMonth.toLong()},${pending.toLong()},$status,${String.format("%.1f", attPct)}%"
            )
        }
        return sb.toString()
    }
}
