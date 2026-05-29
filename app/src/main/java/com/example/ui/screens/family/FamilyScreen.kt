package com.example.ui.screens.family

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import com.example.util.LocalizedStrings
import com.example.util.UserSession
import kotlinx.coroutines.delay

@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    session: UserSession,
    onMemberSelected: () -> Unit
) {
    val language by viewModel.curLanguage.collectAsState()
    val curProfile by viewModel.curProfile.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()

    val isNetworkAvailable = remember { mutableStateOf(FirebaseSyncManager.isNetworkAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            isNetworkAvailable.value = FirebaseSyncManager.isNetworkAvailable()
            delay(3000)
        }
    }

    val activeMember = familyMembers.find { it.uid == curProfile }
    val initialLetter = (activeMember?.name ?: session.name).take(1).uppercase()

    StitchFixedPage {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
                Spacer(modifier = Modifier.height(8.dp))

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
                        StitchLazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(familyMembers, key = { it.uid }) { member ->
                                val isSelected = member.uid == curProfile
                                Box(
                                    modifier = Modifier
                                        .animateItem()
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = if (isSelected) 6.dp else 2.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            ambientColor = if (isSelected) StitchIndigo.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.03f),
                                            spotColor = if (isSelected) StitchIndigo.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) StitchIndigo else StitchBorder,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .clickable {
                                            viewModel.setProfile(member.uid)
                                            onMemberSelected()
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
                                            val isDark = isSystemInDarkTheme()
                                            val accentColor = getMemberColor(member.uid, isDark)
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(
                                                        if (isSelected) StitchIndigo else accentColor.copy(alpha = 0.15f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else accentColor,
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
