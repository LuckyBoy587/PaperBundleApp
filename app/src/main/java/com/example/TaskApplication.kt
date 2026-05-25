package com.example

import android.app.Application
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
        FirebaseSyncManager.init(this)
    }
}
