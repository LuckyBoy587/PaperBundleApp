package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
import com.example.ui.UpdateViewModel
import com.example.ui.UpdateViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "PAPER_BUNDLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate() started")
        enableEdgeToEdge()

        val app = application as TaskApplication
        val repository = app.repository
        val updateRepository = app.updateRepository
        Log.d(TAG, "MainActivity: onCreate: TaskRepository loaded successfully")
        val viewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(application, repository)
        )[TaskViewModel::class.java]

        val updateViewModel = ViewModelProvider(
            this,
            UpdateViewModelFactory(application, updateRepository)
        )[UpdateViewModel::class.java]

        handleIntent(intent, viewModel)

        val mainActivityUI = MainActivityUI(viewModel, updateViewModel)
        setContent {
            MyApplicationTheme {
                mainActivityUI.Render()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "MainActivity: onNewIntent() called")
        val app = application as TaskApplication
        val repository = app.repository
        val viewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(application, repository)
        )[TaskViewModel::class.java]
        handleIntent(intent, viewModel)
    }

    private fun handleIntent(intent: Intent, viewModel: TaskViewModel) {
        val openAddTask = intent.getBooleanExtra("open_add_task", false)
        Log.d(TAG, "MainActivity: handleIntent: openAddTask=$openAddTask")
        if (openAddTask) {
            viewModel.triggerAddTaskDialog.value = true
        }
    }
}
