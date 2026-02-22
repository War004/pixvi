package com.cryptic.pixvi.appShell

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cryptic.pixvi.LocalAppSettings
import com.cryptic.pixvi.LocalImmersiveMode
import com.cryptic.pixvi.LocalSettingEvent
import com.cryptic.pixvi.R
import com.cryptic.pixvi.auth.account.AccountDetails
import com.cryptic.pixvi.core.config.PixivConfigs.NO_PROFILE_IMAGE_URL
import com.cryptic.pixvi.core.storage.AppSettings
import kotlinx.coroutines.launch
import com.cryptic.pixvi.printer.PdfInfo

/**
 * CompositionLocal for triggering PDF save from any screen inside the shell.
 * API 29+: saves via MediaStore (no picker). Below API 29: opens SAF picker.
 */
val LocalSavePdf = compositionLocalOf<(PdfInfo, List<Uri>) -> Unit> { { _, _ -> } }

@Composable
fun MainAppShell(
    viewModel: MainAppShellViewModel,
    onNotificationNav:() ->Unit,
    content: @Composable (PaddingValues) -> Unit
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val settings by viewModel.appSettings.collectAsStateWithLifecycle()

    val eventHandler: (SettingAction) -> Unit = viewModel::onDisplaySettingChanges

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading feed...")
            //freeze maybe due to the loading of refresh
        }
    }
    else {
        ShellView(
            currentScreen = uiState.currentScreen,
            textQuery = uiState.textQuery,
            showProfilemenu = uiState.showProfileDialog,
            accountDetails = uiState.accountInfo,
            showNavigationMenu = uiState.showNavigationMenu,
            showModalNavigationDrawer = uiState.showModalNavigationDrawer,
            isLoading = false,
            notification = 15,
            currentSettings = settings,
            currentSettingActions = eventHandler,
            onNewEvent = { event -> viewModel.onEvent(event) },
            onNavgation = onNotificationNav,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellView(
    currentScreen: Navigation,
    textQuery: String?,
    showProfilemenu: Boolean,
    accountDetails: AccountDetails?,
    showNavigationMenu: Boolean,
    showModalNavigationDrawer: Boolean,
    isLoading: Boolean,
    notification: Int,
    currentSettings: AppSettings,
    currentSettingActions: (SettingAction) -> Unit,
    onNewEvent: (UserActions) -> Unit,
    onNavgation:()->Unit,
    content: @Composable (PaddingValues) -> Unit
){

    var isImmersive by rememberSaveable { mutableStateOf(false) }

    // 1. Get the current window to manipulate System Bars
    /*
    LaunchedEffect(isImmersive) {
        activity?.let {
            val window = it.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            // Standard logic: If immersive, hide bars. If not, show them.
            if (isImmersive) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }*/

    CompositionLocalProvider(
        LocalSettingEvent provides currentSettingActions,
        LocalAppSettings provides currentSettings,
        LocalImmersiveMode provides {active -> isImmersive = active},
    ) {

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val drawerState = rememberDrawerState(initialValue = if(!showModalNavigationDrawer) DrawerValue.Closed else DrawerValue.Open)
        val scope = rememberCoroutineScope()
        val textFieldState = rememberTextFieldState(initialText = textQuery?:"")
        val scrollState = rememberScrollState()
        val focusManager = LocalFocusManager.current

        val interactionSource = remember { MutableInteractionSource() }
        val isTextFieldFocused by interactionSource.collectIsFocusedAsState()

        val iconPainter: Painter = when (currentScreen) {
            Navigation.HOME -> rememberVectorPainter(image = Icons.Filled.Home)
            Navigation.ILLUSTRATIONS  -> painterResource(R.drawable.imagesmode_24px)
            Navigation.MANGA -> rememberVectorPainter(image = Icons.Filled.PhotoAlbum)
            Navigation.NOVEL -> rememberVectorPainter(image = Icons.AutoMirrored.Filled.LibraryBooks)
            Navigation.NEWEST -> rememberVectorPainter(image = Icons.Filled.FiberNew)
            Navigation.RANKING -> rememberVectorPainter(image = Icons.Filled.BarChart)
        }

        LaunchedEffect(textFieldState.text) {
            onNewEvent((UserActions.OnQueryChange
                (textFieldState.text.toString()))
            )
        }

        if(!isLoading){

        }
        else{

        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isImmersive,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Section (Scrollable)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                            //.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                "Quick Options",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            NavigationDrawerItem(
                                label = { Text("Bookmarks") },
                                icon = { Icon(Icons.Outlined.Favorite, "Bookmarks") },
                                selected = false,
                                onClick = { /*TODO*/ }
                            )
                            NavigationDrawerItem(
                                label = { Text("History") },
                                icon = { Icon(Icons.Default.History, "History") },
                                selected = false,
                                onClick = {}
                            )
                            NavigationDrawerItem(
                                label = { Text("Settings") },
                                icon = { Icon(Icons.Outlined.Settings, "Settings") },
                                selected = false,
                                onClick = {}
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    if(isImmersive){
                        //do nothing 
                    }
                    else{
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
                                        IconButton(
                                            onClick = {/*show the dropdown menu for selecting screen*/}) {
                                            Icon(painter = iconPainter, contentDescription = "Selected screen icon")
                                        }
                                        DropdownMenu(
                                            expanded = showNavigationMenu,
                                            onDismissRequest = {
                                                onNewEvent(UserActions.ChangeNavMenuVisibility(!showModalNavigationDrawer))
                                            }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Illustrations") },
                                                onClick = {
                                                    /*
                                                    Change the Illustration page to the and navigation
                                                     */
                                                },
                                                leadingIcon = { Icon(painterResource(R.drawable.imagesmode_24px), "Illustrations") }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Manga") },
                                                onClick = {
                                                    /*
                                                    Navigating to the manga
                                                     */
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.PhotoAlbum, "Manga") }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Novel") },
                                                onClick = {
                                                    /*
                                                    Navigate to the novel screen
                                                     */
                                                },
                                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, "Novel") }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Newest") },
                                                onClick = {
                                                    /*
                                                    Navigate to the newest
                                                     */
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.FiberNew, "Newest tab") }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Ranking") },
                                                onClick = {
                                                    //Navigate to the ranking screen.
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.BarChart, "Ranking tab") }
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        onNewEvent(UserActions.ProfileBoxOpen(!showProfilemenu))
                                        //show the menu option
                                    }) {
                                        if ( accountDetails!= null) {
                                            if (accountDetails.profilePicUrlBig.isBlank() || accountDetails.profilePicUrlBig == NO_PROFILE_IMAGE_URL) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountCircle,
                                                    contentDescription = "Profile Menu - Default Avatar",
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = accountDetails.profilePicUrlBig,
                                                    contentDescription = "Profile Picture of the user ${accountDetails.accountName}",
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.AccountCircle,
                                                contentDescription = "Profile Menu - Logged Out",
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                    }
                }
            ){ paddingValues ->

                if(showProfilemenu && accountDetails != null){
                    ProfileDialogBox(
                        profileUrl = accountDetails.profilePicUrlBig,
                        userName = accountDetails.name,
                        userid = accountDetails.userId,
                        notificationValue = notification,
                        onNotificationPress = onNavgation,
                        onDismissRequest = {onNewEvent(UserActions.ProfileBoxOpen(false))}
                    )
                }
                content(paddingValues)

            }
        }
    }
}