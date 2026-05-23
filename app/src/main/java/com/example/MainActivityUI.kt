package com.example

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import com.example.data.SyncState
import com.example.util.FirebaseSyncManager
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberDraggableState



// Stitch Dashboard Colors
val StitchBg = Color(0xFFF1F5F9) // Premium Modern Slate-100 app background (for gorgeous contrast with white sheets)
val StitchBorder = Color(0xFFE2E8F0) // Premium Modern Slate-200 border for ultra-crisp, defined card boundaries
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
                .border(BorderStroke(1.dp, StitchBorder), RoundedCornerShape(32.dp))
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
                text = "Baski Family",
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

    val isNetworkAvailable = remember { mutableStateOf(FirebaseSyncManager.isNetworkAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            isNetworkAvailable.value = FirebaseSyncManager.isNetworkAvailable()
            kotlinx.coroutines.delay(3000)
        }
    }

    val hasPendingWrites = remember(activeTasks) {
        activeTasks.any { it.syncState == SyncState.PENDING_WRITE || it.syncState == SyncState.SYNCING }
    }
    val familyMembers by viewModel.familyMembers.collectAsState()    var isAddingTask by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showProfileSelector by remember { mutableStateOf(false) }
    var isCompletedSheetOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showSettingsMenu by remember { mutableStateOf(false) }
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
            // Fallback: If current user isn't in familyMembers, add them at the front
            list.add(0, com.example.util.FamilyMember(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBg)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color.Black.copy(alpha = 0.06f),
                                spotColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                    ) {
                        MainHeader(
                            initial = initialLetter,
                            language = language,
                            onAvatarClick = { selectedTab = 1 },
                            onLanguageClick = {
                                viewModel.setLanguage(if (language == Language.EN) Language.TA else Language.EN)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    GlobalSyncStatusBar(
                        isNetworkAvailable = isNetworkAvailable.value,
                        hasPendingWrites = hasPendingWrites,
                        isFirebaseInitialized = FirebaseSyncManager.isFirebaseInitialized
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = 86.dp) // Leave space for bottom nav
            ) {
                if (selectedTab == 0) {
                    // A. Tasks Header with Filters (Fixed) - soft card container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color.Black.copy(alpha = 0.05f),
                                spotColor = Color.Black.copy(alpha = 0.07f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    ) {
                        TasksSectionHeader()
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // B. Scrollable Dynamic Tasks List taking up remaining center part
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                        ) {
                            items(sortedMembers, key = { it.uid }) { member ->
                                val memberTasks = remember(activeTasksList, member.uid) {
                                    activeTasksList.filter { it.profileOwner == member.uid }
                                }
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
                                .background(Color.White)
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
                                            .background(Color(0xFFECFDF5), CircleShape),
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
                                            .background(Color(0xFFEEF2FF))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$completedTasksCount",
                                            color = StitchIndigo,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Small pull-up arrow indicator
                                    Text(
                                        text = "▲",
                                        fontSize = 10.sp,
                                        color = StitchSlate500
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // Beautiful Family Directory screen content directly above navbar
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        // Header section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.05f),
                                    spotColor = Color.Black.copy(alpha = 0.07f)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Family Directory",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSlate800
                                )
                                Text(
                                    text = "Select a family member to manage and view their tasks",
                                    fontSize = 12.sp,
                                    color = StitchSlate500
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // List of Members
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (familyMembers.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No family members found.",
                                        fontSize = 14.sp,
                                        color = StitchSlate500
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(familyMembers) { member ->
                                        val isSelected = member.uid == curProfile
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shadow(
                                                    elevation = if (isSelected) 6.dp else 2.dp,
                                                    shape = RoundedCornerShape(20.dp),
                                                    ambientColor = if (isSelected) StitchIndigo.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.03f),
                                                    spotColor = if (isSelected) StitchIndigo.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)
                                                )
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isSelected) Color(0xFFF5F7FF) else Color.White)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) StitchIndigo else StitchBorder,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .clickable {
                                                    viewModel.setProfile(member.uid)
                                                    selectedTab = 0 // Switch back to Tasks tab
                                                }
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    // Dynamic Letter Avatar with high-quality styling
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .background(
                                                                if (isSelected) StitchIndigo else StitchIndigo.copy(alpha = 0.1f),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = member.name.take(1).uppercase(),
                                                            color = if (isSelected) Color.White else StitchIndigo,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 18.sp
                                                        )
                                                    }

                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            text = member.name,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = StitchSlate800
                                                        )
                                                        Text(
                                                            text = member.email,
                                                            fontSize = 12.sp,
                                                            color = StitchSlate500
                                                        )
                                                    }
                                                }

                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(StitchIndigo.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Active",
                                                            color = StitchIndigo,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
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
                }
            }
        }

        // Floating Action Button (Only shown on Tasks tab)
        if (selectedTab == 0) {
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 1: Tasks
                val isTasksActive = selectedTab == 0
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isTasksActive) StitchIndigo.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable {
                            selectedTab = 0
                            showSettingsMenu = false
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        HomeNavIcon(tint = if (isTasksActive) StitchIndigo else StitchSlate500)
                        Text(
                            text = "Tasks",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTasksActive) StitchIndigo else StitchSlate500
                        )
                    }
                }

                // Item 2: Family
                val isFamilyActive = selectedTab == 1
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isFamilyActive) StitchIndigo.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable {
                            selectedTab = 1
                            showSettingsMenu = false
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        FamilyNavIcon(tint = if (isFamilyActive) StitchIndigo else StitchSlate500)
                        Text(
                            text = "Family",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFamilyActive) StitchIndigo else StitchSlate500
                        )
                    }
                }

                // Item 3: Settings
                val isSettingsActive = showSettingsMenu
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSettingsActive) StitchIndigo.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable {
                            if (showSettingsMenu) {
                                showSettingsMenu = false
                                selectedTab = 0
                            } else {
                                showSettingsMenu = true
                                selectedTab = 2
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        SettingsNavIcon(tint = if (isSettingsActive) StitchIndigo else StitchSlate500)
                        Text(
                            text = "Settings",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSettingsActive) StitchIndigo else StitchSlate500
                        )
                    }
                }
            }
        }

        // Settings floating dismiss scrim
        if (showSettingsMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showSettingsMenu = false
                        selectedTab = 0
                    }
            )
        }

        // Settings floating popup menu
        AnimatedVisibility(
            visible = showSettingsMenu,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp)
                .fillMaxWidth(0.92f),
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 3 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 3 }
        ) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.16f)
                    )
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(1.dp, StitchBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // App info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PaperBundle",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSlate800
                            )
                            Text(
                                text = "Version 2026.1",
                                fontSize = 11.sp,
                                color = StitchSlate500
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StitchBorder)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Logout Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF1F1))
                            .clickable {
                                showSettingsMenu = false
                                viewModel.logout(context)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(StitchRed500.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "↩", fontSize = 14.sp, color = StitchRed500)
                            }
                            Text(
                                text = "Logout",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchRed500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // E. Sliding Completed Tasks Sheet
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

            // Sync sheet offset with open/closed state transitions
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
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Area supporting drag/swipe down to close directly from where finger is released
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
                                        .background(Color(0xFFCBD5E1), RoundedCornerShape(100.dp))
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

// Curated list of vibrant modern accent colors for family members
val MemberColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink/Rose
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF06B6D4), // Cyan
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEF4444)  // Red
)

