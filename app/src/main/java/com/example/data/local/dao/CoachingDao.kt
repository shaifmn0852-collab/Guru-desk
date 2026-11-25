package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.FeePaymentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.TestMarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachingDao {

    // Batches
    @Query("SELECT * FROM batches ORDER BY createdAt ASC")
    fun observeBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches ORDER BY createdAt ASC")
    suspend fun getAllBatches(): List<BatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)

    // Students
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // Fee Payments
    @Query("SELECT * FROM fee_payments ORDER BY paidDate DESC, createdAt DESC")
    fun observeFeePayments(): Flow<List<FeePaymentEntity>>

    @Query("SELECT * FROM fee_payments ORDER BY paidDate DESC, createdAt DESC")
    suspend fun getAllFeePayments(): List<FeePaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeePayment(payment: FeePaymentEntity): Long

    @Delete
    suspend fun deleteFeePayment(payment: FeePaymentEntity)

    // Attendance
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY date DESC")
    suspend fun getAllAttendance(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(record: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendanceList(records: List<AttendanceEntity>)

    // Tests
    @Query("SELECT * FROM tests ORDER BY date DESC, createdAt DESC")
    fun observeTests(): Flow<List<TestEntity>>

    @Query("SELECT * FROM tests ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTests(): List<TestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity): Long

    @Update
    suspend fun updateTest(test: TestEntity)

    @Delete
    suspend fun deleteTest(test: TestEntity)

    // Test Marks
    @Query("SELECT * FROM test_marks")
    fun observeTestMarks(): Flow<List<TestMarkEntity>>

    @Query("SELECT * FROM test_marks")
    suspend fun getAllTestMarks(): List<TestMarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTestMark(mark: TestMarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTestMarks(marks: List<TestMarkEntity>)

    // Bulk operations for seeding & backup restore
    @Query("DELETE FROM test_marks")
    suspend fun clearTestMarks()

    @Query("DELETE FROM tests")
    suspend fun clearTests()

    @Query("DELETE FROM attendance")
    suspend fun clearAttendance()

    @Query("DELETE FROM fee_payments")
    suspend fun clearFeePayments()

    @Query("DELETE FROM students")
    suspend fun clearStudents()

    @Query("DELETE FROM batches")
    suspend fun clearBatches()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeePayments(payments: List<FeePaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<TestEntity>)

    @Transaction
    suspend fun replaceAllData(
        batches: List<BatchEntity>,
        students: List<StudentEntity>,
        payments: List<FeePaymentEntity>,
        attendance: List<AttendanceEntity>,
        tests: List<TestEntity>,
        testMarks: List<TestMarkEntity>,
    ) {
        clearTestMarks()
        clearTests()
        clearAttendance()
        clearFeePayments()
        clearStudents()
        clearBatches()

        insertBatches(batches)
        insertStudents(students)
        insertFeePayments(payments)
        upsertAttendanceList(attendance)
        insertTests(tests)
        upsertTestMarks(testMarks)
    }
}
