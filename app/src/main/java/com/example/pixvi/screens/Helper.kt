package com.example.pixvi.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pixvi.network.response.Home.SaveAllFormat
import com.example.pixvi.network.response.Home.SaveDestination

private const val NO_PROFILE_IMAGE_URL = "https://s.pximg.net/common/images/no_profile.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialBottomSheetOptionsMenu(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    isMultiPage: Boolean,
    onSave: (destination: SaveDestination) -> Unit,
    onSaveAll: (format: SaveAllFormat) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column() { // Padding for nav bar
                ListItem(
                    headlineContent = { Text("Save to Device") },
                    leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = "Save to Device") },
                    modifier = Modifier.clickable {
                        onSave(SaveDestination.Device)
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text("Copy to Clipboard") },
                    leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy to Clipboard") },
                    modifier = Modifier.clickable {
                        onSave(SaveDestination.Clipboard)
                        onDismiss()
                    }
                )
                if (isMultiPage) {
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Save All as Image") },
                        leadingContent = { Icon(Icons.Filled.Image, contentDescription = "Save All as Image") },
                        modifier = Modifier.clickable {
                            onSaveAll(SaveAllFormat.Image)
                            onDismiss()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Save All as PDF") },
                        leadingContent = { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Save All as PDF") },
                        modifier = Modifier.clickable {
                            onSaveAll(SaveAllFormat.Pdf)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun ProfileMenu(
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    currentUserName: String,
    currentUserId: String,
    currentUserAvatarUrl: String? = null, // Optional avatar URL
    currentUserNotificationCount: Int = 0,
    onManagePixivAccountClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    if (showMenu) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                ) {
                    // 1. Current User Section
                    AccountInfoRow(
                        userName = currentUserName,
                        userId = currentUserId,
                        avatarUrl = currentUserAvatarUrl,
                        notificationCount = currentUserNotificationCount,
                        isCurrent = true,
                        onClick = { /*  */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    ActionRow(
                        icon = Icons.Default.Notifications,
                        text = "Notifications",
                        onClick = onNotificationsClick
                    )

                    ActionRow(
                        text = "Manage your Pixiv Account",
                        centerText = true,
                        onClick = onManagePixivAccountClick,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInfoRow(
    userName: String,
    userId: String,
    avatarUrl: String?,
    notificationCount: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl == NO_PROFILE_IMAGE_URL || avatarUrl.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "$userName default avatar",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        //set errro image here
                        .build(),
                    contentDescription = "$userName avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "@$userId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(16.dp))

        if (notificationCount > 0) {
            Text(
                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        CircleShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// --- Helper Composable for Action Rows
@Composable
private fun ActionRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    centerText: Boolean = false // To control text alignment
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centerText) Arrangement.Center else Arrangement.Start
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (centerText && icon == null) Modifier.weight(1f) else Modifier, // Allow centering if no icon
            textAlign = if (centerText) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start
        )
    }
}