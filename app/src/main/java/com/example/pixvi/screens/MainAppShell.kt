package com.example.pixvi.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.pixvi.R
import com.example.pixvi.login.AuthViewModel
import com.example.pixvi.login.LoginState
import com.example.pixvi.network.response.AppLoading.CurrentAccountManager
import com.example.pixvi.utils.ContentRoutes
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.utils.PixivAsyncImage
import com.example.pixvi.viewModels.MainAppShellViewModel
import androidx.core.net.toUri
import com.example.pixvi.repo.BatterySaverThemeRepository
import com.example.pixvi.settings.SettingsRepository


private const val NO_PROFILE_IMAGE_URL = "https://s.pximg.net/common/images/no_profile.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    authViewModel: AuthViewModel,
    contentNavController: NavController,
    rootNavController: NavController,
    pixivApiService: PixivApiService,
    logoutEvent: () -> Unit,
    settingsRepository: SettingsRepository,
    batterySaverThemeRepository: BatterySaverThemeRepository,
    viewModel: MainAppShellViewModel = remember { MainAppShellViewModel(pixivApiService,settingsRepository,batterySaverThemeRepository) },
    content: @Composable (PaddingValues) -> Unit
) {
    val TAG = "MainAppShell"
    Log.d(TAG, "MainAppShell composing - rootNavController: ${rootNavController.hashCode()}")

    val loginState by authViewModel.loginState.collectAsState()
    val showProfileMenu by viewModel.showProfileMenu.collectAsState()
    val showHomeDropdownMenu by viewModel.showHomeDropdownMenu.collectAsState()
    val navigatingToLogin by viewModel.navigatingToLogin.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchFieldFocused by viewModel.isSearchFieldFocused.collectAsState()

    val url = "https://github.com/War004/pixvi/tags"
    val context = LocalContext.current

    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentContentRoute = navBackStackEntry?.destination?.route

    val userBatteryChoice by settingsRepository.isBatterySaver.collectAsState()

    //Icon based on current screen
    val iconPainter: Painter = when (currentContentRoute) {
        ContentRoutes.ILLUSTRATIONS -> painterResource(R.drawable.imagesmode_24px)
        ContentRoutes.MANGA -> rememberVectorPainter(image = Icons.Outlined.PhotoAlbum)
        ContentRoutes.NOVEL -> rememberVectorPainter(image = Icons.AutoMirrored.Outlined.LibraryBooks)
        ContentRoutes.NEWEST -> rememberVectorPainter(image = Icons.Outlined.FiberNew)
        ContentRoutes.RANKING -> rememberVectorPainter(image = Icons.Outlined.BarChart)
        else -> painterResource(R.drawable.imagesmode_24px) // Default icon
    }

    LaunchedEffect(loginState, navigatingToLogin) {
        if (loginState !is LoginState.Success && !navigatingToLogin) {
            viewModel.setNavigatingToLogin(true)
            rootNavController.navigate("LoginScreen") {
                popUpTo(rootNavController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        } else if (loginState is LoginState.Success) {
            viewModel.setNavigatingToLogin(false)
        }
    }

    if (loginState is LoginState.Success) {

        val currentUser by CurrentAccountManager.currentAccount.collectAsStateWithLifecycle()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val textFieldState = rememberTextFieldState()
        val scrollState = rememberScrollState()
        val focusManager = LocalFocusManager.current

        val interactionSource = remember { MutableInteractionSource() }
        val isTextFieldFocused by interactionSource.collectIsFocusedAsState()

        // Update ViewModel when focus changes
        LaunchedEffect(isTextFieldFocused) {
            viewModel.setSearchFieldFocused(isTextFieldFocused)
        }

        // Update ViewModel when text changes
        LaunchedEffect(textFieldState.text) {
            viewModel.updateSearchQuery(textFieldState.text.toString())
        }

        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ){
                        Column (
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(end = 20.dp),
                        ) {
                            Text("Quick Options", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                            NavigationDrawerItem(
                                label = {Text("Bookmarks")},
                                icon = {Icon(Icons.Default.FavoriteBorder,"Option to show bookmarks")},
                                selected = false,
                                onClick ={
                                    viewModel.handleDrawerItemClick {
                                        Toast.makeText(context, "Bookmarks Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            )
                            NavigationDrawerItem(
                                label = {Text("History")},
                                icon = {Icon(Icons.Default.History,"Option to show history")},
                                selected = false,
                                onClick ={
                                    viewModel.handleDrawerItemClick {
                                        Toast.makeText(context, "History Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            )
                            NavigationDrawerItem(
                                label = {Text("Filtered Work History")},
                                icon = {Icon(Icons.Default.HistoryToggleOff,"Option to show the list of filtered through custom tags")},
                                selected = false,
                                onClick ={
                                    viewModel.handleDrawerItemClick {
                                        Toast.makeText(context, "Filtered Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            )
                            NavigationDrawerItem(
                                label = {Text("Settings")},
                                icon = {Icon(Icons.Default.Settings,"Option to show setting menu")},
                                selected = false,
                                onClick ={
                                    viewModel.handleDrawerItemClick {
                                        Toast.makeText(context, "Setting Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            NavigationDrawerItem(
                                label = { Text("Illustrations") },
                                icon = { Icon(painterResource(R.drawable.imagesmode_24px), "Illustrations") },
                                selected = currentContentRoute == ContentRoutes.ILLUSTRATIONS,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    contentNavController.navigate(ContentRoutes.ILLUSTRATIONS) {
                                        popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationDrawerItem(
                                label = {Text("Manga")},
                                icon = {Icon(Icons.Filled.PhotoAlbum,"Option to show following manga")},
                                selected = currentContentRoute == ContentRoutes.MANGA,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    contentNavController.navigate(ContentRoutes.MANGA) {
                                        popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                            NavigationDrawerItem(
                                label = {Text("Novel")},
                                icon = {Icon(Icons.AutoMirrored.Filled.LibraryBooks,"Option to show following novel")},
                                selected = currentContentRoute == ContentRoutes.NOVEL,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    contentNavController.navigate(ContentRoutes.NOVEL) {
                                        popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .background(
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color.Transparent//MaterialTheme.colorScheme.surface
                                )
                                .fillMaxWidth(0.90f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            IconButton(
                                onClick = {
                                    Log.d("MainAppShell Logout Event","Button Pressed")
                                    logoutEvent()
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Logout,
                                    contentDescription = "Logout Button",
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                            context.startActivity(intent)
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ){
                                Text(
                                    text = "App version: 1.xx",
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open github page",
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.changeUserBatteryTheme(!userBatteryChoice)
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.size(56.dp)
                            ) {
                                if (userBatteryChoice) {
                                    Icon(
                                        imageVector = Icons.Default.BatterySaver,
                                        contentDescription = "Theme is based on battery saver: Press the button to on",
                                        modifier = Modifier.size(28.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.BatteryFull,
                                        contentDescription = "Theme is not based on battery saver: Press the button to off",
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            drawerState = drawerState
        ){
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        title = {
                            BasicTextField(
                                state = textFieldState,
                                scrollState = scrollState,
                                modifier = Modifier.fillMaxWidth(),
                                interactionSource = interactionSource,
                                textStyle = LocalTextStyle.current.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                lineLimits = TextFieldLineLimits.SingleLine,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                onKeyboardAction = {
                                    viewModel.performSearch()
                                    focusManager.clearFocus()
                                },
                                decorator = { innerTextField ->
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { /*TODO filter logic to be done*/ }) {
                                            Icon(
                                                painter = painterResource(R.drawable.filter_icon),
                                                contentDescription = "Tune Icon"
                                            )
                                        }

                                        Box(modifier = Modifier
                                            .padding(end = 15.dp)
                                        ) {
                                            if (textFieldState.text.isEmpty()) {
                                                Text(
                                                    text = "Search or enter URL",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.open() }
                                })
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Menu,
                                    contentDescription = "Menu Icon"
                                )
                            }
                        },
                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Box {
                                    IconButton(onClick = { viewModel.setShowHomeDropdownMenu(true) }) {
                                        Icon(painter = iconPainter, contentDescription = "Selected screen icon")
                                    }
                                    DropdownMenu(
                                        expanded = showHomeDropdownMenu,
                                        onDismissRequest = { viewModel.setShowHomeDropdownMenu(false) }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Illustrations") },
                                            onClick = {
                                                viewModel.handleHomeDropdownAction {
                                                    if (currentContentRoute != ContentRoutes.ILLUSTRATIONS) {
                                                        contentNavController.navigate(ContentRoutes.ILLUSTRATIONS) {
                                                            popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(painterResource(R.drawable.imagesmode_24px), "Illustrations") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Manga") },
                                            onClick = {
                                                viewModel.handleHomeDropdownAction{
                                                    if (currentContentRoute != ContentRoutes.MANGA) {
                                                        contentNavController.navigate(ContentRoutes.MANGA) {
                                                            popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.PhotoAlbum, "Manga") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Novel") },
                                            onClick = {
                                                viewModel.handleHomeDropdownAction{
                                                    if (currentContentRoute != ContentRoutes.NOVEL) {
                                                        contentNavController.navigate(ContentRoutes.NOVEL) {
                                                            popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, "Novel") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Newest") },
                                            onClick = {
                                                viewModel.handleHomeDropdownAction{
                                                    if (currentContentRoute != ContentRoutes.NEWEST) {
                                                        contentNavController.navigate(ContentRoutes.NEWEST) {
                                                            popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.FiberNew, "Newest tab") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ranking") },
                                            onClick = {
                                                viewModel.handleHomeDropdownAction{
                                                    if (currentContentRoute != ContentRoutes.RANKING) {
                                                        contentNavController.navigate(ContentRoutes.NEWEST) {
                                                            popUpTo(ContentRoutes.ILLUSTRATIONS) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.BarChart, "Ranking tab") }
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.setShowProfileMenu(true) }) {
                                    val user = currentUser
                                    if (user != null) {
                                        val mediumImageUrl = user.profileImageUrls?.medium
                                        if (mediumImageUrl.isNullOrEmpty() || mediumImageUrl == NO_PROFILE_IMAGE_URL) {
                                            Icon(
                                                imageVector = Icons.Filled.AccountCircle,
                                                contentDescription = "Profile Menu - Default Avatar",
                                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            PixivAsyncImage(
                                                imageUrl = mediumImageUrl,
                                                contentDescription = "Profile Menu button",
                                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = "Profile Menu - Logged Out",
                                            modifier = Modifier.size(32.dp).clip(CircleShape),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    content(PaddingValues())
                }

                if (isTextFieldFocused) {
                    BackHandler {
                        viewModel.clearSearchFocus()
                        focusManager.clearFocus()
                    }

                    // The overlay itself.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)) // A semi-transparent scrim
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // No ripple effect
                            ) {
                                // When the overlay is clicked, clear focus.
                                viewModel.clearSearchFocus()
                                focusManager.clearFocus()
                            }
                    )
                }
            }
            ProfileMenu(
                showMenu = showProfileMenu,
                onDismissRequest = {viewModel.setShowProfileMenu(false) },
                currentUserName = currentUser?.name ?: "Guest User",
                currentUserId = currentUser?.pixivId?.toString() ?: "N/A",
                currentUserAvatarUrl = currentUser?.profileImageUrls?.medium,
                currentUserNotificationCount = 0, // Placeholder
                onManagePixivAccountClick = {
                    viewModel.handleProfileMenuAction {
                        Toast.makeText(context, "Manage Pixiv Account Clicked", Toast.LENGTH_SHORT).show()
                    }
                },
                onNotificationsClick = {
                    viewModel.handleProfileMenuAction {
                        rootNavController.navigate("NotificationScreen")
                    }
                }
            )
        }
    } else if (loginState is LoginState.Loading || navigatingToLogin) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Loading user session...", modifier = Modifier.padding(top = 60.dp))
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This screen should disappear automatically ")
            Text("If not, please continue with the login or reopen the app.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = false)
@Composable
fun SearchFiled(){
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    //Searchable link or text
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            /*TODO search endpoint*/
                            searchQuery = it
                        },
                        modifier = Modifier.fillMaxWidth(),

                        textStyle = LocalTextStyle.current,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { /*TODO filter logic to be done*/ }) {
                                    Icon(
                                        painter = painterResource(R.drawable.filter_icon),
                                        //imageVector = Icons.Outlined.Tune,
                                        contentDescription = "Tune Icon"
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search or enter URL",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    // This is the actual, interactive text input field
                                    innerTextField()
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu Icon"
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        IconButton(
                            onClick = {/*TODO launching the dropdown menu*/}
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Home Icon"
                            )
                        }

                        IconButton(
                            onClick = {/*TODO account options}*/}
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = "Account Icon"
                            )
                        }
                    }
                }
            )
        }

    ){ innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding)
        ){

        }
    }
}