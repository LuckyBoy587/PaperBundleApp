package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
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
        Log.d(TAG, "MainActivity: onCreate: TaskRepository loaded successfully")
        val viewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(application, repository)
        )[TaskViewModel::class.java]

        val mainActivityUI = MainActivityUI(viewModel)
        setContent {
            MyApplicationTheme {
                mainActivityUI.Render()
            }
        }
    }
}
