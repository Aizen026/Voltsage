package com.aizen.voltsage.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizen.voltsage.data.model.ReviewScheduleItem
import com.aizen.voltsage.ui.VoltSageViewModel
import com.aizen.voltsage.ui.theme.SuccessGreen
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltBlue
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltFlame
import com.aizen.voltsage.ui.theme.VoltNeonBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: VoltSageViewModel,
    onLaunchQuiz: (Long) -> Unit,
    onLaunchNotes: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allReviews by viewModel.allReviews.collectAsState()
    val allPacks by viewModel.allPacks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Generate list of 30 days around today (14 days past, today, 15 days future)
    val daysList = remember {
        val list = mutableListOf<Triple<String, String, String>>() // YYYY-MM-DD, DayName, DayNum
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -14)
        
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDayName = SimpleDateFormat("EEE", Locale.getDefault())
        val sdfDayNum = SimpleDateFormat("dd", Locale.getDefault())

        for (i in 0..29) {
            val dateStr = sdfDate.format(cal.time)
            val dayName = sdfDayName.format(cal.time)
            val dayNum = sdfDayNum.format(cal.time)
            list.add(Triple(dateStr, dayName, dayNum))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val reviewsForSelected = allReviews.filter { it.scheduledDate == selectedDate }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = VoltNeonBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 75.dp)
                    .testTag("add_review_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Review")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Calendar Top Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = VoltCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RETENTION CALENDAR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Spaced repetition intervals to maximize memory decay curve",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Streak
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VoltFlame.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = VoltFlame,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${streakDays}d Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltFlame
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Strip (30 days)
                val listState = rememberLazyListState()
                LaunchedEffect(Unit) {
                    listState.scrollToItem(14)
                }
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(daysList) { (dateStr, dayName, dayNum) ->
                        val isSelected = dateStr == selectedDate
                        val hasReviews = allReviews.any { it.scheduledDate == dateStr && !it.completed }

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) VoltBlue else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) VoltCyan else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.setSelectedDate(dateStr) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) VoltCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayNum,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (hasReviews) VoltAmber else Color.Transparent)
                            )
                        }
                    }
                }
            }

            // Reviews List for selected date
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "SCHEDULED REVIEWS FOR $selectedDate (${reviewsForSelected.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                if (reviewsForSelected.isNotEmpty()) {
                    item {
                        val completedCount = reviewsForSelected.count { it.completed }
                        val totalCount = reviewsForSelected.size
                        val dayProgress = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Day Review Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("$completedCount / $totalCount (${(dayProgress * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (dayProgress >= 1f) SuccessGreen else VoltCyan)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { dayProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (dayProgress >= 1f) SuccessGreen else VoltCyan,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }

                if (reviewsForSelected.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = VoltCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Reviews Scheduled for This Date",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "You're all caught up! Use the '+' button to schedule an upcoming recall session.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(reviewsForSelected, key = { it.id }) { review ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleReviewCompleted(review.id, !review.completed) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (review.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Toggle Complete",
                                        tint = if (review.completed) SuccessGreen else MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(VoltCyan.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = review.subject,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltCyan
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(VoltAmber.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = review.reviewType,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VoltAmber
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = review.studyPackTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (review.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (review.reviewType.equals("QUIZ", ignoreCase = true)) {
                                                onLaunchQuiz(review.studyPackId)
                                            } else {
                                                onLaunchNotes(review.studyPackId)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VoltBlue),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = VoltAmber, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Start", fontSize = 11.sp, color = Color.White)
                                    }
                                    
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteReview(review.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Review",
                                            tint = VoltFlame,
                                            modifier = Modifier.size(16.dp)
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

    // Schedule Custom Review Dialog
    if (showAddDialog) {
        var selectedPackId by remember { mutableStateOf(allPacks.firstOrNull()?.id ?: 0L) }
        var reviewType by remember { mutableStateOf("QUIZ") }
        var daysOffset by remember { mutableStateOf(1) }

        val targetPack = allPacks.find { it.id == selectedPackId }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule Spaced Review", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Study Topic:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    allPacks.take(4).forEach { pack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (pack.id == selectedPackId) VoltBlue.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedPackId = pack.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pack.title,
                                fontSize = 12.sp,
                                fontWeight = if (pack.id == selectedPackId) FontWeight.Bold else FontWeight.Normal,
                                color = if (pack.id == selectedPackId) VoltCyan else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text("Review Mode:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("QUIZ", "FLASHCARD", "NOTES").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (reviewType == type) VoltNeonBlue else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { reviewType = type }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    color = if (reviewType == type) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text("Interval:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "+1 Day", 3 to "+3 Days", 7 to "+7 Days").forEach { (offset, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (daysOffset == offset) VoltAmber else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { daysOffset = offset }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (daysOffset == offset) Color(0xFF3F2E00) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetPack != null) {
                            val scheduledDateStr = viewModel.repository.getDateOffsetStr(daysOffset)
                            viewModel.addCustomReview(
                                targetPack.id,
                                targetPack.title,
                                targetPack.subject,
                                scheduledDateStr,
                                reviewType
                            )
                        }
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
