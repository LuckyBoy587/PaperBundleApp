package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import com.example.data.Task
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
import com.example.ui.theme.*
import com.example.util.Language
import com.example.util.LocalizedStrings
import com.example.util.FirebaseSyncManager
import com.example.util.UserSession
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TaskApplication
        val repository = app.repository
        val viewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(application, repository)
        )[TaskViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val userSession by viewModel.currentUserSession.collectAsState()

    when {
        userSession == null -> {
            GoogleLoginScreen(viewModel)
        }
        userSession?.familyId == null -> {
            userSession?.let { session ->
                FamilySetupScreen(viewModel = viewModel, session = session)
            }
        }
        else -> {
            userSession?.let { session ->
                SharedFamilyBoardScreen(viewModel = viewModel, session = session)
            }
        }
    }
}

@Composable
fun GoogleLoginScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaperBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Wooden Clipboard Visual Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        // clip plate
                        drawRoundRect(
                            color = Color(0xFF8F8D88),
                            topLeft = Offset(size.width * 0.2f, 0f),
                            size = Size(size.width * 0.6f, size.height * 0.25f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                        // paper bundle backdrop
                        drawRoundRect(
                            color = PaperCardLight,
                            topLeft = Offset(0f, size.height * 0.15f),
                            size = Size(size.width, size.height * 0.85f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("📌", fontSize = 42.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = LocalizedStrings.get("title", language),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = PencilCharcoal,
                textAlign = TextAlign.Center
            )

            Text(
                text = LocalizedStrings.get("app_desc", language),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = PencilGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Main Google Sign in Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable {
                        // Authenticate as a real Google sign-in simulation
                        viewModel.loginWithGoogleProfile(
                            context,
                            "Kowshik Baskaran",
                            "kowshi587@gmail.com",
                            "https://api.dicebear.com/7.x/adventurer/svg?seed=Kowshik"
                        ) {
                            Toast.makeText(context, "Logged in securely", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .testTag("google_login_button"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SoftDivider),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "G ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4285F4),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizedStrings.get("sign_in_google", language),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (authLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = AmmaPrimary)
            }

            authError?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = err,
                    color = RedPencil,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FamilySetupScreen(viewModel: TaskViewModel, session: UserSession) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var familyNameInput by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaperBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome metadata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8DBC0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(session.name.first().toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Hello, ${session.name}!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal
                    )
                    Text(
                        text = session.email,
                        fontSize = 13.sp,
                        color = PencilGray
                    )
                }
            }

            Divider(color = SoftDivider, modifier = Modifier.padding(bottom = 16.dp))

            // Card 1: Create Family board
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaperCardLight),
                border = BorderStroke(1.dp, SoftDivider),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocalizedStrings.get("create_family_title", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = LocalizedStrings.get("create_family_desc", language),
                        fontSize = 13.sp,
                        color = PencilGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = familyNameInput,
                        onValueChange = { familyNameInput = it },
                        placeholder = { Text(LocalizedStrings.get("family_name_hint", language), fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_family_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmmaPrimary,
                            unfocusedBorderColor = SoftDivider,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.handleCreateFamily(context, familyNameInput) {
                                Toast.makeText(context, "Welcome to your family workspace!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = familyNameInput.isNotBlank() && !authLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_family_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmmaPrimary)
                    ) {
                        Text(
                            text = LocalizedStrings.get("create_button", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = LocalizedStrings.get("or_label", language),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PencilGray,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: Join Family board
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PaperCardLight),
                border = BorderStroke(1.dp, SoftDivider),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocalizedStrings.get("join_family_title", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = LocalizedStrings.get("join_family_desc", language),
                        fontSize = 13.sp,
                        color = PencilGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it },
                        placeholder = { Text(LocalizedStrings.get("invite_code_hint", language), fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_family_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppaPrimary,
                            unfocusedBorderColor = SoftDivider,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.handleJoinFamily(context, inviteCodeInput) {
                                Toast.makeText(context, "Connected to your family board!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = inviteCodeInput.isNotBlank() && !authLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("join_family_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AppaPrimary)
                    ) {
                        Text(
                            text = LocalizedStrings.get("join_button", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Disconnect/Logout button at bottom
            OutlinedButton(
                onClick = { viewModel.logout(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_profile_button"),
                border = BorderStroke(1.dp, SoftDivider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PencilCharcoal)
            ) {
                Text(
                    text = LocalizedStrings.get("logout_button", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (authLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = AmmaPrimary)
            }

            authError?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = err,
                    color = RedPencil,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
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
    var completedExpanded by remember { mutableStateOf(false) }

    // Aggregate counts dynamically
    val memberPendingCounts = remember(allTasks, familyMembers) {
        familyMembers.associate { member ->
            member.uid to allTasks.count { it.profileOwner == member.uid && !it.isCompleted }
        }
    }

    // Split active profile's tasks into Pending vs Completed
    val pendingTasksList = remember(activeTasks) {
        activeTasks.filter { !it.isCompleted }
    }
    val completedTasksList = remember(activeTasks) {
        activeTasks.filter { it.isCompleted }
    }

    // Speech-to-Text Setup
    var voiceTextForInput by remember { mutableStateOf("") }
    var speechTriggerId by remember { mutableStateOf(0) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                voiceTextForInput = spokenText
                speechTriggerId++
            }
        } else {
            Toast.makeText(
                context,
                LocalizedStrings.get("voice_speech_not_understood", language),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val startSpeechRecognition = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == Language.TA) "ta-IN" else "en-US")
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (language == Language.TA) "குறிப்பைச் சொல்லுங்கள்..." else "Speak your note now..."
            )
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                LocalizedStrings.get("voice_not_supported", language),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val selectedIndex = familyMembers.indexOfFirst { it.uid == curProfile }
    val activeTabColor = if (selectedIndex >= 0 && selectedIndex % 2 == 0) AmmaPrimary else AppaPrimary

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = WarmPaperBackground,
        topBar = {
            WoodenHeader(
                language = language,
                userSession = session,
                onLanguageToggle = { viewModel.setLanguage(it) },
                onLogout = { viewModel.logout(context) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    voiceTextForInput = ""
                    isAddingTask = true
                },
                modifier = Modifier
                    .padding(16.dp)
                    .size(72.dp)
                    .testTag("add_task_fab"),
                containerColor = activeTabColor,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = LocalizedStrings.get("add_task", language),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Physical paper bundle hanger selector (Dynamic Hanger Tabs)
            PaperHangerRow(
                language = language,
                curProfile = curProfile,
                onProfileSelect = { viewModel.setProfile(it) },
                familyMembers = familyMembers,
                memberPendingCounts = memberPendingCounts
            )

            // Current Active Task List Scrollable Board
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 10.dp)
                ) {
                    if (pendingTasksList.isEmpty()) {
                        item {
                            val activeMember = familyMembers.find { it.uid == curProfile }
                            val activeMemberName = activeMember?.name?.substringBefore(" ") ?: "Member"
                            EmptyPaperNote(
                                message = String.format(LocalizedStrings.get("no_pending_member", language), activeMemberName),
                                accentColor = activeTabColor
                            )
                        }
                    } else {
                        items(pendingTasksList, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                language = language,
                                onToggle = { viewModel.toggleTaskComplete(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }

                    // Collapsible Completed Archived Stack
                    if (completedTasksList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            CompletedHeaderSection(
                                language = language,
                                count = completedTasksList.size,
                                isExpanded = completedExpanded,
                                onClick = { completedExpanded = !completedExpanded }
                            )
                        }

                        if (completedExpanded) {
                            items(completedTasksList, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    language = language,
                                    onToggle = { viewModel.toggleTaskComplete(task) },
                                    onDelete = { taskToDelete = task }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Notepad Dialog
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

    // Custom Trash Confirmation Popup
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
fun WoodenHeader(
    language: Language,
    userSession: UserSession?,
    onLanguageToggle: (Language) -> Unit,
    onLogout: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B5330), // warm teak top
                        Color(0xFF4C2711)  // dark mahogany bottom
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User photo / avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCF4E8).copy(alpha = 0.2f))
                    .clickable {
                        userSession?.familyInviteCode?.let { code ->
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Invite Code Copied! Send it to your family.", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userSession?.name?.first().toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCF4E8)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = userSession?.familyName ?: LocalizedStrings.get("title", language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFFCF4E8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                userSession?.familyInviteCode?.let { code ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF381C0B))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Invite Code Copied: $code", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(LocalizedStrings.get("invite_code_banner", language), code),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFDCC2A1)
                        )
                    }
                }
            }
        }

        // Beautiful Language toggle and Exit
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF381C0B))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageButton(
                    text = LocalizedStrings.get("english_opt", language),
                    isSelected = language == Language.EN,
                    onClick = { onLanguageToggle(Language.EN) }
                )
                LanguageButton(
                    text = LocalizedStrings.get("tamil_opt", language),
                    isSelected = language == Language.TA,
                    onClick = { onLanguageToggle(Language.TA) }
                )
            }

            // Tactile sign-out icon from the notice board
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF381C0B))
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = Color(0xFFDCC2A1),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) Color(0xFFB57045) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFFC4AAA0)
        )
    }
}

@Composable
fun PaperHangerRow(
    language: Language,
    curProfile: String,
    onProfileSelect: (String) -> Unit,
    familyMembers: List<com.example.util.FamilyMember>,
    memberPendingCounts: Map<String, Int>
) {
    Column {
        // Wooden hanging rod
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B5330), Color(0xFF4C2711))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            familyMembers.forEachIndexed { index, member ->
                val pendingCount = memberPendingCounts[member.uid] ?: 0
                val accentColor = if (index % 2 == 0) AmmaPrimary else AppaPrimary
                val bgColor = if (index % 2 == 0) AmmaSurface else AppaSurface
                val onTextColor = if (index % 2 == 0) AmmaOnSurface else AppaOnSurface
                val rotationAngle = if (index % 2 == 0) -2.5f else 2.5f

                HangingClipboard(
                    modifier = Modifier.weight(1f),
                    label = if (member.name.contains(" (Amma)") || member.name.contains(" (Appa)") || member.name.contains(" (Kowshik)")) {
                        member.name.substringBefore(" (")
                    } else if (member.name.contains(" ")) {
                        member.name.substringBefore(" ")
                    } else {
                        member.name
                    },
                    countText = if (pendingCount == 0) {
                        LocalizedStrings.get("remaining_none", language)
                    } else if (pendingCount == 1) {
                        LocalizedStrings.get("remaining_count_singular", language)
                    } else {
                        String.format(LocalizedStrings.get("remaining_count", language), pendingCount)
                    },
                    accentColor = accentColor,
                    bgColor = bgColor,
                    onTextColor = onTextColor,
                    isSelected = curProfile == member.uid,
                    rotationAngle = rotationAngle,
                    onClick = { onProfileSelect(member.uid) },
                    testTag = "tab_${member.uid}"
                )
            }
        }
    }
}

@Composable
fun HangingClipboard(
    modifier: Modifier = Modifier,
    label: String,
    countText: String,
    accentColor: Color,
    bgColor: Color,
    onTextColor: Color,
    isSelected: Boolean,
    rotationAngle: Float,
    onClick: () -> Unit,
    testTag: String
) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.05f else 0.92f, label = "scale")
    val alpha by animateFloatAsState(targetValue = if (isSelected) 1.0f else 0.65f, label = "alpha")
    val translationY by animateDpAsState(targetValue = if (isSelected) 8.dp else 0.dp, label = "offset")
    val rotation by animateFloatAsState(targetValue = if (isSelected) 0f else rotationAngle, label = "rotate")

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY.toPx()
                rotationZ = rotation
                this.alpha = alpha
            }
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wooden wire hook & bulldog peg clip
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(28.dp)
                .drawBehind {
                    // Draw dual strings
                    drawLine(
                        color = Color(0xFF6B6A68),
                        start = Offset(size.width / 4, -40f),
                        end = Offset(size.width / 4, 15f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF6B6A68),
                        start = Offset(size.width * 0.75f, -40f),
                        end = Offset(size.width * 0.75f, 15f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                    // Draw metallic peg shield/clip plate
                    drawRoundRect(
                        color = Color(0xFF8F8D88),
                        topLeft = Offset(0f, 10f),
                        size = Size(size.width, size.height - 10f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    // peg pivot line
                    drawLine(
                        color = Color(0xFF5E5D5A),
                        start = Offset(0f, size.height * 0.5f),
                        end = Offset(size.width, size.height * 0.5f),
                        strokeWidth = 2f
                    )
                }
        )

        // Clip-attached paper sheet
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isSelected) 8.dp else 2.dp,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp),
            border = BorderStroke(1.5.dp, if (isSelected) accentColor else SoftDivider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pin / peg hole circle
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8DBC0))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = onTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stamp style visual capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = countText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    language: Language,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    // Generate static rotation based on task's UUID hashcode
    val rotation = remember(task.id) { (task.id.hashCode().absoluteValue % 5 - 2) * 0.6f }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer { rotationZ = rotation }
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(4.dp)
            )
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = PaperCardLight),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, SoftDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Typical red notebook margin line on left
                    drawLine(
                        color = Color(0xFFDC6662).copy(alpha = 0.7f),
                        start = Offset(44.dp.toPx(), 0f),
                        end = Offset(44.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Soft graphite ruling lines
                    val lineSpacing = 28.dp.toPx()
                    val totalLines = (size.height / lineSpacing).toInt()
                    for (i in 1..totalLines) {
                        val y = i * lineSpacing
                        drawLine(
                            color = SoftDivider.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                .padding(vertical = 14.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Massive 48dp check target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle)
                    .testTag("check_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .drawBehind {
                            drawCircle(
                                color = if (task.isCompleted) RedPencil else PencilCharcoal,
                                radius = size.minDimension / 2,
                                style = Stroke(
                                    width = 2.5f,
                                    cap = StrokeCap.Round
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = LocalizedStrings.get("completed_label", language),
                            tint = RedPencil,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Task Header and Created metadata in column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) PencilGray.copy(alpha = 0.51f) else PencilCharcoal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Real-time added/completed member tags
                if (task.createdByName.isNotBlank() || task.completedByName?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (task.isCompleted && task.completedByName != null) {
                            String.format(LocalizedStrings.get("completed_by", language), task.completedByName)
                        } else {
                            String.format(LocalizedStrings.get("added_by", language), task.createdByName)
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) PencilGray.copy(alpha = 0.6f) else if (task.profileOwner == "AMMA") AmmaPrimary.copy(alpha = 0.8f) else AppaPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            // Throw away button (massive 48dp target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete)
                    .testTag("delete_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete memo",
                    tint = RedPencil.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyPaperNote(
    message: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PaperCardLight.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SoftDivider.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drawn empty peg slot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8DBC0))
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = message,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                color = PencilGray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun CompletedHeaderSection(
    language: Language,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = AppaPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${LocalizedStrings.get("completed_title", language)} ($count)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = PencilGray,
            textDecoration = TextDecoration.Underline
        )
    }
}

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(12.dp, shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = PaperCardLight),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, if (profile == "AMMA") AmmaPrimary else AppaPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header of notepad popup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocalizedStrings.get("add_task", language),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = PencilCharcoal
                    )

                    // Profile label pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (profile == "AMMA") AmmaPrimary.copy(alpha = 0.15f)
                                else AppaPrimary.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (profile == "AMMA") {
                                LocalizedStrings.get("amma", language)
                            } else {
                                LocalizedStrings.get("appa", language)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profile == "AMMA") AmmaPrimary else AppaPrimary,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notebook styled text field
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = {
                        Text(
                            text = LocalizedStrings.get("enter_task_hint", language),
                            fontSize = 18.sp,
                            color = PencilGray.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Serif
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .focusRequester(focusRequester)
                        .testTag("add_task_input"),
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 26.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (profile == "AMMA") AmmaPrimary else AppaPrimary,
                        unfocusedBorderColor = SoftDivider,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Voice assistant typing hint box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFCF7ED))
                        .clickable(onClick = onVoiceClick)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomMicIcon(
                        color = if (profile == "AMMA") AmmaPrimary else AppaPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = LocalizedStrings.get("voice_hint", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PencilCharcoal,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = if (language == Language.EN) {
                                "Just tap and speak clearly in English or Tamil!"
                            } else {
                                "இதனைத் தட்டி தமிழ் அல்லது ஆங்கிலத்தில் கூறவும்!"
                            },
                            fontSize = 11.sp,
                            color = PencilGray,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("add_task_cancel"),
                        border = BorderStroke(1.5.dp, SoftDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PencilCharcoal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("cancel", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Button(
                        onClick = {
                            if (textState.isNotBlank()) {
                                onSave(textState)
                            }
                        },
                        enabled = textState.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("add_task_save"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (profile == "AMMA") AmmaPrimary else AppaPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("save", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(8.dp),
            colors = CardDefaults.cardColors(containerColor = PaperCardLight),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, SoftDivider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning note pin
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(RedPencil)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = LocalizedStrings.get("delete_confirm", language),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = PencilCharcoal,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("delete_confirm_cancel"),
                        border = BorderStroke(1.5.dp, SoftDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PencilCharcoal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("delete_no", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("delete_confirm_ok"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPencil,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("delete_yes", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomMicIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Microphone capsule
        val capW = w * 0.35f
        val capH = h * 0.5f
        val capLeft = (w - capW) / 2
        val capTop = h * 0.15f
        drawRoundRect(
            color = color,
            topLeft = Offset(capLeft, capTop),
            size = Size(capW, capH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(capW / 2, capW / 2)
        )

        // 2. Microphone cradle
        val path = Path().apply {
            moveTo(w * 0.18f, capTop + capH * 0.4f)
            quadraticTo(
                w * 0.18f,
                capTop + capH + 12f,
                w / 2,
                capTop + capH + 12f
            )
            quadraticTo(
                w * 0.82f,
                capTop + capH + 12f,
                w * 0.82f,
                capTop + capH * 0.4f
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // 3. Stand rod and base plate
        drawLine(
            color = color,
            start = Offset(w / 2, capTop + capH + 12f),
            end = Offset(w / 2, h * 0.85f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.28f, h * 0.85f),
            end = Offset(w * 0.72f, h * 0.85f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }
}
