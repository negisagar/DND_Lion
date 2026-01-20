package com.example.dndlion.ui.screens

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dndlion.AlarmScheduler
import com.example.dndlion.DataStoreManager
import com.example.dndlion.DndViewModel
import com.example.dndlion.PermissionHandler
import com.example.dndlion.TimeUtils
import com.example.dndlion.ui.components.DaySelector
import com.example.dndlion.ui.components.DndTimePickerDialog
import com.example.dndlion.ui.components.PermissionDialog
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DndViewModel,
    alarmScheduler: AlarmScheduler,
    permissionHandler: PermissionHandler
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val settings by viewModel.settings.collectAsState(initial = DataStoreManager.DndSettings())

    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    // Sync local state with settings
    LaunchedEffect(settings) {
        if (settings.isActive || startTime.isEmpty()) {
            startTime = settings.startTime
            endTime = settings.endTime
            smsMessage = settings.smsMessage
            selectedDays = settings.selectedDays
        }
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val isFormValid = startTime.isNotEmpty() && endTime.isNotEmpty() && selectedDays.isNotEmpty()

    // Time Picker Dialogs
    if (showStartTimePicker) {
        DndTimePickerDialog(
            title = "Select Start Time",
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                startTime = TimeUtils.formatTime(hour, minute)
                showStartTimePicker = false
            },
            initialHour = TimeUtils.parseTime(startTime)?.get(Calendar.HOUR_OF_DAY) ?: 22,
            initialMinute = TimeUtils.parseTime(startTime)?.get(Calendar.MINUTE) ?: 0
        )
    }

    if (showEndTimePicker) {
        DndTimePickerDialog(
            title = "Select End Time",
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                endTime = TimeUtils.formatTime(hour, minute)
                showEndTimePicker = false
            },
            initialHour = TimeUtils.parseTime(endTime)?.get(Calendar.HOUR_OF_DAY) ?: 7,
            initialMinute = TimeUtils.parseTime(endTime)?.get(Calendar.MINUTE) ?: 0
        )
    }

    PermissionDialog(
        showDialog = showPermissionDialog,
        title = "Permissions Required",
        text = "This app needs permissions to manage Do Not Disturb mode and send auto-replies.",
        onConfirm = {
            showPermissionDialog = false
            permissionHandler.checkAndRequestPermissions()
        },
        onDismiss = { showPermissionDialog = false }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("DND Lion")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status Card with enhanced animation
            AnimatedVisibility(
                visible = settings.isActive,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + 
                        scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 2 },
                exit = fadeOut() + scaleOut() + slideOutVertically { -it / 2 }
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "DND Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$startTime → $endTime",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Time Selection Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Time Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimePickerCard(
                            label = "Start",
                            time = startTime.ifEmpty { "10:00 PM" },
                            icon = Icons.Default.Bedtime,
                            enabled = !settings.isActive,
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerCard(
                            label = "End",
                            time = endTime.ifEmpty { "7:00 AM" },
                            icon = Icons.Default.WbSunny,
                            enabled = !settings.isActive,
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Day Selector
                    DaySelector(
                        selectedDays = selectedDays,
                        onDaySelected = { day ->
                            if (!settings.isActive) {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            }
                        },
                        enabled = !settings.isActive
                    )
                }
            }

            // Message Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Auto-Reply Message",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = smsMessage,
                        onValueChange = { if (!settings.isActive) smsMessage = it },
                        placeholder = { Text("I'm currently unavailable...") },
                        enabled = !settings.isActive,
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            if (settings.isActive) {
                val stopInteractionSource = remember { MutableInteractionSource() }
                val isStopPressed by stopInteractionSource.collectIsPressedAsState()
                val stopScale by animateFloatAsState(
                    targetValue = if (isStopPressed) 0.96f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "stopButtonScale"
                )
                
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        alarmScheduler.stopDndMode(selectedDays)
                        viewModel.updateSettings(settings.copy(isActive = false))
                        Toast.makeText(context, "DND Stopped", Toast.LENGTH_SHORT).show()
                    },
                    interactionSource = stopInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(stopScale)
                        .semantics { contentDescription = "Stop Do Not Disturb mode" },
                    shape = RoundedCornerShape(28.dp), // More expressive corner radius
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.WbSunny, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("Stop DND Mode", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                val scheduleInteractionSource = remember { MutableInteractionSource() }
                val isSchedulePressed by scheduleInteractionSource.collectIsPressedAsState()
                val scheduleScale by animateFloatAsState(
                    targetValue = if (isSchedulePressed) 0.96f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "scheduleButtonScale"
                )
                
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        if (!permissionHandler.checkPermissions()) {
                            showPermissionDialog = true
                        } else if (!permissionHandler.isNotificationPolicyAccessGranted()) {
                            permissionHandler.requestNotificationPolicyAccess()
                        } else {
                            val start = TimeUtils.parseTime(startTime)
                            val end = TimeUtils.parseTime(endTime)
                            if (start != null && end != null) {
                                if (alarmScheduler.scheduleDndMode(start, end, selectedDays)) {
                                    viewModel.updateSettings(
                                        settings.copy(
                                            startTime = startTime,
                                            endTime = endTime,
                                            selectedDays = selectedDays,
                                            smsMessage = smsMessage,
                                            isActive = true
                                        )
                                    )
                                    Toast.makeText(context, "DND Scheduled!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    interactionSource = scheduleInteractionSource,
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(scheduleScale)
                        .semantics { contentDescription = "Schedule Do Not Disturb mode" },
                    shape = RoundedCornerShape(28.dp) // More expressive corner radius
                ) {
                    Icon(
                        Icons.Default.Bedtime, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text("Schedule DND Mode", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimePickerCard(
    label: String,
    time: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                textAlign = TextAlign.Center
            )
        }
    }
}
