package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Task
import com.example.ui.TaskViewModel
import com.example.util.Language
import com.example.util.LocalizedStrings
import com.example.util.UserSession
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlin.math.max

// Stitch Dashboard Colors
val StitchBg = Color(0xFFF8F9FE)
val StitchIndigo = Color(0xFF6366F1)
val StitchPurple = Color(0xFFA855F7)
val StitchSlate800 = Color(0xFF1E293B)
val StitchSlate500 = Color(0xFF64748B)
val StitchGray100 = Color(0xFFF1F5F9)
val StitchRed50 = Color(0xFFFEF2F2)
val StitchRed500 = Color(0xFFEF4444)
val StitchGreen50 = Color(0xFFF0FDF4)
val StitchGreen500 = Color(0xFF22C55E)
val StitchBlue50 = Color(0xFFEFF6FF)
val StitchBlue500 = Color(0xFF3B82F6)

class MainActivityUI(private val viewModel: TaskViewModel) {
    @Composable
    fun Render() {
        MainScreen(viewModel = viewModel)
    }
}

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val userSession by viewModel.currentUserSession.collectAsState()

    LaunchedEffect(userSession) {
        Log.d("PAPER_BUNDLE", "MainActivityUI: MainScreen session update: uid=${userSession?.uid}")
    }

    when (val session = userSession) {
        null -> {
            GoogleLoginScreen(viewModel)
        }
        else -> {
            SharedFamilyBoardScreen(viewModel = viewModel, session = session)
        }
    }
}

