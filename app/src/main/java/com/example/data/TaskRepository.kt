package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

class TaskRepository(val taskDao: TaskDao) {
    companion object {
        private const val TAG = "PAPER_BUNDLE"
    }

    fun getTasksForProfile(profile: String): Flow<List<Task>> {
        Log.d(TAG, "TaskRepository: getTasksForProfile() requested for profile: $profile")
        return taskDao.getTasksForProfile(profile)
    }

    fun getAllTasks(): Flow<List<Task>> {
        Log.d(TAG, "TaskRepository: getAllTasks() requested")
        return taskDao.getAllTasks()
    }

    suspend fun insertTask(task: Task) {
        Log.d(TAG, "TaskRepository: insertTask() called: ID=${task.id}, title='${task.title}', profileOwner=${task.profileOwner}")
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        Log.d(TAG, "TaskRepository: updateTask() called: ID=${task.id}, title='${task.title}', isCompleted=${task.isCompleted}, profileOwner=${task.profileOwner}")
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(id: String) {
        Log.d(TAG, "TaskRepository: deleteTask() called: ID=$id")
        taskDao.deleteTaskById(id)
    }
}
