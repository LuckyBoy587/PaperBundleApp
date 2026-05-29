package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UpdateUiState
import com.example.ui.components.StitchBg
import com.example.ui.components.StitchBorder
import com.example.ui.components.StitchIndigo
import com.example.ui.components.StitchPurple
import com.example.ui.components.StitchRed500
import com.example.ui.components.StitchScrollablePage
import com.example.ui.components.StitchSlate500
import com.example.ui.components.StitchSlate800
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import com.example.util.UserSession
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    session: UserSession,
    onNavigateToFamily: () -> Unit,
    onLogoutComplete: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.curLanguage.collectAsState()
    val curProfile by viewModel.curProfile.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val updateState by viewModel.uiState.collectAsState()

    val isNetworkAvailable = remember { mutableStateOf(FirebaseSyncManager.isNetworkAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            isNetworkAvailable.value = FirebaseSyncManager.isNetworkAvailable()
            delay(3000)
        }
    }

    val activeMember = familyMembers.find { it.uid == curProfile }
    val initialLetter = (activeMember?.name ?: session.name).take(1).uppercase()
    val scrollState = rememberScrollState()

    StitchScrollablePage(
        scrollState = scrollState,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
                // Settings Header Banner Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = StitchIndigo.copy(alpha = 0.1f),
                            spotColor = StitchIndigo.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    StitchIndigo,
                                    StitchPurple
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏡", fontSize = 28.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Baski Family Space",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Manage your shared board and preferences",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Section 1: App Info
                Text(
                    text = "Application Info",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate500,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StitchBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("App Name", fontSize = 14.sp, color = StitchSlate500)
                            Text("PaperBundle App", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchSlate800)
                        }
                        Divider(color = StitchBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Version", fontSize = 14.sp, color = StitchSlate500)
                            Text("Version 2026.1", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchSlate800)
                        }
                        Divider(color = StitchBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("User Session", fontSize = 14.sp, color = StitchSlate500)
                            Text(session.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StitchIndigo)
                        }
                    }
                }

                // Section 2: Preferences
                Text(
                    text = "Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate500,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StitchBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose System Language",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate800
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // English selection card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (language == Language.EN) StitchIndigo.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (language == Language.EN) StitchIndigo else StitchBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setLanguage(Language.EN) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "English",
                                    fontWeight = FontWeight.Bold,
                                    color = if (language == Language.EN) StitchIndigo else StitchSlate800
                                )
                            }

                            // Tamil selection card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (language == Language.TA) StitchIndigo.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (language == Language.TA) StitchIndigo else StitchBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setLanguage(Language.TA) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "தமிழ் (Tamil)",
                                    fontWeight = FontWeight.Bold,
                                    color = if (language == Language.TA) StitchIndigo else StitchSlate800
                                )
                            }
                        }
                    }
                }

                // Section 3: Updates
                Text(
                    text = "System Maintenance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate500,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                val isChecking = updateState is UpdateUiState.Checking

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StitchBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(StitchIndigo.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = StitchIndigo
                                    )
                                } else {
                                    Text("⟳", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StitchIndigo)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Software Updates",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSlate800
                                )
                                Text(
                                    text = "Keep your app up to date with the latest features",
                                    fontSize = 12.sp,
                                    color = StitchSlate500
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = !isChecking,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo)
                        ) {
                            Text(
                                text = if (isChecking) "Checking..." else "Check for Updates",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Section 4: Danger Zone
                Text(
                    text = "Danger Zone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchSlate500,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, StitchRed500.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Leaving the Family board will sign you out of cloud syncing.",
                            fontSize = 12.sp,
                            color = StitchSlate500
                        )

                        Button(
                            onClick = { viewModel.logout(context, onLogoutComplete) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StitchRed500)
                        ) {
                            Text(
                                text = "Logout",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                    }
                }
            }
    }

    // --- Update Checker Feature Dialogs and Overlays ---
    // 1. Update Available Dialog
    if (updateState is UpdateUiState.UpdateAvailable) {
        val state = updateState as UpdateUiState.UpdateAvailable
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🚀", fontSize = 24.sp)
                    Text(
                        text = "New Update Available!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate800
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Current Version", fontSize = 11.sp, color = StitchSlate500)
                            Text(text = state.currentVersion, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StitchSlate800)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Latest Version", fontSize = 11.sp, color = StitchSlate500)
                            Text(text = state.latestVersion, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StitchIndigo)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StitchBorder)
                    )

                    Text(text = "Release Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchSlate800)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .background(StitchBg.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, StitchBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        val subScrollState = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(subScrollState)) {
                            Text(
                                text = state.releaseNotes,
                                fontSize = 12.sp,
                                color = StitchSlate800,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startDownload(state.downloadUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Update Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.resetState() }
                ) {
                    Text(text = "Later", color = StitchSlate500, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // 2. Up To Date Dialog
    if (updateState is UpdateUiState.NoUpdateAvailable) {
        val state = updateState as UpdateUiState.NoUpdateAvailable
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "✨", fontSize = 22.sp)
                    Text(
                        text = "App Up To Date",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate800
                    )
                }
            },
            text = {
                Text(
                    text = "You are already on the latest version (${state.currentVersion}).",
                    fontSize = 14.sp,
                    color = StitchSlate800
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Awesome", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. Update Error Dialog
    if (updateState is UpdateUiState.Error) {
        val state = updateState as UpdateUiState.Error
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚠️", fontSize = 22.sp)
                    Text(
                        text = "Update Check Failed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchRed500
                    )
                }
            },
            text = {
                Text(
                    text = state.message,
                    fontSize = 14.sp,
                    color = StitchSlate800
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. Downloading Progress Overlay
    if (updateState is UpdateUiState.Downloading) {
        val progress = (updateState as UpdateUiState.Downloading).progress
        val percentage = (progress * 100).toInt()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(1.dp, StitchBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Downloading update...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSlate800
                    )

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(100.dp),
                            strokeWidth = 6.dp,
                            color = StitchIndigo,
                            trackColor = StitchIndigo.copy(alpha = 0.15f)
                        )
                        Text(
                            text = "$percentage%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = StitchIndigo
                        )
                    }

                    Text(
                        text = "Please keep the app open",
                        fontSize = 11.sp,
                        color = StitchSlate500
                    )
                }
            }
        }
    }
}
