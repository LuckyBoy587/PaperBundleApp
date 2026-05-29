package com.example.ui.screens.family

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import kotlinx.coroutines.flow.MutableStateFlow

class FamilyViewModel(
    application: Application
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
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