@Composable
fun GoogleLoginScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            val name = account.displayName ?: "User"
            val email = account.email ?: ""
            val photoUrl = account.photoUrl?.toString() ?: ""
            
            viewModel.loginWithGoogleProfile(context, name, email, photoUrl, idToken) {
                Toast.makeText(context, "Logged in securely", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Sign-In failed with status code: ${e.statusCode}", e)
            val friendlyError = when (e.statusCode) {
                7 -> "Network error. Please check your internet connection."
                10 -> "Developer configuration error (status code 10). Please verify signing certificate SHA-1 and Client ID match."
                12500 -> "Sign-in failed (status code 12500). Please check Google Play Services."
                12501 -> "Sign-in cancelled."
                else -> "Sign-In failed: ${e.localizedMessage ?: "Error code ${e.statusCode}"}"
            }
            viewModel.authError.value = friendlyError
        } catch (e: Exception) {
            Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Unexpected error during Sign-In", e)
            viewModel.authError.value = "Unexpected error: ${e.localizedMessage}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(32.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon / Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(colors = listOf(StitchIndigo, StitchPurple))),
                contentAlignment = Alignment.Center
            ) {
                Text("🏡", fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Baski Home",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = StitchSlate800
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedStrings.get("app_desc", language),
                fontSize = 14.sp,
                color = StitchSlate500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (authLoading) {
                CircularProgressIndicator(color = StitchIndigo)
            } else {
                Button(
                    onClick = {
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        )
                            .requestEmail()
                            .requestIdToken("676948202486-o9k8m2rkth5d70vr573c5jhmk706l5ce.apps.googleusercontent.com")
                            .requestProfile()
                            .build()
                        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo)
                ) {
                    Text(
                        text = LocalizedStrings.get("sign_in_google", language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            authError?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = err, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SharedFamilyBoardScreen(viewModel: TaskViewModel, session: UserSession) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val curProfile by viewModel.curProfile.collectAsState()
    val activeTasks by viewModel.tasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()

    var isAddingTask by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showProfileSelector by remember { mutableStateOf(false) }
    var taskFilter by remember { mutableStateOf("All") }

    // Speech-to-Text Setup
    var voiceTextForInput by remember { mutableStateOf("") }
    var speechTriggerId by remember { mutableStateOf(0) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                voiceTextForInput = spokenText
                speechTriggerId++
            }
        }
    }

    val startSpeechRecognition = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == Language.TA) "ta-IN" else "en-US")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech not supported", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredTasks = remember(activeTasks, taskFilter) {
        when (taskFilter) {
            "Pending" -> activeTasks.filter { !it.isCompleted }
            "Completed" -> activeTasks.filter { it.isCompleted }
            else -> activeTasks
        }
    }

    val activeMember = familyMembers.find { it.uid == curProfile }
    val initialLetter = (activeMember?.name ?: session.name).take(1).uppercase()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBg)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                MainHeader(
                    initial = initialLetter,
                    language = language,
                    onAvatarClick = { showProfileSelector = true },
                    onLanguageClick = {
                        viewModel.setLanguage(if (language == Language.EN) Language.TA else Language.EN)
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 86.dp) // Leave space for bottom nav
            ) {
                // A. Tasks Header with Filters (Fixed)
                TasksSectionHeader(
                    currentFilter = taskFilter,
                    onFilterChange = { taskFilter = it }
                )

                // B. Scrollable Dynamic Tasks List taking up remaining center part
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredTasks.isEmpty()) {
                        val activeMemberName = (activeMember?.name ?: "Member").substringBefore(" ")
                        val emptyMsg = when (taskFilter) {
                            "Completed" -> "No completed tasks yet."
                            "Pending" -> String.format(LocalizedStrings.get("no_pending_member", language), activeMemberName)
                            else -> "No tasks created yet."
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emptyMsg,
                                color = StitchSlate500,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    onToggle = { viewModel.toggleTaskComplete(task) },
                                    onDelete = { taskToDelete = task }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // D. Overview Statistics - fit above bottom nav (Fixed)
                BoardOverviewCard(
                    pendingCount = activeTasks.count { !it.isCompleted },
                    completedCount = activeTasks.count { it.isCompleted }
                )
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 88.dp, end = 24.dp)
                .size(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = StitchIndigo.copy(alpha = 0.4f),
                    spotColor = StitchIndigo.copy(alpha = 0.6f)
                )
                .background(Brush.linearGradient(colors = listOf(StitchPurple, StitchIndigo)), CircleShape)
                .clickable {
                    voiceTextForInput = ""
                    isAddingTask = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Floating Language/Status Switcher Badge removed

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .fillMaxWidth(0.92f)
                .height(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 1: Tasks
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable { /* Already on Tasks */ }
                ) {
                    HomeNavIcon(tint = StitchIndigo)
                    Text(
                        text = "Tasks",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchIndigo
                    )
                }

                // Item 2: Family
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable { showProfileSelector = true }
                ) {
                    FamilyNavIcon(tint = StitchSlate500)
                    Text(
                        text = "Family",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate500
                    )
                }

                // Item 3: Settings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Version 2026.1 - Designed by Kowshik B", Toast.LENGTH_LONG).show()
                    }
                ) {
                    SettingsNavIcon(tint = StitchSlate500)
                    Text(
                        text = "Settings",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate500
                    )
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
            onVoiceClick = startSpeechRecognition,
            onDismiss = { isAddingTask = false },
            onSave = { title ->
                viewModel.addTask(title)
                isAddingTask = false
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

    // Family Member Switcher Modal
    if (showProfileSelector) {
        FamilySelectorDialog(
            language = language,
            familyMembers = familyMembers,
            currentProfile = curProfile,
            onProfileSelect = {
                viewModel.setProfile(it)
                showProfileSelector = false
            },
            onDismiss = { showProfileSelector = false }
        )
    }
}

@Composable
fun MainHeader(
    initial: String,
    language: Language,
    onAvatarClick: () -> Unit,
    onLanguageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Letter Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StitchIndigo)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 12.sp,
                    color = StitchSlate500
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Baski Home",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate800
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "🏡", fontSize = 14.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Notification Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                    .clickable { /* No-op badge click */ },
                contentAlignment = Alignment.Center
            ) {
                BellNavIcon(tint = StitchSlate500)
                // Red badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }

            // Globe Language Toggle Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                    .clickable { onLanguageClick() },
                contentAlignment = Alignment.Center
            ) {
                GlobeNavIcon(tint = StitchSlate500)
            }
        }
    }
}

@Composable
fun TasksSectionHeader(
    currentFilter: String,
    onFilterChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Your Tasks",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StitchSlate800
        )
        
        Row(
            modifier = Modifier
                .background(StitchGray100, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filters = listOf("All", "Pending", "Completed")
            filters.forEach { filter ->
                val isSelected = currentFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onFilterChange(filter) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) StitchIndigo else StitchSlate500
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .background(Color.White, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (task.isCompleted) StitchIndigo else Color(0xFFC7D2FE), CircleShape)
                        .background(if (task.isCompleted) StitchIndigo else Color.Transparent, CircleShape)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        // Empty dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = task.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) StitchSlate500 else StitchSlate800,
                        style = TextStyle(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Creator Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFEEF2FF))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.createdByName,
                                color = StitchIndigo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Vertical bar divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(10.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        // Due Date Icon + Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CalendarIcon(tint = StitchSlate500)
                            Text(
                                text = "No due date",
                                fontSize = 10.sp,
                                color = StitchSlate500
                            )
                        }
                    }
                }
            }

            // Trash Button
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// StitchHeroBanner removed as Good Day banner is deleted.

