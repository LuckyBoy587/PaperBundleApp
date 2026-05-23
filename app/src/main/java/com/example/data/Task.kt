package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncState {
    CACHED,
    SYNCING,
    PENDING_WRITE,
    SYNCED,
    ERROR
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val profileOwner: String, // "AMMA" or "APPA" or "GENERAL"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    
    // Family shared metadata
    val createdByUid: String = "",
    val createdByName: String = "",
    val completedByUid: String? = null,
    val completedByName: String? = null,
    val familyId: String = "",
    val syncState: SyncState = SyncState.SYNCED
)
