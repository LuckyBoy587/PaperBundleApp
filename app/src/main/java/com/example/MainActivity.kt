package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.tasks.TasksViewModel
import com.example.ui.screens.tasks.TasksViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "PAPER_BUNDLE"
    }

    private lateinit var tasksViewModel: TasksViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity: onCreate() started")
        enableEdgeToEdge()

        val app = application as TaskApplication
        val repository = app.repository
        Log.d(TAG, "MainActivity: onCreate: TaskRepository loaded successfully")

        // Scope TasksViewModel to the Activity to handle widgets / external intents
        tasksViewModel = ViewModelProvider(
            this,
            TasksViewModelFactory(application, repository)
        )[TasksViewModel::class.java]

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val mainActivityUI = MainActivityUI()
                mainActivityUI.Render()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG, "MainActivity: onNewIntent() called")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val openAddTask = intent.getBooleanExtra("open_add_task", false)
        Log.d(TAG, "MainActivity: handleIntent: openAddTask=$openAddTask")
        if (openAddTask) {
            tasksViewModel.triggerAddTaskDialog.value = true
        }
    }
}
