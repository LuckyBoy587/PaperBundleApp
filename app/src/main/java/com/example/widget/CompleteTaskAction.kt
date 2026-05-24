package com.example.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.data.TaskDatabase
import com.example.data.TaskRepository
import com.example.util.FirebaseSyncManager
import kotlinx.coroutines.flow.first

class CompleteTaskAction : ActionCallback {
    companion object {
        private const val TAG = "PAPER_BUNDLE_WIDGET"
        val taskIdKey = ActionParameters.Key<String>("task_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        Log.d(TAG, "CompleteTaskAction: Tapped complete button for task: $taskId")

        try {
            // 1. Initialize database and repository
            val database = TaskDatabase.getDatabase(context)
            val repository = TaskRepository(database.taskDao)

            // 2. Fetch all local tasks and find the matching task
            val tasks = database.taskDao.getAllTasks().first()
            val task = tasks.find { it.id == taskId }

            if (task != null) {
                // 3. Create a completed copy of the task
                val session = FirebaseSyncManager.currentUserSession.value
                val updatedTask = task.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    completedByUid = session?.uid ?: "local_user",
                    completedByName = session?.name ?: "Local User"
                )

                // 4. Save to Room database
                repository.updateTask(updatedTask)
                Log.d(TAG, "CompleteTaskAction: Room DB updated successfully for task: ${task.title}")

                // 5. If sync is enabled, push the update to Firestore
                if (session?.familyId != null) {
                    Log.d(TAG, "CompleteTaskAction: Family session active. Syncing update to Firestore...")
                    FirebaseSyncManager.pushTaskAdditionOrUpdate(updatedTask)
                }
            } else {
                Log.w(TAG, "CompleteTaskAction: Task not found in local database: $taskId")
            }

            // 6. Request widget update immediately
            TodoWidget().updateAll(context)
            Log.d(TAG, "CompleteTaskAction: Widget refresh triggered.")
        } catch (e: Exception) {
            Log.e(TAG, "CompleteTaskAction: Exception in CompleteTaskAction", e)
        }
    }
}
