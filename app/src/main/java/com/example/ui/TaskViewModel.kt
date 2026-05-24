package com.example.ui

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PAPER_BUNDLE"
    }

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

    // Trigger to open the add task dialog (e.g. from Widget + button click intent)
    val triggerAddTaskDialog = MutableStateFlow(false)

    init {
        Log.d(TAG, "TaskViewModel: initialized: curLanguage=${curLanguage.value}, curProfile=${curProfile.value}")
        // Collect user session changes to automatically bind/unbind Firestore listener
        viewModelScope.launch {
            FirebaseSyncManager.currentUserSession.collect { session ->
                Log.d(TAG, "TaskViewModel: currentUserSession collected update: familyId=${session?.familyId}, uid=${session?.uid}")
                if (session?.familyId != null) {
                    FirebaseSyncManager.startSyncing(repository.taskDao)
                    // If no active profile is selected, or if the current profile is still AMMA/APPA,
                    // default to the user's own UID
                    val savedProfile = prefs.getString("Profile", "") ?: ""
                    if (savedProfile.isEmpty() || savedProfile == "AMMA" || savedProfile == "APPA") {
                        val defaultProfile = session.uid
                        Log.d(TAG, "TaskViewModel: Defaulting active profile to user UID: $defaultProfile")
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
                Log.d(TAG, "TaskViewModel: familyMembers collected update: size=${members.size}")
                // If current profile is not in the list of family members, and members list is not empty
                if (members.isNotEmpty() && members.none { it.uid == curProfile.value }) {
                    val defaultProfile = members.first().uid
                    Log.d(TAG, "TaskViewModel: Current profile not in family members list. Defaulting profile to: $defaultProfile")
                    curProfile.value = defaultProfile
                    prefs.edit { putString("Profile", defaultProfile) }
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
        Log.d(TAG, "TaskViewModel: setLanguage() called: language=$language")
        curLanguage.value = language
        prefs.edit { putString("Language", language.name) }
    }

    fun setProfile(profile: String) {
        Log.d(TAG, "TaskViewModel: setProfile() called: profile=$profile")
        curProfile.value = profile
        prefs.edit { putString("Profile", profile) }
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
        Log.d(TAG, "TaskViewModel: loginWithGoogleProfile() called: name=$name, email=$email, hasIdToken=${idToken != null}")
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
                Log.d(TAG, "TaskViewModel: loginWithGoogleProfile callback: success=$success, errorMsg=$errorMsg")
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
        Log.d(TAG, "TaskViewModel: logout() called")
        viewModelScope.launch {
            FirebaseSyncManager.saveSession(context, null)
        }
    }

    // Task Manipulations
    fun addTask(title: String) {
        if (title.isBlank()) {
            Log.w(TAG, "TaskViewModel: addTask() rejected: title is blank")
            return
        }
        Log.d(TAG, "TaskViewModel: addTask() called: title='$title', curProfile=${curProfile.value}")
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
            Log.d(TAG, "TaskViewModel: addTask: local insert complete for taskId=${task.id}")
            FirebaseSyncManager.triggerWidgetUpdate()
            
            // Sync to Firebase if a Family board is linked
            if (session?.familyId != null) {
                Log.d(TAG, "TaskViewModel: addTask: Pushing new task to Firestore...")
                FirebaseSyncManager.pushTaskAdditionOrUpdate(task)
            }
        }
    }

    fun toggleTaskComplete(task: Task) {
        Log.d(TAG, "TaskViewModel: toggleTaskComplete() called: taskId=${task.id}, title='${task.title}', currentCompleted=${task.isCompleted}")
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
            Log.d(TAG, "TaskViewModel: toggleTaskComplete: local update complete to isCompleted=$isNowCompleted")
            FirebaseSyncManager.triggerWidgetUpdate()
            
            // Sync status to Firestore
            if (session?.familyId != null) {
                Log.d(TAG, "TaskViewModel: toggleTaskComplete: Pushing task update to Firestore...")
                FirebaseSyncManager.pushTaskAdditionOrUpdate(updatedTask)
            }
        }
    }

    fun deleteTask(id: String) {
        Log.d(TAG, "TaskViewModel: deleteTask() called: id=$id")
        viewModelScope.launch {
            repository.deleteTask(id)
            Log.d(TAG, "TaskViewModel: deleteTask: local deletion complete")
            FirebaseSyncManager.triggerWidgetUpdate()
            
            // Sync deletion to Firestore
            val session = currentUserSession.value
            if (session?.familyId != null) {
                Log.d(TAG, "TaskViewModel: deleteTask: Pushing task deletion to Firestore...")
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
