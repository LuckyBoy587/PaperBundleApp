package com.example.ui.screens.family

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FamilyViewModel(
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

    // Expose family members State Flow directly from Sync Manager
    val familyMembers = FirebaseSyncManager.familyMembers

    // Expose all tasks for tracking active/completed counts per member
    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
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
}

class FamilyViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
