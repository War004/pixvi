package com.cryptic.piyek.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cryptic.piyek.feature.home.presentation.CurrentAccount
import com.example.app.feature.main.presentation.components.NavigationMenuItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationMenu(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    onCardClick: (Long) -> Unit = {},
    onProfilePicClick: (String?) -> Unit = {},
    onManageAccountsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onAddAccount: () -> Unit = {},
    notificationNumber: Int = 0,
    currentAccount: CurrentAccount,
    content: @Composable (onOpenDrawer: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Scrollable Drawer Items
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .padding(bottom = 200.dp), // Prevents content from hiding behind floating footer
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        NavigationMenuItem(
                            icon = Icons.Outlined.History,
                            label = "History",
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationMenuItem(
                            icon = Icons.Outlined.BookmarkBorder,
                            label = "Bookmark",
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationMenuItem(
                            icon = Icons.Outlined.Notifications,
                            label = "Notifications",
                            badgeCount = notificationNumber,
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationMenuItem(
                            icon = Icons.Outlined.Collections,
                            label = "Collection",
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        NavigationMenuItem(
                            icon = Icons.Outlined.Settings,
                            label = "App Setting",
                            subtitle = "Default Quality, Disable History, Automation",
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationMenuItem(
                            icon = Icons.Outlined.Monitor,
                            label = "Display Setting",
                            subtitle = "Navigator preference, Use Immersive Photo",
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        NavigationMenuItem(
                            icon = Icons.Outlined.Download,
                            label = "Download Manager",
                            subtitle = "Current Downloads, Default Location, Ask save location",
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    // Floating Bottom Profile & Quick Action Section
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserCard(
                            modifier = Modifier.fillMaxWidth(),
                            accountName = currentAccount.accountName?:"Loading...",
                            accountEmail = currentAccount.accountEmail?:"loading@....com",
                            accountId = currentAccount.accountId,
                            accountProfilePicBestLink = currentAccount.profilePicBestUrl,
                            accountBannerBestLink = null,
                            onCardClick = onCardClick,
                            onProfilePicClick = onProfilePicClick
                        )

                        HorizontalFloatingToolbar(
                            expanded = true,
                            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                                toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                toolbarContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp)
                        ) {
                            IconButton(onClick = onManageAccountsClick) {
                                Icon(
                                    imageVector = Icons.Outlined.SwitchAccount,
                                    contentDescription = "Switch Account"
                                )
                            }

                            IconButton(onClick = onAddAccount) {
                                Icon(
                                    imageVector = Icons.Default.PersonAddAlt1,
                                    contentDescription = "Add account"
                                )
                            }

                            IconButton(onClick = onLogoutClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        content {
            scope.launch { drawerState.open() }
        }
    }
}