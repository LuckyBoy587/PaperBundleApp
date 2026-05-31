package com.example.ui.screens.tasks

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TasksViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("PaperBundlePrefs", Context.MODE_PRIVATE)

    // Current language (saved to Prefs)
    val curLanguage = MutableStateFlow(
        Language.valueOf(prefs.getString("Language", Language.EN.name) ?: Language.EN.name)
    )

    // Selected profile owner (defaults to user's UID or empty)
    val curProfile = MutableStateFlow(
        prefs.getString("Profile", "") ?: ""
    )

    // Expose User Session State Flow directly from Sync Manager
    val currentUserSession = FirebaseSyncManager.currentUserSession

    // Expose family members State Flow directly from Sync Manager
    val familyMembers = FirebaseSyncManager.familyMembers

    // Trigger to open the add task dialog (e.g. from Widget + button click intent)
    val triggerAddTaskDialog = MutableStateFlow(false)

    init {
        Log.d("TasksViewModel", "TasksViewModel: initialized: curProfile=${curProfile.value}")
        // Collect user session changes to automatically bind/unbind Firestore listener
        viewModelScope.launch {
            FirebaseSyncManager.currentUserSession.collect { session ->
                if (session?.familyId != null) {
                    FirebaseSyncManager.startSyncing(repository.taskDao)
                    val savedProfile = prefs.getString("Profile", "") ?: ""
                    if (savedProfile.isEmpty() || savedProfile == "AMMA" || savedProfile == "APPA") {
                        val defaultProfile = session.uid
                        curProfile.value = defaultProfile
                        prefs.edit { putString("Profile", defaultProfile) }
                    }
                } else {
                    FirebaseSyncManager.stopSyncing()
                }
            }
        }

        viewModelScope.launch {
            FirebaseSyncManager.familyMembers.collect { members ->
                if (members.isNotEmpty() && members.none { it.uid == curProfile.value }) {
                    val defaultProfile = members.first().uid
                    curProfile.value = defaultProfile
                    prefs.edit { putString("Profile", defaultProfile) }
                }
            }
        }
    }

    // Pending tasks for current profile
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = combine(curProfile, currentUserSession) { profile, _ ->
        profile
    }.flatMapLatest { profile ->
        repository.getTasksForProfile(profile)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTasks: StateFlow<List<Task>> = currentUserSession
        .flatMapLatest { _ ->
            repository.getAllTasks()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setProfile(profile: String) {
        curProfile.value = profile
        prefs.edit { putString("Profile", profile) }
    }

    fun setLanguage(language: Language) {
        curLanguage.value = language
        prefs.edit { putString("Language", language.name) }
    }

    // Task Manipulations
    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val session = currentUserSession.value
            val taskId = UUID.randomUUID().toString()
            val task = Task(
                id = taskId,
                title = title.trim(),
                profileOwner = curProfile.value,
                isCompleted = false,
                createdByUid = session?.uid ?: "local_user",
                createdByName = session?.name ?: "Local User",
                familyId = session?.familyId ?: ""
            )
            repository.insertTask(task)
            FirebaseSyncManager.triggerWidgetUpdate()
            
            // Sync to Firebase if a Family board is linked
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskAdditionOrUpdate(task)
            }
        }
    }

    fun updateTaskTitle(task: Task, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val session = currentUserSession.value
            val updatedTask = task.copy(
                title = newTitle.trim()
            )
            repository.updateTask(updatedTask)
            FirebaseSyncManager.triggerWidgetUpdate()

            // Sync status to Firestore
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskAdditionOrUpdate(updatedTask)
            }
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            val session = currentUserSession.value
            val isNowCompleted = !task.isCompleted
            val updatedTask = task.copy(
                isCompleted = isNowCompleted,
                completedAt = if (isNowCompleted) System.currentTimeMillis() else null,
                completedByUid = if (isNowCompleted) (session?.uid ?: "local_user") else null,
                completedByName = if (isNowCompleted) (session?.name ?: "Local User") else null
            )
            repository.updateTask(updatedTask)
            FirebaseSyncManager.triggerWidgetUpdate()
            
            // Sync status to Firestore
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskAdditionOrUpdate(updatedTask)
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
            FirebaseSyncManager.triggerWidgetUpdate()
            
            // Sync deletion to Firestore
            val session = currentUserSession.value
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskDeletion(id)
            }
        }
    }
}

class TasksViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