fun getMemberColor(uid: String): Color {
    val index = Math.abs(uid.hashCode()) % MemberColors.size
    return MemberColors[index]
}

@Composable
fun MemberTaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
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
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, CircleShape)
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
                    // Creator badge (only show if created by someone else to avoid noise)
                    if (task.createdByUid.isNotEmpty() && task.createdByUid != task.profileOwner) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFF1F5F9))
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
                                .background(Color(0xFFE2E8F0))
                        )
                    }

                    // Sync State Badge
                    val stateColor = when (task.syncState) {
                        SyncState.SYNCED -> StitchGreen500
                        SyncState.CACHED -> Color(0xFF64748B)
                        SyncState.PENDING_WRITE -> Color(0xFFD97706)
                        SyncState.SYNCING -> StitchIndigo
                        SyncState.ERROR -> Color.Red
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
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun MemberTodoBox(
    member: com.example.util.FamilyMember,
    isCurrentUser: Boolean,
    tasks: List<Task>,
    showAddButton: Boolean = true,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTaskClick: () -> Unit = {}
) {
    val accentColor = remember(member.uid) { getMemberColor(member.uid) }
    
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
            .background(Color.White)
            .border(BorderStroke(1.dp, StitchBorder), RoundedCornerShape(24.dp))
    ) {
        // Unique member accent vertical bar indicator on the left
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
            // Box Header - clickable if expandable
            val headerModifier = if (onToggleExpand != null) {
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
            } else {
                Modifier.fillMaxWidth()
            }

            Row(
                modifier = headerModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Small Avatar with custom accent
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.name.take(1).uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.name.substringBefore(" "),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSlate800
                            )
                            if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "You",
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge count
                    if (tasks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            val badgeText = if (showAddButton) "${tasks.size} pending" else "${tasks.size} done"
                            Text(
                                text = badgeText,
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Add Task "+" icon specifically for this member
                    if (showAddButton) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { onAddTaskClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task for ${member.name}",
                                tint = StitchSlate500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Expand/Collapse Chevron
                    if (onToggleExpand != null) {
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            label = "chevronRotation"
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▼",
                                fontSize = 10.sp,
                                color = StitchSlate500,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotationAngle
                                }
                            )
                        }
                    }
                }
            }

            // Only show content if expanded
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Task List or Empty State
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "✨", fontSize = 14.sp)
                            Text(
                                text = "All caught up!",
                                color = StitchSlate500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        tasks.forEachIndexed { index, task ->
                            MemberTaskRow(
                                task = task,
                                onToggle = { onToggleTask(task) },
                                onDelete = { onDeleteTask(task) }
                            )
                            if (index < tasks.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(StitchBorder)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainHeader(
    initial: String,
    language: Language,
    onAvatarClick: () -> Unit,
    onLanguageClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                        .border(1.dp, StitchBorder, CircleShape)
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
                        .border(1.dp, StitchBorder, CircleShape)
                        .clickable { onLanguageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    GlobeNavIcon(tint = StitchSlate500)
                }
            }
        }
    }
}

