package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(val taskDao: TaskDao) {
    fun getTasksForProfile(profile: String): Flow<List<Task>> =
        taskDao.getTasksForProfile(profile)

    fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks()

    suspend fun insertTask(task: Task) =
        taskDao.insertTask(task)

    suspend fun updateTask(task: Task) =
        taskDao.updateTask(task)

    suspend fun deleteTask(id: String) =
        taskDao.deleteTaskById(id)
}
