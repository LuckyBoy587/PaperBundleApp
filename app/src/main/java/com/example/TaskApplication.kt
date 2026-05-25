package com.example

import android.app.Application
import android.util.Log
import com.example.data.TaskDatabase
import com.example.data.TaskRepository
import com.example.data.UpdateRepository
import com.example.util.FirebaseSyncManager

class TaskApplication : Application() {
    val database by lazy { TaskDatabase.getDatabase(this) }
    val repository by lazy { TaskRepository(database.taskDao) }
    val updateRepository by lazy { UpdateRepository(this) }

    override fun onCreate() {
        super.onCreate()
        Log.d(MainActivity.TAG, "Application started. Current version: ${BuildConfig.VERSION_NAME}")
        FirebaseSyncManager.init(this)
    }
}
