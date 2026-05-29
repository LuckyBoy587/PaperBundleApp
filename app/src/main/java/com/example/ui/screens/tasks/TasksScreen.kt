package com.example.ui.screens.tasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SyncState
import com.example.data.Task
import com.example.ui.components.*
import com.example.util.FamilyMember
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import com.example.util.LocalizedStrings
import com.example.util.UserSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    session: UserSession,
    onNavigateToFamily: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val curProfile by viewModel.curProfile.collectAsState()
    val activeTasks by viewModel.tasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val triggerAddTask by viewModel.triggerAddTaskDialog.collectAsState()

    val isNetworkAvailable = remember { mutableStateOf(FirebaseSyncManager.isNetworkAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            isNetworkAvailable.value = FirebaseSyncManager.isNetworkAvailable()
            delay(3000)
        }
    }

    val hasPendingWrites = remember(activeTasks) {
        activeTasks.any { it.syncState == SyncState.PENDING_WRITE || it.syncState == SyncState.SYNCING }
    }
    
    var isAddingTask by remember { mutableStateOf(false) }

    LaunchedEffect(triggerAddTask) {
        if (triggerAddTask) {
            isAddingTask = true
            viewModel.triggerAddTaskDialog.value = false
        }
    }

    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var isCompletedSheetOpen by remember { mutableStateOf(false) }
    val collapsedCompletedMembers = remember { mutableStateMapOf<String, Boolean>() }

    // Speech-to-Text Setup
    var voiceTextForInput by remember { mutableStateOf("") }
    var speechTriggerId by remember { mutableStateOf(0) }
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                voiceTextForInput = spokenText
                speechTriggerId++
            } else {
                Toast.makeText(
                    context,
                    LocalizedStrings.get("voice_speech_not_understood", language),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val startSpeechRecognition = {
        val locale = if (language == Language.TA) "ta-IN" else "en-US"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                LocalizedStrings.get("enter_task_hint", language)
            )
        }
        try {
            isListening = true
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(
                context,
                LocalizedStrings.get("voice_not_supported", language),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val activeTasksList = remember(allTasks) {
        allTasks.filter { !it.isCompleted }
    }

    val sortedMembers = remember(familyMembers, session) {
        val list = familyMembers.toMutableList()
        val currentUserIndex = list.indexOfFirst { it.uid == session.uid }
        if (currentUserIndex != -1) {
            val currentUser = list.removeAt(currentUserIndex)
            list.add(0, currentUser)
        } else {
            list.add(0, FamilyMember(
                uid = session.uid,
                name = session.name,
                email = session.email,
                photoUrl = session.photoUrl
            ))
        }
        list
    }

    val activeMember = familyMembers.find { it.uid == curProfile }
    val initialLetter = (activeMember?.name ?: session.name).take(1).uppercase()

    StitchFixedPage {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
                Spacer(modifier = Modifier.height(8.dp))

                // B. Scrollable Dynamic Tasks List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    StitchLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(sortedMembers, key = { it.uid }) { member ->
                            val memberTasks = remember(activeTasksList, member.uid) {
                                activeTasksList.filter { it.profileOwner == member.uid }
                            }
                            Box(modifier = Modifier.animateItem()) {
                                MemberTodoBox(
                                    member = member,
                                    isCurrentUser = member.uid == session.uid,
                                    tasks = memberTasks,
                                    onToggleTask = { task -> viewModel.toggleTaskComplete(task) },
                                    onDeleteTask = { task -> taskToDelete = task },
                                    onAddTaskClick = {
                                        viewModel.setProfile(member.uid)
                                        isAddingTask = true
                                    }
                                )
                            }
                        }
                    }
                }

                // C. Completed Tasks Pull-up Bar
                val completedTasksCount = remember(allTasks) { allTasks.count { it.isCompleted } }
                if (completedTasksCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.05f),
                                spotColor = Color.Black.copy(alpha = 0.07f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .clickable { isCompletedSheetOpen = true }
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    if (delta < -3f) {
                                        isCompletedSheetOpen = true
                                    }
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completed icon",
                                        tint = StitchGreen500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Completed Tasks",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSlate800
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$completedTasksCount",
                                        color = StitchIndigo,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "▲",
                                    fontSize = 10.sp,
                                    color = StitchSlate500
                                )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

        // D. Sliding Completed Tasks Sheet
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isCompletedSheetOpen) 0.4f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "scrimAlpha"
        )

        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isCompletedSheetOpen = false
                    }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val screenHeightPx = constraints.maxHeight.toFloat()
            val expandedOffsetPx = screenHeightPx * 0.25f
            val collapsedOffsetPx = screenHeightPx

            val coroutineScope = rememberCoroutineScope()
            val sheetOffsetY = remember { Animatable(collapsedOffsetPx) }

            LaunchedEffect(isCompletedSheetOpen, collapsedOffsetPx, expandedOffsetPx) {
                if (isCompletedSheetOpen) {
                    sheetOffsetY.animateTo(
                        targetValue = expandedOffsetPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                } else {
                    sheetOffsetY.animateTo(
                        targetValue = collapsedOffsetPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }

            if (sheetOffsetY.value < collapsedOffsetPx) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(with(density) { (constraints.maxHeight * 0.75f).toDp() })
                        .offset {
                            androidx.compose.ui.unit.IntOffset(0, (sheetOffsetY.value - expandedOffsetPx).toInt())
                        }
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        if (delta > 0f) {
                                            coroutineScope.launch {
                                                sheetOffsetY.snapTo((sheetOffsetY.value + delta).coerceIn(expandedOffsetPx, collapsedOffsetPx))
                                            }
                                        }
                                    },
                                    onDragStopped = { velocity ->
                                        if (sheetOffsetY.value > expandedOffsetPx + 120f || velocity > 400f) {
                                            isCompletedSheetOpen = false
                                        } else {
                                            coroutineScope.launch {
                                                sheetOffsetY.animateTo(
                                                    targetValue = expandedOffsetPx,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                        ) {
                            // Drag Handle
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(5.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(100.dp))
                                )
                            }

                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completed Tasks",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSlate800
                                )

                                IconButton(
                                    onClick = { isCompletedSheetOpen = false },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", fontSize = 18.sp, color = StitchSlate500, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(StitchBorder)
                        )

                        // Completed list
                        val completedTasksList = remember(allTasks) { allTasks.filter { it.isCompleted } }
                        if (completedTasksList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No completed tasks yet.",
                                    color = StitchSlate500,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                            ) {
                                items(sortedMembers, key = { it.uid }) { member ->
                                    val memberCompletedTasks = remember(completedTasksList, member.uid) {
                                        completedTasksList.filter { it.profileOwner == member.uid }
                                    }
                                    val isExpanded = collapsedCompletedMembers[member.uid] ?: true
                                    Box(modifier = Modifier.animateItem()) {
                                        MemberTodoBox(
                                            member = member,
                                            isCurrentUser = member.uid == session.uid,
                                            tasks = memberCompletedTasks,
                                            showAddButton = false,
                                            isExpanded = isExpanded,
                                            onToggleExpand = {
                                                collapsedCompletedMembers[member.uid] = !isExpanded
                                            },
                                            onToggleTask = { task -> viewModel.toggleTaskComplete(task) },
                                            onDeleteTask = { task -> taskToDelete = task }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (isAddingTask) {
        AddTaskDialog(
            language = language,
            profile = curProfile,
            voiceText = voiceTextForInput,
            speechTriggerId = speechTriggerId,
            isListening = isListening,
            onVoiceClick = startSpeechRecognition,
            onDismiss = {
                isAddingTask = false
                voiceTextForInput = ""
            },
            onSave = { title ->
                viewModel.addTask(title)
                isAddingTask = false
                voiceTextForInput = ""
            }
        )
    }

    // Delete Task Dialog
    taskToDelete?.let { task ->
        DeleteConfirmDialog(
            language = language,
            onDismiss = { taskToDelete = null },
            onConfirm = {
                viewModel.deleteTask(task.id)
                taskToDelete = null
            }
        )
    }
}



@Composable
fun MemberTaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    isNetworkAvailable: Boolean,
    isFirebaseInitialized: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (task.isCompleted) StitchIndigo else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                    .background(if (task.isCompleted) StitchIndigo else Color.Transparent, CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) StitchSlate500 else StitchSlate800,
                    style = TextStyle(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Creator badge
                    if (task.createdByUid.isNotEmpty() && task.createdByUid != task.profileOwner) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "By ${task.createdByName.substringBefore(" ")}",
                                color = StitchSlate500,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    // Sync State Badge
                    val stateColor = when (task.syncState) {
                        SyncState.SYNCED -> StitchGreen500
                        SyncState.CACHED -> StitchSlate500
                        SyncState.PENDING_WRITE -> if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706)
                        SyncState.SYNCING -> StitchIndigo
                        SyncState.ERROR -> MaterialTheme.colorScheme.error
                    }
                    val stateIcon = when (task.syncState) {
                        SyncState.SYNCED -> Icons.Default.CheckCircle
                        SyncState.CACHED -> Icons.Default.Info
                        SyncState.PENDING_WRITE -> Icons.Default.Refresh
                        SyncState.SYNCING -> Icons.Default.Refresh
                        SyncState.ERROR -> Icons.Default.Warning
                    }
                    val stateText = when (task.syncState) {
                        SyncState.SYNCED -> "Synced"
                        SyncState.CACHED -> "Cached"
                        SyncState.PENDING_WRITE -> "Pending"
                        SyncState.SYNCING -> "Syncing"
                        SyncState.ERROR -> "Error"
                    }

                    val rotation = if (task.syncState == SyncState.SYNCING) {
                        val infiniteTransition = rememberInfiniteTransition(label = "SyncingRotRow")
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "RotAngleRow"
                        )
                        angle
                    } else 0f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = stateIcon,
                            contentDescription = stateText,
                            tint = stateColor,
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                        Text(
                            text = stateText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = stateColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Trash Button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun MemberTodoBox(
    member: FamilyMember,
    isCurrentUser: Boolean,
    tasks: List<Task>,
    showAddButton: Boolean = true,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTaskClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = remember(member.uid, isDark) { getMemberColor(member.uid, isDark) }
    val isNetworkAvailable = FirebaseSyncManager.isNetworkAvailable()
    val isFirebaseInitialized = FirebaseSyncManager.isFirebaseInitialized
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.07f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, StitchBorder), RoundedCornerShape(24.dp))
            .animateContentSize()
    ) {
        // Left color indicator bar
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(1).uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = if (isCurrentUser) "${member.name} (You)" else member.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate800
                        )
                        Text(
                            text = if (tasks.isEmpty()) "All caught up" else "${tasks.size} tasks remaining",
                            fontSize = 11.sp,
                            color = StitchSlate500
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showAddButton) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onAddTaskClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add task",
                                tint = StitchIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (onToggleExpand != null) {
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "▼" else "▲",
                                fontSize = 10.sp,
                                color = StitchSlate500
                            )
                        }
                    }
                }
            }

            if (isExpanded && tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tasks.forEachIndexed { index, task ->
                        MemberTaskRow(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onDelete = { onDeleteTask(task) },
                            isNetworkAvailable = isNetworkAvailable,
                            isFirebaseInitialized = isFirebaseInitialized
                        )
                        if (index < tasks.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    language: Language,
    profile: String,
    voiceText: String,
    speechTriggerId: Int,
    isListening: Boolean = false,
    onVoiceClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(voiceText, speechTriggerId) {
        if (voiceText.isNotBlank()) {
            textState = voiceText
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val micColor = if (isListening) StitchRed500 else StitchIndigo
    val micBg = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.5f else 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 700 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }
    val scale by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )
    val alphaVal by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = alphaVal
                }
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = LocalizedStrings.get("add_task", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800
                )

                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text(text = LocalizedStrings.get("enter_task_hint", language), fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(micBg)
                            .clickable { onVoiceClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(micColor.copy(alpha = pulseAlpha), CircleShape)
                                .border(1.dp, micColor, CircleShape)
                        )
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val w = size.width
                            val h = size.height
                            drawRoundRect(
                                color = micColor,
                                topLeft = Offset(w * 0.35f, h * 0.05f),
                                size = Size(w * 0.3f, h * 0.52f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawArc(
                                color = micColor,
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(w * 0.18f, h * 0.32f),
                                size = Size(w * 0.64f, h * 0.42f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawLine(
                                color = micColor,
                                start = Offset(w * 0.5f, h * 0.74f),
                                end = Offset(w * 0.5f, h * 0.96f),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = micColor,
                                start = Offset(w * 0.32f, h * 0.96f),
                                end = Offset(w * 0.68f, h * 0.96f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Text(
                        text = if (isListening)
                            if (language == Language.TA) "கேட்கிறேன்…" else "Listening…"
                        else
                            LocalizedStrings.get("voice_hint", language),
                        fontSize = 12.sp,
                        color = if (isListening) StitchRed500 else StitchSlate500,
                        fontWeight = if (isListening) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = LocalizedStrings.get("cancel", language), color = StitchSlate500, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (textState.isNotBlank()) {
                                onSave(textState)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(text = LocalizedStrings.get("save", language), color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