@Composable
fun StitchProgressRing(progress: Float) {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.5.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2,
                (size.height - diameter) / 2
            )
            val arcSize = Size(diameter, diameter)
            
            // Draw background track
            drawArc(
                color = Color(0xFFF1F5F9),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            
            // Draw progress arc
            drawArc(
                color = StitchIndigo,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = StitchRed500
        )
    }
}

@Composable
fun StitchStatChip(
    label: String,
    value: String,
    tintColor: Color,
    bgColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = tintColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
        }
    }
}

@Composable
fun BoardOverviewCard(
    pendingCount: Int,
    completedCount: Int
) {
    val totalCount = pendingCount + completedCount
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFF1F5F9), shape = RoundedCornerShape(20.dp))
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.01f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Progress Ring
            StitchProgressRing(progress = progress)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 2. Info and Stats Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overview",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(StitchRed500, CircleShape))
                        Text(
                            text = "Pending: $pendingCount",
                            fontSize = 10.sp,
                            color = StitchSlate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Done
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(StitchGreen500, CircleShape))
                        Text(
                            text = "Done: $completedCount",
                            fontSize = 10.sp,
                            color = StitchSlate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Total
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(StitchBlue500, CircleShape))
                        Text(
                            text = "Total: $totalCount",
                            fontSize = 10.sp,
                            color = StitchSlate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Custom Canvas Vectors to maintain 100% dependency safety
// ---------------------------------------------------------

@Composable
fun BellNavIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.width / 24f
        val strokeWidth = 2.dp.toPx()
        
        drawCircle(
            color = tint,
            radius = 2.dp.toPx(),
            center = Offset(12f * scale, 4f * scale),
            style = Stroke(width = strokeWidth)
        )
        
        val bellPath = Path().apply {
            moveTo(6f * scale, 17f * scale)
            lineTo(18f * scale, 17f * scale)
            cubicTo(18f * scale, 14f * scale, 17f * scale, 9f * scale, 12f * scale, 9f * scale)
            cubicTo(7f * scale, 9f * scale, 6f * scale, 14f * scale, 6f * scale, 17f * scale)
        }
        drawPath(bellPath, color = tint, style = Stroke(width = strokeWidth))
        
        drawLine(
            color = tint,
            start = Offset(4f * scale, 17f * scale),
            end = Offset(20f * scale, 17f * scale),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(10f * scale, 17f * scale),
            size = Size(4f * scale, 4f * scale),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun GlobeNavIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.width / 24f
        val strokeWidth = 2.dp.toPx()
        val center = Offset(12f * scale, 12f * scale)
        val radius = 9f * scale
        
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        
        drawLine(
            color = tint,
            start = Offset(3f * scale, 12f * scale),
            end = Offset(21f * scale, 12f * scale),
            strokeWidth = strokeWidth
        )
        
        drawOval(
            color = tint,
            topLeft = Offset(8.5f * scale, 3f * scale),
            size = Size(7f * scale, 18f * scale),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun HomeNavIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.width / 24f
        val strokeWidth = 2.dp.toPx()
        val path = Path().apply {
            moveTo(3f * scale, 12f * scale)
            lineTo(12f * scale, 4f * scale)
            lineTo(21f * scale, 12f * scale)
            moveTo(5f * scale, 12f * scale)
            lineTo(5f * scale, 20f * scale)
            lineTo(19f * scale, 20f * scale)
            lineTo(19f * scale, 12f * scale)
            moveTo(10f * scale, 20f * scale)
            lineTo(10f * scale, 15f * scale)
            lineTo(14f * scale, 15f * scale)
            lineTo(14f * scale, 20f * scale)
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FamilyNavIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.width / 24f
        val strokeWidth = 2.dp.toPx()
        
        drawCircle(
            color = tint,
            radius = 3.5f * scale,
            center = Offset(8f * scale, 8f * scale),
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(3f * scale, 13f * scale),
            size = Size(10f * scale, 8f * scale),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        drawCircle(
            color = tint,
            radius = 3f * scale,
            center = Offset(16f * scale, 10f * scale),
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(11f * scale, 14f * scale),
            size = Size(10f * scale, 7f * scale),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SettingsNavIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.width / 24f
        val strokeWidth = 2.dp.toPx()
        val center = Offset(12f * scale, 12f * scale)
        
        drawCircle(
            color = tint,
            radius = 3f * scale,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        
        drawCircle(
            color = tint,
            radius = 7f * scale,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        
        for (i in 0 until 8) {
            val angle = i * 45f * (Math.PI / 180f).toFloat()
            val startDist = 7f * scale
            val endDist = 9.5f * scale
            drawLine(
                color = tint,
                start = Offset(center.x + startDist * kotlin.math.cos(angle), center.y + startDist * kotlin.math.sin(angle)),
                end = Offset(center.x + endDist * kotlin.math.cos(angle), center.y + endDist * kotlin.math.sin(angle)),
                strokeWidth = 2.5f * scale,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun CalendarIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val scale = size.width / 12f
        val strokeWidth = 1f * scale
        
        // Main box
        drawRoundRect(
            color = tint,
            topLeft = Offset(1.5f * scale, 2.5f * scale),
            size = Size(9f * scale, 8f * scale),
            cornerRadius = CornerRadius(1.5f * scale, 1.5f * scale),
            style = Stroke(width = strokeWidth)
        )
        
        // Two prongs/loops
        drawLine(color = tint, start = Offset(3.5f * scale, 1f * scale), end = Offset(3.5f * scale, 3.5f * scale), strokeWidth = strokeWidth)
        drawLine(color = tint, start = Offset(8.5f * scale, 1f * scale), end = Offset(8.5f * scale, 3.5f * scale), strokeWidth = strokeWidth)
        
        // Inner line
        drawLine(color = tint, start = Offset(1.5f * scale, 5.5f * scale), end = Offset(10.5f * scale, 5.5f * scale), strokeWidth = strokeWidth)
    }
}

// ---------------------------------------------------------
// Interactive Modal Dialogs
// ---------------------------------------------------------

@Composable
fun AddTaskDialog(
    language: Language,
    profile: String,
    voiceText: String,
    speechTriggerId: Int,
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
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp))
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = LocalizedStrings.get("add_task", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800
                )

                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = { Text(text = LocalizedStrings.get("enter_task_hint", language), fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchIndigo,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Speech recognition button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF2FF))
                            .clickable { onVoiceClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StitchIndigo.copy(alpha = pulseAlpha), CircleShape)
                                .border(1.dp, StitchIndigo, CircleShape)
                        )
                        // Simple Mic Icon
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val w = size.width
                            val h = size.height
                            drawRoundRect(
                                color = StitchIndigo,
                                topLeft = Offset(w * 0.35f, h * 0.15f),
                                size = Size(w * 0.3f, h * 0.5f),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                            drawArc(
                                color = StitchIndigo,
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(w * 0.2f, h * 0.35f),
                                size = Size(w * 0.6f, h * 0.4f),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawLine(
                                color = StitchIndigo,
                                start = Offset(w * 0.5f, h * 0.75f),
                                end = Offset(w * 0.5f, h * 0.95f),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text(text = LocalizedStrings.get("cancel", language), color = StitchSlate500)
                        }
                        Button(
                            onClick = {
                                if (textState.isNotBlank()) {
                                    onSave(textState)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = LocalizedStrings.get("save", language), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    language: Language,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(StitchRed50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash Icon",
                        tint = StitchRed500,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = LocalizedStrings.get("delete_confirm", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchSlate500),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(text = LocalizedStrings.get("delete_no", language))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchRed500)
                    ) {
                        Text(text = LocalizedStrings.get("delete_yes", language), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun FamilySelectorDialog(
    language: Language,
    familyMembers: List<com.example.util.FamilyMember>,
    currentProfile: String,
    onProfileSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = LocalizedStrings.get("choose_user_profile", language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (familyMembers.isEmpty()) {
                        Text(
                            text = "No family members found. Synced to your individual board.",
                            fontSize = 13.sp,
                            color = StitchSlate500
                        )
                    } else {
                        familyMembers.forEach { member ->
                            val isSelected = member.uid == currentProfile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFFEEF2FF) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) StitchIndigo else Color(0xFFF1F5F9),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onProfileSelect(member.uid) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // User Avatar representation
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(StitchIndigo.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.name.take(1).uppercase(),
                                            color = StitchIndigo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = member.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = StitchSlate800
                                        )
                                        Text(
                                            text = member.email,
                                            fontSize = 11.sp,
                                            color = StitchSlate500
                                        )
                                    }
                                }
                                
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = StitchIndigo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = LocalizedStrings.get("cancel", language), color = StitchIndigo)
                    }
                }
            }
        }
    }
}
