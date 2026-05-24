package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import com.example.MainActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.R
import com.example.data.Task
import com.example.data.TaskDatabase
import com.example.data.TaskRepository
import androidx.glance.unit.ColorProvider
import com.example.util.FirebaseSyncManager

class TodoWidget : GlanceAppWidget() {

    companion object {
        val openAddTaskKey = ActionParameters.Key<Boolean>("open_add_task")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TaskDatabase.getDatabase(context)
        val repository = TaskRepository(database.taskDao)

        // Read active profile from app shared preferences
        val prefs = context.getSharedPreferences("PaperBundlePrefs", Context.MODE_PRIVATE)
        val profile = prefs.getString("Profile", "") ?: ""

        // Fetch task stream reactively via Room database
        val tasksFlow = repository.getTasksForProfile(profile)

        provideContent {
            GlanceTheme {
                val tasks by tasksFlow.collectAsState(initial = emptyList())
                val pendingTasks = tasks.filter { !it.isCompleted }

                TodoWidgetContent(
                    pendingTasks = pendingTasks
                )
            }
        }
    }

    @Composable
    private fun TodoWidgetContent(
        pendingTasks: List<Task>
    ) {
        val countText = if (pendingTasks.size == 1) "1 pending" else "${pendingTasks.size} pending"

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header Bar - Clickable to open app homepage
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Bundle",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Count Badge
                    Box(
                        modifier = GlanceModifier
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Plus Button inside a circular themed background
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(16.dp)
                            .clickable(
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(openAddTaskKey to true)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_add_task),
                            contentDescription = "Add Task",
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                        )
                    }

                    // Refresh Button inside a circular themed background
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(16.dp)
                            .clickable(actionRunCallback<RefreshTasksAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_refresh),
                            contentDescription = "Refresh Tasks",
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Divider line
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GlanceTheme.colors.outline)
                ) {}

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Dynamic UI state
                if (pendingTasks.isEmpty()) {
                    // Empty Motivational State
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_task_empty),
                            contentDescription = "Empty state illustration",
                            modifier = GlanceModifier.size(48.dp)
                        )
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = "All Caught Up!",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Your paper bundle is clear! 🎉",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                } else {
                    // Scrollable list of pending todos
                    LazyColumn(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                    ) {
                        items(pendingTasks) { task ->
                            TaskItemRow(task = task)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskItemRow(task: Task) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Task Text
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = task.title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Interactive circular check button
                Image(
                    provider = ImageProvider(R.drawable.ic_check_circle_outline),
                    contentDescription = "Complete Task",
                    modifier = GlanceModifier
                        .size(26.dp)
                        .clickable(
                            onClick = actionRunCallback<CompleteTaskAction>(
                                actionParametersOf(CompleteTaskAction.taskIdKey to task.id)
                            )
                        ),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
            }

            // Divider between items
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlanceTheme.colors.outline)
            ) {}
        }
    }
}
