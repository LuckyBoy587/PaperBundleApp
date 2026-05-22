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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import kotlin.math.absoluteValue

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC), // Slate 50
                        Color(0xFFF1F5F9)  // Slate 100
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Modern Glowing Visual Logo (Abstract Circular Gradient representing Amma & Appa unity)
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                    .background(Color.White, shape = RoundedCornerShape(24.dp))
                    .drawBehind {
                        // Ambient glowing gradient ring on border
                        drawRoundRect(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    AmmaPrimary,
                                    AppaPrimary,
                                    AmmaPrimary
                                )
                            ),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📌",
                    fontSize = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = -2.dp.toPx()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = LocalizedStrings.get("title", language),
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = PencilCharcoal,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = LocalizedStrings.get("app_desc", language),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = PencilGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Main Google Sign in Button - Sleek capsule with modern elevation and clean border
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
                            } else {
                                Log.d("PAPER_BUNDLE", "GoogleLoginScreen: Web Client ID not found in resources. Proceeding without ID Token request.")
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SoftDivider),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Google Modern Logo Representation
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFF1F5F9), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "G",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = LocalizedStrings.get("sign_in_google", language),
                        fontSize = 16.sp,
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC), // Slate 50
                        Color(0xFFF1F5F9)  // Slate 100
                    )
                )
            )
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
                    .padding(vertical = 16.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient Profile Initials avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AmmaPrimary, AppaPrimary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.name.first().toString().uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Hello, ${session.name}!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = session.email,
                        fontSize = 14.sp,
                        color = PencilGray,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Divider(color = SoftDivider, modifier = Modifier.padding(bottom = 24.dp))

            // Card 1: Create Family board
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SoftDivider),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = LocalizedStrings.get("create_family_title", language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = LocalizedStrings.get("create_family_desc", language),
                        fontSize = 14.sp,
                        color = PencilGray,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = familyNameInput,
                        onValueChange = { familyNameInput = it },
                        placeholder = { Text(LocalizedStrings.get("family_name_hint", language), fontSize = 14.sp, color = PencilGray.copy(alpha = 0.7f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_family_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmmaPrimary,
                            unfocusedBorderColor = SoftDivider,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedTextColor = PencilCharcoal,
                            unfocusedTextColor = PencilCharcoal
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.handleCreateFamily(context, familyNameInput) {
                                Toast.makeText(context, "Welcome to your family workspace!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = familyNameInput.isNotBlank() && !authLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(2.dp, shape = RoundedCornerShape(26.dp))
                            .testTag("create_family_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmmaPrimary,
                            disabledContainerColor = AmmaPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = LocalizedStrings.get("create_button", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modern Divider OR indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Divider(modifier = Modifier.weight(1f), color = SoftDivider)
                Text(
                    text = LocalizedStrings.get("or_label", language),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PencilGray,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Divider(modifier = Modifier.weight(1f), color = SoftDivider)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: Join Family board
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SoftDivider),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = LocalizedStrings.get("join_family_title", language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PencilCharcoal,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = LocalizedStrings.get("join_family_desc", language),
                        fontSize = 14.sp,
                        color = PencilGray,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = it },
                        placeholder = { Text(LocalizedStrings.get("invite_code_hint", language), fontSize = 14.sp, color = PencilGray.copy(alpha = 0.7f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_family_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppaPrimary,
                            unfocusedBorderColor = SoftDivider,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedTextColor = PencilCharcoal,
                            unfocusedTextColor = PencilCharcoal
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.handleJoinFamily(context, inviteCodeInput) {
                                Toast.makeText(context, "Connected to your family board!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = inviteCodeInput.isNotBlank() && !authLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(2.dp, shape = RoundedCornerShape(26.dp))
                            .testTag("join_family_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppaPrimary,
                            disabledContainerColor = AppaPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = LocalizedStrings.get("join_button", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Disconnect/Logout button at bottom
            OutlinedButton(
                onClick = { viewModel.logout(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_profile_button"),
                border = BorderStroke(1.5.dp, SoftDivider),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PencilCharcoal)
            ) {
                Text(
                    text = LocalizedStrings.get("logout_button", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif
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
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.85f)
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
            // User photo / avatar - modern gradient look
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AmmaPrimary,
                                AppaPrimary
                            )
                        )
                    )
                    .clickable {
                        userSession?.familyInviteCode?.let { code ->
                            clipboardManager.setText(AnnotatedString(code))
                            Toast.makeText(context, "Invite Code Copied! Send it to your family.", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userSession?.name?.firstOrNull()?.uppercase().toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = userSession?.familyName ?: LocalizedStrings.get("title", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = PencilCharcoal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                userSession?.familyInviteCode?.let { code ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Invite Code Copied: $code", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(LocalizedStrings.get("invite_code_banner", language), code),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = PencilGray
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
                    .background(Color(0xFFF1F5F9))
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

            // Tactile modern sign-out icon from the notice board
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = PencilCharcoal,
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
fun PaperHangerRow(
    language: Language,
    curProfile: String,
    onProfileSelect: (String) -> Unit,
    familyMembers: List<com.example.util.FamilyMember>,
    memberPendingCounts: Map<String, Int>
) {
    Column {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            familyMembers.forEachIndexed { index, member ->
                val pendingCount = memberPendingCounts[member.uid] ?: 0
                val accentColor = if (index % 2 == 0) AmmaPrimary else AppaPrimary
                val bgColor = if (index % 2 == 0) AmmaSurface else AppaSurface
                val onTextColor = if (index % 2 == 0) AmmaOnSurface else AppaOnSurface

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
                    rotationAngle = 0f,
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
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.02f else 0.96f, label = "scale")
    val alpha by animateFloatAsState(targetValue = if (isSelected) 1.0f else 0.75f, label = "alpha")
    val translationY by animateDpAsState(targetValue = if (isSelected) (-4).dp else 0.dp, label = "offset")

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationY.toPx()
                this.alpha = alpha
            }
            .clickable(onClick = onClick)
            .testTag(testTag)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = accentColor.copy(alpha = 0.3f),
                spotColor = accentColor.copy(alpha = 0.5f)
            ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) bgColor else Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, if (isSelected) accentColor else SoftDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant modern indicator: simple clean colored dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else PencilGray.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = if (isSelected) onTextColor else PencilCharcoal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Modern capsule badge for remaining count
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accentColor else PencilGray,
                    fontFamily = FontFamily.SansSerif
                )
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
    val ownerAccentColor = if (task.profileOwner == "AMMA" || task.profileOwner == "user_mom") AmmaPrimary else AppaPrimary
    val ownerBgColor = if (task.profileOwner == "AMMA" || task.profileOwner == "user_mom") AmmaSurface else AppaSurface

    val checkBgColor by animateColorAsState(
        targetValue = if (task.isCompleted) ownerAccentColor else Color.Transparent,
        label = "checkBgColor"
    )
    val checkBorderColor by animateColorAsState(
        targetValue = if (task.isCompleted) ownerAccentColor else PencilGray.copy(alpha = 0.4f),
        label = "checkBorderColor"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 1.0f else 0.9f,
        label = "checkScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = PaperCardLight),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SoftDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant left Owner accent indicator stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(ownerAccentColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Massive 48dp check target with smooth animations
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
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(checkBgColor)
                        .border(
                            width = 2.dp,
                            color = checkBorderColor,
                            shape = CircleShape
                        )
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = LocalizedStrings.get("completed_label", language),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Task title and styled badge row
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) PencilGray.copy(alpha = 0.55f) else PencilCharcoal,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Real-time added/completed member premium badge pills
                if (task.createdByName.isNotBlank() || task.completedByName?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ownerBgColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (task.isCompleted && task.completedByName != null) {
                                String.format(LocalizedStrings.get("completed_by", language), task.completedByName)
                            } else {
                                String.format(LocalizedStrings.get("added_by", language), task.createdByName)
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = ownerAccentColor
                        )
                    }
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
                    modifier = Modifier.size(20.dp)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SoftDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-end glowing empty state icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message,
                fontSize = 16.sp,
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = PencilGray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = LocalizedStrings.get("completed_title", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = PencilCharcoal
            )
        }
        
        // Count badge pill
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(PencilGray.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
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

    // Mic soundwave pulse transition
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = themeColor.copy(alpha = 0.2f),
                    spotColor = themeColor.copy(alpha = 0.3f)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, themeColor.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header of the modern dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocalizedStrings.get("add_task", language),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PencilCharcoal
                        )
                    )

                    // Profile label badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
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
                                fontSize = 16.sp,
                                color = PencilGray.copy(alpha = 0.5f)
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .focusRequester(focusRequester)
                        .testTag("add_task_input"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        color = PencilCharcoal,
                        lineHeight = 24.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = SoftDivider,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedTextColor = PencilCharcoal,
                        unfocusedTextColor = PencilCharcoal
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice assistant pulsing hint box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(themeSurfaceColor, Color.White)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = themeColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onVoiceClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing microphone container
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glowing pulsing wave circle
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .background(
                                    color = themeColor.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        )

                        // Base inner circle
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
                        shape = RoundedCornerShape(14.dp)
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
                                elevation = if (textState.isNotBlank()) 4.dp else 0.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = themeColor.copy(alpha = 0.3f),
                                spotColor = themeColor.copy(alpha = 0.5f)
                            )
                            .testTag("add_task_save"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor,
                            contentColor = Color.White,
                            disabledContainerColor = themeColor.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(14.dp)
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = RedPencil.copy(alpha = 0.2f),
                    spotColor = RedPencil.copy(alpha = 0.3f)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, RedPencil.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modern glowing danger trash icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(RedPencil.copy(alpha = 0.12f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(RedPencil.copy(alpha = 0.18f), shape = CircleShape),
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
                        shape = RoundedCornerShape(14.dp)
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
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = RedPencil.copy(alpha = 0.3f),
                                spotColor = RedPencil.copy(alpha = 0.5f)
                            )
                            .testTag("delete_confirm_ok"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPencil,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
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