@Composable
fun TasksSectionHeader() {
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
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(Color.White, RoundedCornerShape(24.dp))
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Creator Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(StitchIndigo)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.createdByName,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
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
                                color = StitchSlate500,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Vertical bar divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(10.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        // Sync State Badge
                        val stateColor = when (task.syncState) {
                            SyncState.SYNCED -> StitchGreen500
                            SyncState.CACHED -> Color(0xFF64748B) // Slate-500
                            SyncState.PENDING_WRITE -> Color(0xFFD97706) // Amber-600
                            SyncState.SYNCING -> StitchIndigo
                            SyncState.ERROR -> Color.Red
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
                            SyncState.ERROR -> "Sync Error"
                        }

                        val rotation = if (task.syncState == SyncState.SYNCING) {
                            val infiniteTransition = rememberInfiniteTransition(label = "SyncingRot")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RotAngle"
                            )
                            angle
                        } else 0f

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = stateIcon,
                                contentDescription = stateText,
                                tint = stateColor,
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer {
                                        rotationZ = rotation
                                    }
                            )
                            Text(
                                text = stateText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = stateColor,
                                maxLines = 1,
                                softWrap = false
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

@Composable
fun GlobalSyncStatusBar(
    isNetworkAvailable: Boolean,
    hasPendingWrites: Boolean,
    isFirebaseInitialized: Boolean
) {
    val bgColor = when {
        !isFirebaseInitialized -> Color(0xFFF1F5F9) // Slate-100
        !isNetworkAvailable -> Color(0xFFFEF3C7) // Amber-100
        hasPendingWrites -> Color(0xFFEEF2FF) // Indigo-50
        else -> Color(0xFFECFDF5) // Green-50
    }
    val contentColor = when {
        !isFirebaseInitialized -> Color(0xFF475569)
        !isNetworkAvailable -> Color(0xFFD97706)
        hasPendingWrites -> StitchIndigo
        else -> StitchGreen500
    }
    val icon = when {
        !isFirebaseInitialized -> Icons.Default.Info
        !isNetworkAvailable -> Icons.Default.Info
        hasPendingWrites -> Icons.Default.Refresh
        else -> Icons.Default.CheckCircle
    }
    val text = when {
        !isFirebaseInitialized -> "Sandbox Mode (Local Room)"
        !isNetworkAvailable -> "Offline Mode — Viewing Cached Data"
        hasPendingWrites -> "Synchronizing pending writes..."
        else -> "Connected & Synced with Cloud Firestore"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    AnimatedVisibility(
        visible = true,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        if (hasPendingWrites && isNetworkAvailable && isFirebaseInitialized) {
                            rotationZ = rotationAngle
                        }
                    }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// StitchHeroBanner removed as Good Day banner is deleted.



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

    // Idle: subtle indigo pulse. Listening: vivid red pulse.
    val micColor = if (isListening) StitchRed500 else StitchIndigo
    val micBg = if (isListening) Color(0xFFFFEBEB) else Color(0xFFDFE7F9)

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.5f else 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 700 else 1400, easing = LinearEasing),
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
                .border(2.dp, Color(0xFFD4D7E8), RoundedCornerShape(28.dp))
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
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Mic button + hint label
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

                // Action buttons — right-aligned
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
                        Text(text = LocalizedStrings.get("save", language), color = Color.White, fontSize = 14.sp)
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
                .border(1.dp, StitchBorder, RoundedCornerShape(24.dp))
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
                .border(1.dp, StitchBorder, RoundedCornerShape(28.dp))
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
