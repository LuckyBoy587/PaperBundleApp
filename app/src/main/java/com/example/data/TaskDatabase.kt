package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract val taskDao: TaskDao

    companion object {
        private const val TAG = "PAPER_BUNDLE"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            Log.d(TAG, "TaskDatabase: getDatabase() requested")
            return INSTANCE ?: synchronized(this) {
                val existing = INSTANCE
                if (existing != null) {
                    Log.d(TAG, "TaskDatabase: Returning existing database instance: $existing")
                    existing
                } else {
                    Log.d(TAG, "TaskDatabase: No existing database instance found. Building paperbundle_database...")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        TaskDatabase::class.java,
                        "paperbundle_database"
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    INSTANCE = instance
                    Log.d(TAG, "TaskDatabase: Database instance successfully built: $instance")
                    instance
                }
            }
        }
    }
}
