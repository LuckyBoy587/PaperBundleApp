package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import kotlin.math.absoluteValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
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
import com.example.ui.theme.AmmaOnSurface
import com.example.ui.theme.AmmaPrimary
import com.example.ui.theme.AmmaSurface
import com.example.ui.theme.AppaOnSurface
import com.example.ui.theme.AppaPrimary
import com.example.ui.theme.AppaSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PaperCardLight
import com.example.ui.theme.PencilCharcoal
import com.example.ui.theme.PencilGray
import com.example.ui.theme.RedPencil
import com.example.ui.theme.SoftDivider
import com.example.ui.theme.WarmPaperBackground
import com.example.util.Language
import com.example.util.LocalizedStrings
import com.example.util.UserSession

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

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun AmbientMeshBackground(
    activeAccentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedAccentColor by animateColorAsState(
        targetValue = activeAccentColor,
        animationSpec = tween(1500),
        label = "bgAccentColor"
    )
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        // Base Stitch pale off-white background
        drawRect(color = StitchBg)

        // 1. Primary Ambient Glow (Soft peach/amber sunset light top-left)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF2E1), // Light warm peach
                    Color(0x00FFF2E1)
                ),
                center = Offset(w * 0.15f, h * 0.12f),
                radius = w * 0.95f
            )
        )

        // 2. Dynamic Accent Glow (Bottom right - switches between rose or teal depending on active profile)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animatedAccentColor.copy(alpha = 0.08f),
                    animatedAccentColor.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(w * 0.85f, h * 0.85f),
                radius = w * 1.15f
            )
        )

        // 3. Central atmospheric float glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF1F2).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.7f
            )
        )
    }
}

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val userSession by viewModel.currentUserSession.collectAsState()

    LaunchedEffect(userSession) {
        Log.d("PAPER_BUNDLE", "MainActivity: MainScreen: userSession state updated - uid=${userSession?.uid}, familyId=${userSession?.familyId}")
    }

    when {
        userSession == null -> {
            GoogleLoginScreen(viewModel)
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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                val name = account.displayName ?: "User"
                val email = account.email ?: ""
                val photoUrl = account.photoUrl?.toString() ?: ""
                
                Log.d("PAPER_BUNDLE", "GoogleLoginScreen: Native sign-in success. Name: $name, Email: $email, ID Token: ${idToken != null}")
                
                viewModel.loginWithGoogleProfile(context, name, email, photoUrl, idToken) {
                    Toast.makeText(context, "Logged in securely", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Google Sign-In failed code=${e.statusCode}, message=${e.message}", e)
                viewModel.authError.value = "Google Sign-In failed: ${e.message} (status: ${e.statusCode})"
                Toast.makeText(context, "Google Sign-In failed (status: ${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Activity result code is not OK: ${result.resultCode}")
            viewModel.authError.value = "Google Sign-In cancelled or failed (code: ${result.resultCode})"
            Toast.makeText(context, "Google Sign-In cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        AmbientMeshBackground(activeAccentColor = AmmaPrimary)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.82f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(32.dp))
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .shadow(16.dp, shape = RoundedCornerShape(26.dp), ambientColor = AmmaPrimary.copy(alpha = 0.2f), spotColor = AppaPrimary.copy(alpha = 0.3f))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AmmaPrimary, AppaPrimary)
                                ),
                                shape = RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = LocalizedStrings.get("title", language),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        color = PencilCharcoal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = LocalizedStrings.get("app_desc", language),
                        fontSize = 15.sp,
                        color = PencilGray,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .clickable {
                                try {
                                    var resId = context.resources.getIdentifier("default_web_client_id", "string", "com.example")
                                    if (resId == 0) {
                                        resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                    }
                                    val webClientId = if (resId != 0) context.getString(resId) else null

                                    val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .requestProfile()

                                    if (!webClientId.isNullOrBlank()) {
                                        Log.d("PAPER_BUNDLE", "GoogleLoginScreen: Found Web Client ID: $webClientId. Requesting ID Token.")
                                        gsoBuilder.requestIdToken(webClientId)
                                    }
                                    val gso = gsoBuilder.build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                } catch (e: Exception) {
                                    Log.e("PAPER_BUNDLE", "GoogleLoginScreen: Failed to launch Google Sign-In.", e)
                                    viewModel.authError.value = "Failed to launch Google Sign-In: ${e.message}"
                                    Toast.makeText(context, "Google Sign-In unavailable", Toast.LENGTH_LONG).show()
                                }
                            }
                            .testTag("google_login_button"),
                        colors = CardDefaults.cardColors(containerColor = PencilCharcoal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.16f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "G",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = LocalizedStrings.get("sign_in_google", language),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (authLoading) {
                        CircularProgressIndicator(color = AmmaPrimary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    authError?.let { err ->
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

    var taskFilter by remember { mutableStateOf("All") }

    val filteredTasks = remember(activeTasks, taskFilter) {
        when (taskFilter) {
            "Pending" -> activeTasks.filter { !it.isCompleted }
            "Completed" -> activeTasks.filter { it.isCompleted }
            else -> activeTasks
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(activeAccentColor = activeTabColor)

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_screen"),
            containerColor = Color.Transparent, // Let the beautiful gradient background show through!
            topBar = {
                WoodenHeader(
                    language = language,
                    userSession = session,
                    onLogout = { viewModel.logout(context) }
                )
            },
            floatingActionButton = {
                Column(
                    modifier = Modifier.padding(bottom = 80.dp, end = 16.dp), // Elevated above bottom nav
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactLanguageSwitcher(
                        language = language,
                        onClick = {
                            viewModel.setLanguage(
                                if (language == Language.EN) Language.TA else Language.EN
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = StitchIndigo.copy(alpha = 0.4f),
                                spotColor = StitchIndigo.copy(alpha = 0.6f)
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(StitchPurple, StitchIndigo)
                                ),
                                CircleShape
                            )
                            .clickable {
                                voiceTextForInput = ""
                                isAddingTask = true
                            }
                            .testTag("add_task_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = LocalizedStrings.get("add_task", language),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Profile Switcher Row (Tactile cozy family members switcher)
                PaperHangerRow(
                    language = language,
                    curProfile = curProfile,
                    onProfileSelect = { viewModel.setProfile(it) },
                    familyMembers = familyMembers,
                    memberPendingCounts = memberPendingCounts
                )

                // 2. Scrollable content area
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 0.dp, end = 0.dp, bottom = 110.dp, top = 6.dp)
                ) {
                    // A. YOUR TASKS SECTION
                    item {
                        TasksSectionHeader(
                            language = language,
                            currentFilter = taskFilter,
                            onFilterChange = { taskFilter = it }
                        )
                    }

                    if (filteredTasks.isEmpty()) {
                        item {
                            val activeMember = familyMembers.find { it.uid == curProfile }
                            val activeMemberName = activeMember?.name?.substringBefore(" ") ?: "Member"
                            val emptyMsg = when (taskFilter) {
                                "Completed" -> "No completed tasks yet."
                                "Pending" -> String.format(LocalizedStrings.get("no_pending_member", language), activeMemberName)
                                else -> "No tasks created yet."
                            }
                            EmptyPaperNote(
                                message = emptyMsg,
                                accentColor = activeTabColor
                            )
                        }
                    } else {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                language = language,
                                onToggle = { viewModel.toggleTaskComplete(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }

                    // B. HERO BANNER
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        StitchHeroBanner()
                    }

                    // C. OVERVIEW SECTION
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        BoardOverviewCard(
                            language = language,
                            session = session,
                            pendingCount = activeTasks.count { !it.isCompleted },
                            completedCount = activeTasks.count { it.isCompleted },
                            activeAccent = activeTabColor
                        )
                    }
                }
            }
        }

        // 3. Floating Stitch Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .fillMaxWidth(0.92f)
                .height(72.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp),
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { /* Already on Tasks */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Tasks",
                        tint = StitchIndigo,
                        modifier = Modifier.size(24.dp)
                    )
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { /* No-op in demo */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Family",
                        tint = StitchSlate500,
                        modifier = Modifier.size(24.dp)
                    )
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { /* No-op in demo */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Settings",
                        tint = StitchSlate500,
                        modifier = Modifier.size(24.dp)
                    )
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
fun TasksSectionHeader(
    language: Language,
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
            fontWeight = FontWeight.ExtraBold,
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
fun StitchHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(StitchIndigo, StitchPurple)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "Good day!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Let's get things done",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "✨",
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun StitchProgressRing(progress: Float) {
    Box(
        modifier = Modifier.size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StitchIndigo
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
            .height(76.dp)
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
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate800,
                    lineHeight = 1.sp
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
    language: Language,
    session: UserSession,
    pendingCount: Int,
    completedCount: Int,
    activeAccent: Color
) {
    val totalCount = pendingCount + completedCount
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(24.dp)
            )
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate800
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Task summary",
                        fontSize = 11.sp,
                        color = StitchSlate500
                    )
                }
                
                StitchProgressRing(progress = progress)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StitchStatChip(
                    label = "Pending",
                    value = pendingCount.toString(),
                    tintColor = StitchRed500,
                    bgColor = StitchRed50,
                    icon = {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = StitchRed500.copy(alpha = 0.6f), style = Stroke(width = 1.5.dp.toPx()))
                            drawLine(color = StitchRed500.copy(alpha = 0.6f), start = center, end = Offset(center.x, center.y - 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                            drawLine(color = StitchRed500.copy(alpha = 0.6f), start = center, end = Offset(center.x + 2.dp.toPx(), center.y), strokeWidth = 1.5.dp.toPx())
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                StitchStatChip(
                    label = "Done",
                    value = completedCount.toString(),
                    tintColor = StitchGreen500,
                    bgColor = StitchGreen50,
                    icon = {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = StitchGreen500.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
                    },
                    modifier = Modifier.weight(1f)
                )
                StitchStatChip(
                    label = "Total",
                    value = totalCount.toString(),
                    tintColor = StitchBlue500,
                    bgColor = StitchBlue50,
                    icon = {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            val strokeWidth = 1.5.dp.toPx()
                            drawLine(color = StitchBlue500.copy(alpha = 0.6f), start = Offset(0f, 2.dp.toPx()), end = Offset(size.width, 2.dp.toPx()), strokeWidth = strokeWidth)
                            drawLine(color = StitchBlue500.copy(alpha = 0.6f), start = Offset(0f, 5.dp.toPx()), end = Offset(size.width, 5.dp.toPx()), strokeWidth = strokeWidth)
                            drawLine(color = StitchBlue500.copy(alpha = 0.6f), start = Offset(0f, 8.dp.toPx()), end = Offset(size.width, 8.dp.toPx()), strokeWidth = strokeWidth)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun WoodenHeader(
    language: Language,
    userSession: UserSession?,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Letter Icon (e.g. K) inside Indigo rounded-xl box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StitchIndigo),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userSession?.name?.firstOrNull()?.uppercase().toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 12.sp,
                    color = StitchSlate500
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userSession?.familyName ?: LocalizedStrings.get("title", language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate800
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🏡",
                        fontSize = 16.sp
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                    .clickable { /* No-op notifications */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add, // Stand-in for notifications
                    contentDescription = "Notifications",
                    tint = StitchSlate500,
                    modifier = Modifier.size(20.dp)
                )
                // Red badge dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
            }

            // Logout button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                    .clickable(onClick = onLogout),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = StitchSlate500,
                    modifier = Modifier.size(20.dp)
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
            .background(if (isSelected) PencilCharcoal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else PencilGray
        )
    }
}

@Composable
fun CompactLanguageSwitcher(
    language: Language,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, SoftDivider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (language == Language.EN) "EN" else "TA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = PencilCharcoal
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
        Spacer(modifier = Modifier.height(4.dp))

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(familyMembers.size) { index ->
                val member = familyMembers[index]
                val pendingCount = memberPendingCounts[member.uid] ?: 0
                val isSelected = curProfile == member.uid
                
                // Colors
                val accentColor = if (index % 2 == 0) AmmaPrimary else AppaPrimary
                val bgColor = if (index % 2 == 0) AmmaSurface else AppaSurface
                val onTextColor = if (index % 2 == 0) AmmaOnSurface else AppaOnSurface

                val displayName = if (member.name.contains(" (Amma)") || member.name.contains(" (Appa)") || member.name.contains(" (Kowshik)")) {
                    member.name.substringBefore(" (")
                } else if (member.name.contains(" ")) {
                    member.name.substringBefore(" ")
                } else {
                    member.name
                }

                ProfileAvatarTab(
                    name = displayName,
                    pendingCount = pendingCount,
                    accentColor = accentColor,
                    bgColor = bgColor,
                    onTextColor = onTextColor,
                    isSelected = isSelected,
                    onClick = { onProfileSelect(member.uid) },
                    testTag = "tab_${member.uid}"
                )
            }
        }
    }
}

@Composable
fun ProfileAvatarTab(
    name: String,
    pendingCount: Int,
    accentColor: Color,
    bgColor: Color,
    onTextColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    // Spring physics transitions for realistic cozy 2026 motion feel
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.92f,
        animationSpec = spring(
            dampingRatio = 0.55f, // organic bouncy physics
            stiffness = Spring.StiffnessLow
        ),
        label = "avatarScale"
    )
    val translationY by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessLow
        ),
        label = "avatarTranslationY"
    )
    val selectionGlowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.15f else 0.0f,
        animationSpec = tween(400),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY.toPx()
            }
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(accentColor.copy(alpha = selectionGlowAlpha), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = if (isSelected) 10.dp else 2.dp,
                        shape = CircleShape,
                        ambientColor = accentColor.copy(alpha = 0.3f),
                        spotColor = accentColor.copy(alpha = 0.6f)
                    )
                    .background(if (isSelected) bgColor else Color.White, CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.5.dp,
                        color = if (isSelected) accentColor else SoftDivider,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase().toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = if (isSelected) onTextColor else PencilCharcoal
                )

                if (pendingCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(21.dp)
                            .align(Alignment.TopEnd)
                            .background(accentColor, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pendingCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = if (isSelected) accentColor else PencilCharcoal
        )
    }
}

@Composable
fun TaskCard(
    task: Task,
    language: Language,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val checkBgColor by animateColorAsState(
        targetValue = if (task.isCompleted) StitchIndigo else Color.Transparent,
        animationSpec = tween(300),
        label = "checkBgColor"
    )
    val checkBorderColor by animateColorAsState(
        targetValue = if (task.isCompleted) StitchIndigo else Color(0xFFC7D2FE), // border-indigo-200
        animationSpec = tween(300),
        label = "checkBorderColor"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.02f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF9FAFB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Checkbox
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle)
                    .testTag("check_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                        .clip(CircleShape)
                        .background(checkBgColor)
                        .border(
                            width = 2.dp,
                            color = checkBorderColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = LocalizedStrings.get("completed_label", language),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) StitchSlate500.copy(alpha = 0.6f) else StitchSlate800,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profile initials badge
                    if (task.createdByName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xFFEEF2FF)) // bg-indigo-50
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = task.createdByName,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = StitchIndigo
                            )
                        }
                    }
                    
                    // No due date or metadata
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = StitchSlate500.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))
                            drawLine(color = StitchSlate500.copy(alpha = 0.5f), start = center, end = Offset(center.x, center.y - 3.dp.toPx()), strokeWidth = 1.dp.toPx())
                            drawLine(color = StitchSlate500.copy(alpha = 0.5f), start = center, end = Offset(center.x + 2.dp.toPx(), center.y), strokeWidth = 1.dp.toPx())
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "No due date",
                            fontSize = 10.sp,
                            color = StitchSlate500
                        )
                    }
                }
            }

            // Modern Delete Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete)
                    .testTag("delete_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete memo",
                    tint = StitchSlate500.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(vertical = 36.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val animatedAccentColor by animateColorAsState(targetValue = accentColor, label = "emptyAccent")
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(animatedAccentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = animatedAccentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Nothing here yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PencilCharcoal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = PencilGray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PencilCharcoal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = LocalizedStrings.get("completed_title", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = PencilCharcoal
            )
        }
        
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(PencilCharcoal.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PencilCharcoal,
                fontFamily = FontFamily.SansSerif
            )
        }
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

    val isMom = profile == "AMMA" || profile == "user_mom"
    val themeColor = if (isMom) AmmaPrimary else AppaPrimary
    val themeSurfaceColor = if (isMom) AmmaSurface else AppaSurface

    // Mic soundwave pulse concentric rings transition
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha2"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = themeColor.copy(alpha = 0.25f),
                    spotColor = themeColor.copy(alpha = 0.4f)
                )
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header of the modern dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocalizedStrings.get("add_task", language),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = PencilCharcoal
                        )
                    )

                    // Profile label badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(themeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isMom) {
                                LocalizedStrings.get("amma", language)
                            } else {
                                LocalizedStrings.get("appa", language)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = themeColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Premium Outlined Text Field
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = {
                        Text(
                            text = LocalizedStrings.get("enter_task_hint", language),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = PencilGray.copy(alpha = 0.45f)
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .focusRequester(focusRequester)
                        .testTag("add_task_input"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = PencilCharcoal,
                        lineHeight = 23.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor.copy(alpha = 0.7f),
                        unfocusedBorderColor = SoftDivider,
                        focusedContainerColor = Color(0xFFFDFBF7),
                        unfocusedContainerColor = Color(0xFFFDFBF7),
                        focusedTextColor = PencilCharcoal,
                        unfocusedTextColor = PencilCharcoal
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice assistant pulsing hint box (concentric waves)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(themeSurfaceColor, Color.White)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = themeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(onClick = onVoiceClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pulseScale1
                                    scaleY = pulseScale1
                                    alpha = pulseAlpha1
                                }
                                .background(
                                    color = themeColor.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pulseScale2
                                    scaleY = pulseScale2
                                    alpha = pulseAlpha2
                                }
                                .background(
                                    color = themeColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(themeColor.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomMicIcon(
                                color = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = LocalizedStrings.get("voice_hint", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PencilCharcoal
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (language == Language.EN) {
                                "Just tap and speak clearly in English or Tamil!"
                            } else {
                                "இதனைத் தட்டி தமிழ் அல்லது ஆங்கிலத்தில் கூறவும்!"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp,
                                color = PencilGray
                            )
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
                            .height(50.dp)
                            .testTag("add_task_cancel"),
                        border = BorderStroke(1.5.dp, SoftDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PencilCharcoal),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("cancel", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PencilCharcoal
                            )
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
                            .height(50.dp)
                            .shadow(
                                elevation = if (textState.isNotBlank()) 6.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = themeColor.copy(alpha = 0.3f),
                                spotColor = themeColor.copy(alpha = 0.6f)
                            )
                            .testTag("add_task_save"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.White,
                            disabledContainerColor = themeColor.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("save", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = RedPencil.copy(alpha = 0.2f),
                    spotColor = RedPencil.copy(alpha = 0.4f)
                )
                .background(Color.White, RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(RedPencil.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(RedPencil.copy(alpha = 0.16f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = RedPencil,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = LocalizedStrings.get("delete_confirm", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("delete_confirm_cancel"),
                        border = BorderStroke(1.5.dp, SoftDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PencilCharcoal),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("delete_no", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PencilCharcoal
                            )
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = RedPencil.copy(alpha = 0.3f),
                                spotColor = RedPencil.copy(alpha = 0.6f)
                            )
                            .testTag("delete_confirm_ok"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPencil,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("delete_yes", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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
