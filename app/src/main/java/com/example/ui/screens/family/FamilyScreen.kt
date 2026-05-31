package com.example.ui.screens.family

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StitchBlue50
import com.example.ui.components.StitchBlue500
import com.example.ui.components.StitchBorder
import com.example.ui.components.StitchFixedPage
import com.example.ui.components.StitchGray100
import com.example.ui.components.StitchGreen50
import com.example.ui.components.StitchGreen500
import com.example.ui.components.StitchIndigo
import com.example.ui.components.StitchLazyColumn
import com.example.ui.components.StitchRed50
import com.example.ui.components.StitchRed500
import com.example.ui.components.StitchSlate500
import com.example.ui.components.StitchSlate800
import com.example.ui.components.getMemberColor
import com.example.util.FirebaseSyncManager
import com.example.util.Language
import com.example.util.UserSession
import kotlinx.coroutines.delay

@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    session: UserSession,
    onMemberSelected: () -> Unit
) {
    val language by viewModel.curLanguage.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    val isNetworkAvailable = remember { mutableStateOf(FirebaseSyncManager.isNetworkAvailable()) }
    LaunchedEffect(Unit) {
        while (true) {
            isNetworkAvailable.value = FirebaseSyncManager.isNetworkAvailable()
            delay(3000)
        }
    }

    StitchFixedPage {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // Beautiful View-Only Premium Header
            val headerTitle =
                if (language == Language.TA) "குடும்ப உறுப்பினர்கள்" else "Family Members"
            val headerSubtitle = if (language == Language.TA) {
                "உறுப்பினர்களின் தற்போதைய செயல்பாடுகள் மற்றும் பணி முடித்த விபரங்கள்."
            } else {
                "View active task checklists and real-time progress across your household."
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = headerTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StitchSlate800,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = headerSubtitle,
                    fontSize = 13.sp,
                    color = StitchSlate500,
                    fontWeight = FontWeight.Normal
                )
            }

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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 120.dp), // Breathing room for navigation
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(familyMembers, key = { it.uid }) { member ->
                            val isCurrentUser = member.uid == session.uid
                            val isDark = isSystemInDarkTheme()
                            val accentColor = getMemberColor(member.uid, isDark)

                            // Dynamic theme-aware colors for perfect readability & premium contrast
                            val titleColor = StitchSlate800
                            val subtitleColor = StitchSlate500
                            val labelColor = StitchSlate500
                            val activeNumberColor = StitchSlate800

                            // Fetch task counts
                            val memberTasks = allTasks.filter { it.profileOwner == member.uid }
                            val activeTasksCount = memberTasks.count { !it.isCompleted }
                            val completedTasksCount = memberTasks.count { it.isCompleted }
                            val totalTasks = memberTasks.size

                            // Infer urgent tasks (e.g. contains 'urgent', 'asap', '! ' in title)
                            val hasUrgentTasks = memberTasks.any {
                                !it.isCompleted && (it.title.contains(
                                    "urgent",
                                    ignoreCase = true
                                ) || it.title.contains(
                                    "asap",
                                    ignoreCase = true
                                ) || it.title.contains("!") || it.title.contains(
                                    "important",
                                    ignoreCase = true
                                ))
                            }

                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.04f),
                                        spotColor = Color.Black.copy(alpha = 0.06f)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrentUser) StitchIndigo.copy(alpha = 0.5f) else StitchBorder.copy(
                                            alpha = 0.5f
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Header Row: Avatar, Name & Role, Status Dot
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Beautiful Avatar with a border and subtle highlight
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .background(
                                                        accentColor.copy(alpha = 0.12f),
                                                        CircleShape
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = accentColor.copy(alpha = 0.3f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    color = accentColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 20.sp
                                                )
                                            }

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = titleColor
                                                )

                                                // Dynamically map role
                                                val roleSuffix = if (isCurrentUser) " (You)" else ""
                                                val role = when {
                                                    member.name.contains(
                                                        "amma",
                                                        ignoreCase = true
                                                    ) -> "Mother (Family Admin)$roleSuffix"

                                                    member.name.contains(
                                                        "appa",
                                                        ignoreCase = true
                                                    ) -> "Father (Co-Admin)$roleSuffix"

                                                    member.name.contains(
                                                        "kowsh",
                                                        ignoreCase = true
                                                    ) || member.email.contains(
                                                        "kowsh",
                                                        ignoreCase = true
                                                    ) -> "Developer / Creator$roleSuffix"

                                                    else -> "Family Contributor$roleSuffix"
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            accentColor.copy(alpha = 0.08f),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = role,
                                                        color = accentColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // Status Indicator dot
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val statusColor = when {
                                                hasUrgentTasks -> StitchRed500
                                                activeTasksCount > 0 -> StitchBlue500
                                                totalTasks > 0 && activeTasksCount == 0 -> StitchGreen500
                                                else -> StitchSlate500
                                            }

                                            val statusLabel = when {
                                                hasUrgentTasks -> if (language == Language.TA) "கவனம் தேவை" else "Needs Attention"
                                                activeTasksCount > 0 -> if (language == Language.TA) "செயலில் உள்ளது" else "In Progress"
                                                totalTasks > 0 && activeTasksCount == 0 -> if (language == Language.TA) "முடிக்கப்பட்டது" else "On Track"
                                                else -> if (language == Language.TA) "குறிப்புகள் இல்லை" else "On Track"
                                            }

                                            val statusBg = when {
                                                hasUrgentTasks -> StitchRed50.copy(alpha = 0.5f)
                                                activeTasksCount > 0 -> StitchBlue50.copy(alpha = 0.5f)
                                                totalTasks > 0 && activeTasksCount == 0 -> StitchGreen50.copy(
                                                    alpha = 0.5f
                                                )

                                                else -> StitchGray100.copy(alpha = 0.5f)
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .background(statusBg, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(statusColor, CircleShape)
                                                )
                                                Text(
                                                    text = statusLabel,
                                                    color = statusColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Sleek divider to split metrics block
                                    HorizontalDivider(
                                        color = StitchBorder.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = 14.dp)
                                    )

                                    // Activity Metrics block
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "$activeTasksCount",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = activeNumberColor
                                                )
                                                Text(
                                                    text = "Active Tasks",
                                                    fontSize = 11.sp,
                                                    color = labelColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "$completedTasksCount",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (completedTasksCount > 0) StitchGreen500 else activeNumberColor
                                                )
                                                Text(
                                                    text = "Completed",
                                                    fontSize = 11.sp,
                                                    color = labelColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (totalTasks > 0) {
                                            val progress =
                                                completedTasksCount.toFloat() / totalTasks.toFloat()
                                            val percentage = (progress * 100).toInt()

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    Text(
                                                        text = "$percentage%",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = activeNumberColor
                                                    )
                                                    Text(
                                                        text = "Done",
                                                        fontSize = 10.sp,
                                                        color = labelColor,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(bottom = 1.dp)
                                                    )
                                                }

                                                LinearProgressIndicator(
                                                    progress = { progress },
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.9f)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = StitchGreen500,
                                                    trackColor = StitchBorder.copy(alpha = 0.3f),
                                                    strokeCap = StrokeCap.Round
                                                )
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "No Tasks",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = subtitleColor
                                                )
                                                Text(
                                                    text = "All clear!",
                                                    fontSize = 10.sp,
                                                    color = labelColor,
                                                    fontWeight = FontWeight.Normal
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
