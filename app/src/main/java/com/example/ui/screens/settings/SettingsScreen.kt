package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Settings Header Banner Card - Sleek & Shrunken by 40-50%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = StitchIndigo.copy(alpha = 0.08f),
                    spotColor = StitchIndigo.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            StitchIndigo,
                            StitchPurple
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏡", fontSize = 20.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Baski Family Space",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Manage your shared board and preferences",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Section: Preferences & System Maintenance
        Text(
            text = "System Preferences",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = StitchSlate500,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, StitchBorder)
        ) {
            Column {
                // Item 1: Language selection row (placed side-by-side)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Language",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchSlate800
                        )
                        Text(
                            text = "Select system display language",
                            fontSize = 11.sp,
                            color = StitchSlate500
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // English Selection Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (language == Language.EN) StitchIndigo.copy(alpha = 0.12f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (language == Language.EN) StitchIndigo else StitchBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setLanguage(Language.EN) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "English",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (language == Language.EN) StitchIndigo else StitchSlate800
                            )
                        }

                        // Tamil Selection Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (language == Language.TA) StitchIndigo.copy(alpha = 0.12f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (language == Language.TA) StitchIndigo else StitchBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setLanguage(Language.TA) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "தமிழ்",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (language == Language.TA) StitchIndigo else StitchSlate800
                            )
                        }
                    }
                }

                Divider(color = StitchBorder, modifier = Modifier.padding(horizontal = 16.dp))

                // Item 2: Software Updates row (compact single row)
                val isChecking = updateState is UpdateUiState.Checking
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(StitchIndigo.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = StitchIndigo
                                )
                            } else {
                                Text(
                                    "⟳",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchIndigo
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Software Updates",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSlate800
                            )
                            Text(
                                text = if (isChecking) "Checking for updates..." else "Keep the app up to date",
                                fontSize = 11.sp,
                                color = StitchSlate500
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.checkForUpdates() },
                        enabled = !isChecking,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchIndigo)
                    ) {
                        Text(
                            text = if (isChecking) "Checking" else "Check",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Section: Danger Zone
        Text(
            text = "Danger Zone",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = StitchSlate500,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, StitchRed500.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Logout from Family Space",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchRed500
                    )
                    Text(
                        text = "Will sign you out of cloud syncing",
                        fontSize = 11.sp,
                        color = StitchSlate500
                    )
                }

                Button(
                    onClick = { viewModel.logout(context, onLogoutComplete) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchRed500)
                ) {
                    Text(
                        text = "Logout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtle footer for App Info to save space
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "PaperBundle App • Version 2026.1",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = StitchSlate500
            )
            Text(
                text = "Session: ${session.name}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StitchIndigo.copy(alpha = 0.8f)
            )
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
