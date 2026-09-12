package com.aizen.voltsage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aizen.voltsage.ui.theme.SuccessGreen
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltBlue
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltFlame
import com.aizen.voltsage.ui.theme.VoltNeonBlue

@Composable
fun HeroHeader(
    streakDays: Int,
    isStudiedToday: Boolean = false,
    activeModelName: String,
    onOpenModelSelector: () -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStreakDialog by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VoltBlue.copy(alpha = 0.95f),
                            Color(0xFF1C2541)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hero Brand Logo & Tagline (flexible, prevents squishing right-side controls)
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(VoltAmber, VoltFlame))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "VoltSage Emblem",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VOLTSAGE",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VoltCyan.copy(alpha = 0.25f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AI STUDY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltCyan,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Text(
                                text = "Intelligent Study Notes & Recall",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right-side actions: Cloud Sync & Day Streak Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Cloud Sync button
                        IconButton(
                            onClick = onOpenAccount,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .testTag("cloud_sync_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Google Drive Sync",
                                tint = VoltCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Streak Pill (interactive, guaranteed single-line, non-wrapping)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (streakDays > 0) {
                                        Brush.horizontalGradient(
                                            listOf(VoltFlame.copy(alpha = 0.28f), VoltAmber.copy(alpha = 0.15f))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f))
                                        )
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (streakDays > 0) VoltFlame.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { showStreakDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("day_streak_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = if (streakDays > 0) VoltFlame else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streakDays ${if (streakDays == 1) "day" else "days"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isStudiedToday) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Model & AI Engine Quick Control Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable { onOpenModelSelector() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("model_selector_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Model",
                            tint = VoltCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Brain: ",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = activeModelName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltAmber
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Configure / Key",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = VoltCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configure",
                            tint = VoltCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showStreakDialog) {
        StreakDetailsDialog(
            streakDays = streakDays,
            isStudiedToday = isStudiedToday,
            onDismiss = { showStreakDialog = false }
        )
    }
}

@Composable
fun StreakDetailsDialog(
    streakDays: Int,
    isStudiedToday: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(VoltFlame, VoltAmber)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Fire Streak",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "$streakDays Day Study Streak",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (streakDays > 0) {
                        "You're building an unstoppable learning habit!"
                    } else {
                        "Start your learning journey today!"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStudiedToday) {
                            SuccessGreen.copy(alpha = 0.12f)
                        } else {
                            VoltAmber.copy(alpha = 0.12f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isStudiedToday) Icons.Default.CheckCircle else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isStudiedToday) SuccessGreen else VoltAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isStudiedToday) "Today's Streak Secured!" else "Action Needed Today",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStudiedToday) SuccessGreen else VoltFlame
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isStudiedToday) {
                                    "Great job! You've studied today. Your streak is safe for tomorrow."
                                } else {
                                    "Take a quick quiz or generate study notes to maintain your flame!"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Streaks automatically increase every consecutive calendar day you review notes or complete a quiz.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                ) {
                    Text(text = "Keep Studying", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
