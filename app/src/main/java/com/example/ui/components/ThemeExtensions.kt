package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SyncState
import com.example.util.Language
import com.example.util.LocalizedStrings

// Stitch Dashboard Colors - Dynamic Material 3 properties for dynamic theme support
val StitchBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val StitchBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
val StitchIndigo: Color @Composable get() = MaterialTheme.colorScheme.primary
val StitchPurple: Color @Composable get() = MaterialTheme.colorScheme.tertiary
val StitchSlate800: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val StitchSlate500: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val StitchGray100: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val StitchRed50: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
val StitchRed500: Color @Composable get() = MaterialTheme.colorScheme.error
val StitchGreen50: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
val StitchGreen500: Color @Composable get() = MaterialTheme.colorScheme.secondary
val StitchBlue50: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val StitchBlue500: Color @Composable get() = MaterialTheme.colorScheme.primary

// Vibrant modern accent colors for family members
val MemberColorsLight = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink/Rose
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF06B6D4), // Cyan
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEF4444)  // Red
)

val MemberColorsDark = listOf(
    Color(0xFF818CF8), // Luminous Indigo
    Color(0xFFF472B6), // Luminous Pink/Rose
    Color(0xFFFBBF24), // Luminous Amber
    Color(0xFF34D399), // Luminous Emerald
    Color(0xFF22D3EE), // Luminous Cyan
    Color(0xFFA78BFA), // Luminous Violet
    Color(0xFFF87171)  // Luminous Red
)

fun getMemberColor(uid: String, isDark: Boolean): Color {
    val colors = if (isDark) MemberColorsDark else MemberColorsLight
    val index = Math.abs(uid.hashCode()) % colors.size
    return colors[index]
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App title
        Text(
            text = LocalizedStrings.get("title", language),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = StitchSlate800
        )
        
        // Quick Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language Button - Canvas-based Vector
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onLanguageClick() },
                contentAlignment = Alignment.Center
            ) {
                GlobeNavIcon(tint = StitchIndigo, modifier = Modifier.size(18.dp))
            }
            
            // User letter badge (dynamic avatar)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(StitchIndigo, StitchPurple)))
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
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
    val isDark = isSystemInDarkTheme()
    val bgColor = when {
        !isFirebaseInitialized -> MaterialTheme.colorScheme.surfaceVariant
        !isNetworkAvailable -> if (isDark) Color(0xFF3E2D00) else Color(0xFFFEF3C7)
        hasPendingWrites -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        !isFirebaseInitialized -> MaterialTheme.colorScheme.onSurfaceVariant
        !isNetworkAvailable -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
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

    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
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

@Composable
fun DeleteConfirmDialog(
    language: Language,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
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
        label = "deleteDialogScale"
    )
    val alphaVal by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "deleteDialogAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = alphaVal
                }
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(text = LocalizedStrings.get("delete_no", language))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchRed500)
                    ) {
                        Text(text = LocalizedStrings.get("delete_yes", language), color = MaterialTheme.colorScheme.onError)
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
        label = "familyDialogScale"
    )
    val alphaVal by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "familyDialogAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = alphaVal
                }
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
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) StitchIndigo else MaterialTheme.colorScheme.surfaceVariant,
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

// ---------------------------------------------------------
// Modular Page Containers to ensure layout integrity
// ---------------------------------------------------------

@Composable
fun StitchScrollablePage(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBg)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .padding(top = 12.dp),
            verticalArrangement = verticalArrangement
        ) {
            content()
            Spacer(modifier = Modifier.height(24.dp)) // Premium bottom breathing room
        }
    }
}

@Composable
fun StitchFixedPage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchBg)
    ) {
        content()
    }
}

@Composable
fun StitchLazyColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    contentPadding: PaddingValues = PaddingValues(top = 8.dp, bottom = 24.dp),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        content = content
    )
}
