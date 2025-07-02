package com.example.pixvi.preview

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pixvi.viewModels.NotificationInfoUi
import com.example.pixvi.viewModels.NotificationViewModel
import com.example.pixvi.viewModels.TaskStatus


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    notificationViewModel: NotificationViewModel = viewModel()
) {
    // State for each collapsible section
    var isDeviceSectionExpanded by remember { mutableStateOf(true) }
    var isApplicationSectionExpanded by remember { mutableStateOf(true) }

    // deviceNotifications flow now includes both regular device notifications and PDF export jobs
    val deviceNotifications by notificationViewModel.deviceNotifications.collectAsState(initial = emptyList())
    val applicationNotifications by notificationViewModel.inAppNotifications.collectAsState(initial = emptyList()) // Renamed for clarity
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    IconButton(onClick = {
                        //notificationViewModel.clearAllNotifications()
                        notificationViewModel.clearAllDeviceNotifications()
                        notificationViewModel.clearAllInAppNotifications()
                        notificationViewModel.clearAllPdfNotifications()
                    }) {
                        Icon(Icons.Filled.ClearAll, contentDescription = "Clear all notifications")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (deviceNotifications.isEmpty() && applicationNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You're all caught up!\nNo new notifications.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Collapsible "Device" Section ---
                // This section will contain regular device notifications and PDF export jobs
                if (deviceNotifications.isNotEmpty()) {
                    item(key = "device_notifications_section") {
                        CollapsibleNotificationSection( // Using a generic section composable
                            title = "Device", // As requested
                            isExpanded = isDeviceSectionExpanded,
                            onToggle = { isDeviceSectionExpanded = !isDeviceSectionExpanded },
                            modifier = Modifier
                                .animateItem()
                                .animateContentSize()
                        ) {
                            NotificationList(
                                notifications = deviceNotifications,
                                onActionClick = { notification, isPrimaryAction ->
                                    // Heuristic or 'type' field to identify PDF jobs
                                    val isPdfJob = notification.author == "PDF Export Service" ||
                                            notification.actionText == "Save" ||
                                            notification.secondaryActionText == "Print" ||
                                            notification.actionText == "Retry" ||
                                            notification.actionText == "Open Folder" ||
                                            notification.actionText == "In Progress" ||
                                            notification.actionText == "Downloading..." ||
                                            notification.actionText == "Saving..."

                                    if (isPdfJob) {
                                        if (isPrimaryAction) {
                                            when (notification.actionText) {
                                                "Save" -> if (notification.taskStatus == TaskStatus.DOWNLOAD_COMPLETE) notificationViewModel.savePdfDirectly(context, notification.id)
                                                "Retry" -> if (notification.taskStatus == TaskStatus.FAILED) notificationViewModel.retryPdfExportJob(notification.id, context)
                                                "Open Folder" -> if (notification.taskStatus == TaskStatus.PDF_SAVED_DIRECTLY) Toast.makeText(context, "Open folder not implemented.", Toast.LENGTH_SHORT).show() // TODO
                                            }
                                        } else { // Secondary Action for PDF Job
                                            if (notification.secondaryActionText == "Print" && notification.taskStatus == TaskStatus.DOWNLOAD_COMPLETE) {
                                                notificationViewModel.triggerPrintPreviewForPdfJob(context, notification.id)
                                            }
                                        }
                                    } else { // Regular device notification
                                        if (isPrimaryAction) {
                                            notificationViewModel.handleDeepLink(context, notification.deepLinkUrl)
                                            notificationViewModel.markNotificationAsRead(notification.id)
                                        }
                                    }
                                },
                                onItemClick = { notification ->
                                    if (notification.author == "PDF Export Service") { // Heuristic
                                        Toast.makeText(context, "Clicked on task: ${notification.message}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                },
                                onDelete = { notificationId ->
                                    notificationViewModel.deleteNotification(notificationId)
                                }
                            )
                        }
                    }
                }

                // --- Collapsible "Application" Section ---
                // This section will contain in-app notifications
                if (applicationNotifications.isNotEmpty()) {
                    item(key = "application_notifications_section") {
                        CollapsibleNotificationSection( // Using the same generic section composable
                            title = "Application", // As requested
                            isExpanded = isApplicationSectionExpanded,
                            onToggle = { isApplicationSectionExpanded = !isApplicationSectionExpanded },
                            modifier = Modifier
                                .animateItem()
                                .animateContentSize()
                        ) {
                            NotificationList(
                                notifications = applicationNotifications,
                                onActionClick = { notification, isPrimaryAction ->
                                    if (isPrimaryAction) { // Assuming in-app only have primary actions
                                        notificationViewModel.handleDeepLink(context, notification.deepLinkUrl)
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                },
                                onItemClick = { notification ->
                                    notificationViewModel.markNotificationAsRead(notification.id)
                                },
                                onDelete = { notificationId ->
                                    notificationViewModel.deleteNotification(notificationId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Generic Collapsible Notification Section Composable ---
// (This replaces your previous DeviceSection and InAppSection if they were separate)
@Composable
fun CollapsibleNotificationSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceVariant, // Or MaterialTheme.colorScheme.surface for a different look
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column { // Content wrapper for AnimatedVisibility
                    Spacer(modifier = Modifier.height(8.dp)) // Space between title and content
                    content()
                }
            }
        }
    }
}


@Composable
fun DeviceSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun InAppSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onToggle() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun NotificationList(
    notifications: List<NotificationInfoUi>,
    onActionClick: (notification: NotificationInfoUi, isPrimaryAction: Boolean) -> Unit, // MODIFIED
    onItemClick: (NotificationInfoUi) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column {
        notifications.forEach { notification ->
            NotificationItem(
                notification = notification,
                onActionClick = onActionClick, // Pass it down
                onItemClick = onItemClick,
                onDelete = onDelete
            )
        }
    }
}

// --- Notification Item Composable ---
@Composable
fun NotificationItem(
    notification: NotificationInfoUi,
    onActionClick: (notification: NotificationInfoUi, isPrimaryAction: Boolean) -> Unit, // Modified to indicate which action
    onItemClick: (NotificationInfoUi) -> Unit,
    onDelete: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClick(notification) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = notification.author, style = MaterialTheme.typography.titleMedium)
                    Text(text = notification.message, style = MaterialTheme.typography.bodyMedium)
                    Text(text = notification.timestamp, style = MaterialTheme.typography.bodySmall)
                }
                if (notification.isDismissible) {
                    IconButton(onClick = { onDelete(notification.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp)) // Placeholder for non-dismissible
                }
            }

            // --- Action Buttons ---
            // Use a Row if both primary and secondary actions can exist together
            if (notification.actionText != null || notification.secondaryActionText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = if (notification.actionText != null && notification.secondaryActionText != null) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary Action Button (Outlined) - e.g., "Print"
                    if (notification.secondaryActionText != null) {
                        OutlinedButton(
                            onClick = { onActionClick(notification, false) }, // isPrimaryAction = false
                            modifier = if (notification.actionText != null) Modifier else Modifier.fillMaxWidth() // Fill width if it's the only button
                        ) {
                            Text(text = notification.secondaryActionText)
                        }
                    }

                    // Spacer if both buttons are present and primary is not null
                    if (notification.actionText != null && notification.secondaryActionText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Primary Action Button (Filled) - e.g., "Save"
                    if (notification.actionText != null) {
                        Button(
                            onClick = { onActionClick(notification, true) }, // isPrimaryAction = true
                            modifier = if (notification.secondaryActionText != null) Modifier else Modifier.fillMaxWidth() // Fill width if it's the only button
                        ) {
                            Text(text = notification.actionText)
                        }
                    }
                }
            }
        }
    }
}
