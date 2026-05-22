package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import com.example.util.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TaskViewModel(
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

    // Loading & error status for sync and setup actions
    val authLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)

    init {
        // Collect user session changes to automatically bind/unbind Firestore listener
        viewModelScope.launch {
            FirebaseSyncManager.currentUserSession.collect { session ->
                if (session?.familyId != null) {
                    FirebaseSyncManager.startSyncing(repository.taskDao)
                    // If no active profile is selected, or if the current profile is still AMMA/APPA,
                    // default to the user's own UID
                    val savedProfile = prefs.getString("Profile", "") ?: ""
                    if (savedProfile.isEmpty() || savedProfile == "AMMA" || savedProfile == "APPA") {
                        val defaultProfile = session.uid
                        curProfile.value = defaultProfile
                        prefs.edit().putString("Profile", defaultProfile).apply()
                    }
                } else {
                    FirebaseSyncManager.stopSyncing()
                }
            }
        }

        viewModelScope.launch {
            FirebaseSyncManager.familyMembers.collect { members ->
                // If current profile is not in the list of family members, and members list is not empty
                if (members.isNotEmpty() && members.none { it.uid == curProfile.value }) {
                    val defaultProfile = members.first().uid
                    curProfile.value = defaultProfile
                    prefs.edit().putString("Profile", defaultProfile).apply()
                }
            }
        }
    }

    // Pending tasks for current profile
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = curProfile
        .flatMapLatest { profile ->
            repository.getTasksForProfile(profile)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setLanguage(language: Language) {
        curLanguage.value = language
        prefs.edit().putString("Language", language.name).apply()
    }

    fun setProfile(profile: String) {
        curProfile.value = profile
        prefs.edit().putString("Profile", profile).apply()
    }

    // Authentication Actions
    fun loginWithGoogleProfile(
        context: Context,
        name: String,
        email: String,
        photoUrl: String,
        idToken: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            FirebaseSyncManager.authenticateWithGoogle(
                context = context,
                idToken = idToken,
                profileChoiceName = name,
                profileChoiceEmail = email,
                profileChoicePhoto = photoUrl
            ) { success, errorMsg ->
                authLoading.value = false
                if (success) {
                    onSuccess()
                } else {
                    authError.value = errorMsg
                }
            }
        }
    }

    fun handleCreateFamily(context: Context, familyName: String, onSuccess: () -> Unit) {
        if (familyName.isBlank()) return
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            FirebaseSyncManager.createFamily(context, familyName.trim()) { success, errorMsg ->
                authLoading.value = false
                if (success) {
                    onSuccess()
                } else {
                    authError.value = errorMsg
                }
            }
        }
    }

    fun handleJoinFamily(context: Context, inviteCode: String, onSuccess: () -> Unit) {
        if (inviteCode.isBlank()) return
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            FirebaseSyncManager.joinFamily(context, inviteCode.trim()) { success, errorMsg ->
                authLoading.value = false
                if (success) {
                    onSuccess()
                } else {
                    authError.value = errorMsg
                }
            }
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            FirebaseSyncManager.saveSession(context, null)
        }
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
            
            // Sync to Firebase if a Family board is linked
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskAdditionOrUpdate(task)
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
            
            // Sync status to Firestore
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskAdditionOrUpdate(updatedTask)
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
            
            // Sync deletion to Firestore
            val session = currentUserSession.value
            if (session?.familyId != null) {
                FirebaseSyncManager.pushTaskDeletion(id)
            }
        }
    }
}

class TaskViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
