package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE profileOwner = :profile ORDER BY isCompleted ASC, createdAt DESC")
    fun getTasksForProfile(profile: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM tasks WHERE familyId = :familyId")
    suspend fun getTasksByFamily(familyId: String): List<Task>
}
