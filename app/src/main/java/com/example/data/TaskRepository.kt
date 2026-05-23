package com.example.data

import android.util.Log
import com.example.util.FirebaseSyncManager
import com.example.util.LocalSyncTracker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine

class TaskRepository(val taskDao: TaskDao) {
    companion object {
        private const val TAG = "PAPER_BUNDLE"
    }

    fun getTasksForProfile(profile: String): Flow<List<Task>> {
        Log.d(TAG, "TaskRepository: getTasksForProfile() requested for profile: $profile, isFirebaseInitialized=${FirebaseSyncManager.isFirebaseInitialized}")
        if (FirebaseSyncManager.isFirebaseInitialized) {
            val session = FirebaseSyncManager.currentUserSession.value
            val familyId = session?.familyId
            if (!familyId.isNullOrEmpty()) {
                return getTasksForProfileFromFirestore(familyId, profile)
            }
        }
        return taskDao.getTasksForProfile(profile)
    }

    fun getAllTasks(): Flow<List<Task>> {
        Log.d(TAG, "TaskRepository: getAllTasks() requested, isFirebaseInitialized=${FirebaseSyncManager.isFirebaseInitialized}")
        if (FirebaseSyncManager.isFirebaseInitialized) {
            val session = FirebaseSyncManager.currentUserSession.value
            val familyId = session?.familyId
            if (!familyId.isNullOrEmpty()) {
                return getAllTasksFromFirestore(familyId)
            }
        }
        return taskDao.getAllTasks()
    }

    private fun getTasksForProfileFromFirestore(familyId: String, profile: String): Flow<List<Task>> {
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("families")
            .document(familyId)
            .collection("tasks")
            .whereEqualTo("profileOwner", profile)

        val firestoreFlow = callbackFlow<QuerySnapshot> {
            val listener = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore snapshot listener error for profile $profile", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot)
                }
            }
            awaitClose { listener.remove() }
        }

        return combine(firestoreFlow, LocalSyncTracker.syncStates) { snapshot, syncStateMap ->
            val isFromCache = snapshot.metadata.isFromCache
            val tasks = snapshot.documents.mapNotNull { doc ->
                try {
                    val title = doc.getString("title") ?: ""
                    val isCompleted = doc.getBoolean("completed") ?: false
                    val profileOwner = doc.getString("profileOwner") ?: "GENERAL"
                    val createdBy = doc.getString("createdBy") ?: "Unknown"
                    val completedBy = doc.getString("completedBy")
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val completedAt = doc.getLong("completedAt")
                    
                    val hasPendingWrites = doc.metadata.hasPendingWrites()
                    
                    // Determine Sync State: Check local tracker override first, fallback to metadata
                    val syncState = syncStateMap[doc.id] ?: when {
                        hasPendingWrites -> {
                            if (FirebaseSyncManager.isNetworkAvailable()) SyncState.SYNCING else SyncState.PENDING_WRITE
                        }
                        isFromCache -> SyncState.CACHED
                        else -> SyncState.SYNCED
                    }

                    Task(
                        id = doc.id,
                        title = title,
                        isCompleted = isCompleted,
                        profileOwner = profileOwner,
                        createdAt = createdAt,
                        completedAt = completedAt,
                        createdByUid = doc.getString("createdByUid") ?: "",
                        createdByName = createdBy,
                        completedByUid = doc.getString("completedByUid"),
                        completedByName = completedBy,
                        familyId = familyId,
                        syncState = syncState
                    )
                } catch (e: Exception) {
                    null
                }
            }
            tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    private fun getAllTasksFromFirestore(familyId: String): Flow<List<Task>> {
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("families")
            .document(familyId)
            .collection("tasks")

        val firestoreFlow = callbackFlow<QuerySnapshot> {
            val listener = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore snapshot listener error for all tasks", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot)
                }
            }
            awaitClose { listener.remove() }
        }

        return combine(firestoreFlow, LocalSyncTracker.syncStates) { snapshot, syncStateMap ->
            val isFromCache = snapshot.metadata.isFromCache
            val tasks = snapshot.documents.mapNotNull { doc ->
                try {
                    val title = doc.getString("title") ?: ""
                    val isCompleted = doc.getBoolean("completed") ?: false
                    val profileOwner = doc.getString("profileOwner") ?: "GENERAL"
                    val createdBy = doc.getString("createdBy") ?: "Unknown"
                    val completedBy = doc.getString("completedBy")
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val completedAt = doc.getLong("completedAt")
                    
                    val hasPendingWrites = doc.metadata.hasPendingWrites()
                    
                    // Determine Sync State: Check local tracker override first, fallback to metadata
                    val syncState = syncStateMap[doc.id] ?: when {
                        hasPendingWrites -> {
                            if (FirebaseSyncManager.isNetworkAvailable()) SyncState.SYNCING else SyncState.PENDING_WRITE
                        }
                        isFromCache -> SyncState.CACHED
                        else -> SyncState.SYNCED
                    }

                    Task(
                        id = doc.id,
                        title = title,
                        isCompleted = isCompleted,
                        profileOwner = profileOwner,
                        createdAt = createdAt,
                        completedAt = completedAt,
                        createdByUid = doc.getString("createdByUid") ?: "",
                        createdByName = createdBy,
                        completedByUid = doc.getString("completedByUid"),
                        completedByName = completedBy,
                        familyId = familyId,
                        syncState = syncState
                    )
                } catch (e: Exception) {
                    null
                }
            }
            tasks.sortedByDescending { it.createdAt }
        }
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
