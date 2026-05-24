package com.example.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.data.TaskDatabase
import com.example.util.FirebaseSyncManager

class RefreshTasksAction : ActionCallback {
    companion object {
        private const val TAG = "PAPER_BUNDLE_WIDGET"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "RefreshTasksAction: Manual refresh button clicked from widget.")
        try {
            val database = TaskDatabase.getDatabase(context)
            
            if (FirebaseSyncManager.isFirebaseInitialized) {
                val session = FirebaseSyncManager.currentUserSession.value
                if (session?.familyId != null) {
                    Log.d(TAG, "RefreshTasksAction: Family session active. Restarting sync to pull latest tasks...")
                    FirebaseSyncManager.startSyncing(database.taskDao)
                } else {
                    Log.d(TAG, "RefreshTasksAction: No family session active. Only local refresh.")
                }
            } else {
                Log.w(TAG, "RefreshTasksAction: Firebase not initialized. Only local refresh.")
            }

            // Trigger Glance widget redraw immediately
            TodoWidget().updateAll(context)
            Log.d(TAG, "RefreshTasksAction: Widget redraw triggered.")
        } catch (e: Exception) {
            Log.e(TAG, "RefreshTasksAction: Exception during tasks refresh", e)
        }
    }
}
