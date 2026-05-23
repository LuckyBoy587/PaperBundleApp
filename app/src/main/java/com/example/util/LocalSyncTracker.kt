package com.example.util

import com.example.data.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A local event storage and synchronization state tracker.
 * It stores immediate, optimistic sync overrides (such as SYNCING, SYNCED, or ERROR)
 * whenever a local operation is performed. The Repository combines these overrides
 * with Firestore's background metadata to ensure the UI updates instantly and perfectly.
 */
object LocalSyncTracker {
    private val _syncStates = MutableStateFlow<Map<String, SyncState>>(emptyMap())
    val syncStates: StateFlow<Map<String, SyncState>> get() = _syncStates

    /**
     * Updates the local sync state override for a task.
     */
    fun updateSyncState(taskId: String, state: SyncState) {
        synchronized(this) {
            _syncStates.value = _syncStates.value + (taskId to state)
        }
    }

    /**
     * Clears any local override, allowing Firestore's native metadata to take over.
     */
    fun clearSyncState(taskId: String) {
        synchronized(this) {
            _syncStates.value = _syncStates.value - taskId
        }
    }

    /**
     * Clears all tracked sync states.
     */
    fun clearAll() {
        synchronized(this) {
            _syncStates.value = emptyMap()
        }
    }
}
