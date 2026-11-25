package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CoachingDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.BatchEntity
import com.example.data.local.entity.FeePaymentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.TestMarkEntity

@Database(
    entities = [
        BatchEntity::class,
        StudentEntity::class,
        FeePaymentEntity::class,
        AttendanceEntity::class,
        TestEntity::class,
        TestMarkEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachingDao(): CoachingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gurudesk_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
