package com.example.ui.screens.login

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("PaperBundlePrefs", Context.MODE_PRIVATE)

    val curLanguage = MutableStateFlow(
        Language.valueOf(prefs.getString("Language", Language.EN.name) ?: Language.EN.name)
    )

    val authLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)

    fun loginWithGoogleProfile(
        context: Context,
        name: String,
        email: String,
        photoUrl: String,
        idToken: String?,
        onSuccess: () -> Unit
    ) {
        Log.d("PAPER_BUNDLE", "LoginViewModel: loginWithGoogleProfile() called: name=$name, email=$email")
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
}

class LoginViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
