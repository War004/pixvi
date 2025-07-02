package com.example.pixvi.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pixvi.R
import com.example.pixvi.AppViewModels
import com.example.pixvi.login.AuthViewModel
import com.example.pixvi.login.LoginState
import com.example.pixvi.network.response.AppLoading.CurrentAccountManager
import com.example.pixvi.screens.homeScreen.IllustrationsScreen
import com.example.pixvi.screens.homeScreen.MangaScreen
import com.example.pixvi.screens.homeScreen.NewestScreen
import com.example.pixvi.screens.homeScreen.NovelHomeScreen
import com.example.pixvi.screens.homeScreen.NovelScreen
import com.example.pixvi.screens.homeScreen.RankingScreen
import kotlinx.coroutines.launch


// Route definitions
object ContentRoutes {
    const val ILLUSTRATIONS = "illustrations"
    const val MANGA = "manga"
    const val NOVEL = "novel"
    const val NEWEST = "newest"
    const val RANKING = "ranking"
}

private const val NO_PROFILE_IMAGE_URL = "https://s.pximg.net/common/images/no_profile.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    authViewModel: AuthViewModel,
    rootNavController: NavController,
    //pixivApiService: PixivApiService = RetrofitClient.apiService,
    viewModels: AppViewModels
) {
    val TAG = "MainAppShell"

    Log.d(TAG, "MainAppShell composing - rootNavController: ${rootNavController.hashCode()}")
    val loginState by authViewModel.loginState.collectAsState()
    var showProfileMenu by remember { mutableStateOf(false) }
    var navigatingToLogin by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showHomeDropdownMenu by remember { mutableStateOf(false) }

    val contentNavController = rememberNavController()

    Log.d(TAG, "ContentNavController created: ${contentNavController.hashCode()}")

    var savedStartRoute by rememberSaveable { mutableStateOf(ContentRoutes.ILLUSTRATIONS) }




    // Observe the current route from the contentNavController
    val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
    val currentContentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentContentRoute) {
        if (currentContentRoute != null) {
            savedStartRoute = currentContentRoute
            Log.d(TAG, "Saved current route for recreation: $savedStartRoute")
        }
    }

    /*LaunchedEffect(currentContentRoute) {
        if (currentContentRoute != null) {
            savedRoute = currentContentRoute
            Log.d(TAG, "Saved current route for recreation: $savedRoute")
        }
    }*/

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
            navigatingToLogin = true
            rootNavController.navigate("LoginScreen") {
                popUpTo(rootNavController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        } else if (loginState is LoginState.Success) {
            navigatingToLogin = false
        }
    }

    if (loginState is LoginState.Success) {
        val currentUser by CurrentAccountManager.currentAccount.collectAsStateWithLifecycle()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ){
                        // --- DRAWER CONTENT ---
                        Text("Quick Options", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        // Illustrations Item in Drawer (optional, if you want it here too)
                        NavigationDrawerItem(
                            label = { Text("Illustrations") },
                            icon = { Icon(painterResource(R.drawable.imagesmode_24px), "Illustrations") },
                            selected = currentContentRoute == ContentRoutes.ILLUSTRATIONS,
                            onClick = {
                                scope.launch { drawerState.close() }
                                contentNavController.navigate(ContentRoutes.ILLUSTRATIONS) {
                                    popUpTo(contentNavController.graph.findStartDestination().id) {
                                        saveState = true // Good practice for drawer/bottom bar
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationDrawerItem(
                            label = {Text("Bookmarks")},
                            icon = {Icon(Icons.Default.FavoriteBorder,"Option to show bookmarks")},
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "Bookmarks Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = {Text("History")},
                            icon = {Icon(Icons.Default.History,"Option to show history")},
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "History Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = {Text("Filtered Work History")},
                            icon = {Icon(Icons.Default.HistoryToggleOff,"Option to show the list of filtered through custom tags")},
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "Filtered History Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = {Text("Settings")},
                            icon = {Icon(Icons.Default.Settings,"Option to show setting menu")},
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "Settings Clicked (TODO)", Toast.LENGTH_SHORT).show()
                                scope.launch { drawerState.close() }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Watchlisted works", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        NavigationDrawerItem(
                            label = {Text("Manga")},
                            icon = {Icon(Icons.Filled.PhotoAlbum,"Option to show following manga")},
                            selected = currentContentRoute == ContentRoutes.MANGA,
                            onClick = {
                                scope.launch { drawerState.close() }
                                contentNavController.navigate(ContentRoutes.MANGA) {
                                    popUpTo(contentNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
                                    popUpTo(contentNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
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
                        title = { Text("") },

                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) {
                                            drawerState.open()
                                        } else {
                                            drawerState.close()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }

                                Box {
                                    IconButton(onClick = { showHomeDropdownMenu = true }) {
                                        Icon(painter = iconPainter, contentDescription = "Selected screen icon")
                                    }
                                    DropdownMenu(
                                        expanded = showHomeDropdownMenu,
                                        onDismissRequest = { showHomeDropdownMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Illustrations") },
                                            onClick = {
                                                Log.d(TAG, "TopAppBar Dropdown: Navigating to Illustrations")
                                                showHomeDropdownMenu = false
                                                if (currentContentRoute != ContentRoutes.ILLUSTRATIONS) {
                                                    contentNavController.navigate(ContentRoutes.ILLUSTRATIONS) {
                                                        popUpTo(contentNavController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(painterResource(R.drawable.imagesmode_24px), "Illustrations") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Manga") },
                                            onClick = {
                                                Log.d(TAG, "TopAppBar Dropdown: Navigating to Manga")

                                                showHomeDropdownMenu = false
                                                if (currentContentRoute != ContentRoutes.MANGA) {
                                                    contentNavController.navigate(ContentRoutes.MANGA) {
                                                        popUpTo(contentNavController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.PhotoAlbum, "Manga") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Novel") },
                                            onClick = {
                                                showHomeDropdownMenu = false
                                                if (currentContentRoute != ContentRoutes.NOVEL) {
                                                    contentNavController.navigate(ContentRoutes.NOVEL) {
                                                        popUpTo(contentNavController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, "Novel") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Newest") },
                                            onClick = {
                                                showHomeDropdownMenu = false
                                                if (currentContentRoute != ContentRoutes.NEWEST) {
                                                    contentNavController.navigate(ContentRoutes.NEWEST) {
                                                        popUpTo(contentNavController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.FiberNew, "Newest tab") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ranking") },
                                            onClick = {
                                                showHomeDropdownMenu = false
                                                if (currentContentRoute != ContentRoutes.RANKING) {
                                                    contentNavController.navigate(ContentRoutes.RANKING) {
                                                        popUpTo(contentNavController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.BarChart, "Ranking tab") }
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                Toast.makeText(context, "Search clicked", Toast.LENGTH_SHORT).show()
                                Log.d("MainAppShell", "Search icon clicked (TODO: Implement)")
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { showProfileMenu = true }) {
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
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(mediumImageUrl)
                                                .crossfade(true)
                                                .placeholder(R.drawable.ic_launcher_background)
                                                .error(R.drawable.ic_launcher_background)
                                                .build(),
                                            contentDescription = "Profile Menu",
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
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { innerPadding ->
                Log.d(TAG, "NavHost composing with startDestination: $savedStartRoute")
                NavHost(
                    navController = contentNavController,
                    startDestination = savedStartRoute,// Default content screen
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                ) {
                    composable(ContentRoutes.ILLUSTRATIONS) {
                        Log.d(TAG, "Composing IllustrationsScreen")
                        IllustrationsScreen(
                            navController = rootNavController,
                            homeIllustViewModel = viewModels.homeIllustViewModel
                        )
                    }
                    composable(ContentRoutes.MANGA) {

                        Log.d(TAG, "Composing MangaScreen")
                        MangaScreen(
                            navController = rootNavController,
                            mangaViewModel = viewModels.mangaViewModel
                        )
                    }
                    composable(ContentRoutes.NOVEL) {
                        NovelHomeScreen(
                            navController = rootNavController,
                            homeINovelViewModel = viewModels.homeINovelViewModel
                        )
                    }
                    composable(ContentRoutes.NEWEST) {
                        NewestScreen(navController = rootNavController)
                    }
                    composable(ContentRoutes.RANKING) {
                        RankingScreen(navController = rootNavController)
                    }
                }
            }

            ProfileMenu(
                showMenu = showProfileMenu,
                onDismissRequest = { showProfileMenu = false },
                currentUserName = currentUser?.name ?: "Guest User",
                currentUserId = currentUser?.pixivId?.toString() ?: "N/A",
                currentUserAvatarUrl = currentUser?.profileImageUrls?.medium,
                currentUserNotificationCount = 0, // Placeholder
                onManagePixivAccountClick = {
                    showProfileMenu = false
                    Toast.makeText(context, "Manage Pixiv Account Clicked", Toast.LENGTH_SHORT).show()
                },
                onNotificationsClick = {
                    showProfileMenu = false
                    rootNavController.navigate("NotificationScreen")
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
            Text("Redirecting...")
        }
    }
}