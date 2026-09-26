package com.cryptic.piyek.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptic.piyek.R
import com.cryptic.piyek.presentation.components.FloatingImageInfoToolbar
import com.cryptic.piyek.presentation.components.NavigationMenu
import com.cryptic.piyek.presentation.components.SearchBar
import com.cryptic.piyek.presentation.components.VerticalHomeNavigator
import com.cryptic.piyek.core.content.data.model.Artwork
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel,
    iLLustHomeScreen: @Composable () -> Unit,
    mangaHomeScreen: @Composable () -> Unit,
    novelHomeScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    //backStack: Any,
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val activeArtwork by homeViewModel.currentActiveArtwork.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        activeArtwork = activeArtwork,
        destinationWindows = homeViewModel.destinationWindows,
        onAction = homeViewModel::action,
        iLLustHomeScreen = iLLustHomeScreen,
        mangaHomeScreen = mangaHomeScreen,
        novelHomeScreen = novelHomeScreen,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeScreenUiState,
    activeArtwork: Artwork?,
    destinationWindows: List<NavOptions>,
    onAction: (HomeScreenAction) -> Unit,
    iLLustHomeScreen: @Composable () -> Unit,
    mangaHomeScreen: @Composable () -> Unit,
    novelHomeScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Floating Navigator internal UI states
    var isNavigatorVisible by rememberSaveable { mutableStateOf(false) }
    var navigatorInteractionKey by remember { mutableLongStateOf(0L) }

    // Search focus & BackHandler lifecycle
    BackHandler(enabled = uiState.isSearchActive) {
        focusManager.clearFocus()
        onAction(HomeScreenAction.SearchStatusChange(false))
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (!uiState.isSearchActive) {
            delay(100L.milliseconds)
            focusManager.clearFocus()
        } else {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Auto-dismiss timer for VerticalHomeNavigator
    LaunchedEffect(isNavigatorVisible, navigatorInteractionKey) {
        if (isNavigatorVisible) {
            delay(3000L.milliseconds)
            isNavigatorVisible = false
        }
    }

    // Reset TopAppBar height offset on destination change
    LaunchedEffect(uiState.currentDestination) {
        if (scrollBehavior.state.heightOffset < 0f) {
            AnimationState(initialValue = scrollBehavior.state.heightOffset).animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 250)
            ) {
                scrollBehavior.state.heightOffset = value
            }
        }
    }

    NavigationMenu(
        modifier = modifier,
        onCardClick = { /* Handle Profile Card Click */ },
        onProfilePicClick = { /* Handle Avatar Click */ },
        onManageAccountsClick = { /* Handle Settings Click */ },
        onLogoutClick = {onAction(HomeScreenAction.LogOut)},
        currentAccount = uiState.currentAccount
    ) { onOpenDrawer ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    HomeTopBar(
                        uiState = uiState,
                        scrollBehavior = scrollBehavior,
                        focusRequester = focusRequester,
                        onOpenDrawer = onOpenDrawer,
                        onToggleNavigator = {
                            isNavigatorVisible = !isNavigatorVisible
                            navigatorInteractionKey++
                        },
                        onAction = onAction
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    HomeDestinationHost(
                        currentDestination = uiState.currentDestination,
                        activeArtwork = activeArtwork,
                        saveableStateHolder = saveableStateHolder,
                        iLLustHomeScreen = iLLustHomeScreen,
                        mangaHomeScreen = mangaHomeScreen,
                        novelHomeScreen = novelHomeScreen
                    )
                }

                SearchScrimOverlay(
                    isVisible = uiState.isSearchActive,
                    onDismiss = {
                        focusManager.clearFocus()
                        onAction(HomeScreenAction.SearchStatusChange(false))
                    }
                )
            }

            FloatingNavigatorOverlay(
                isVisible = isNavigatorVisible,
                currentDestination = uiState.currentDestination,
                availableWindows = destinationWindows,
                onWindowChange = { option ->
                    onAction(HomeScreenAction.NewUiMode(option))
                    navigatorInteractionKey++
                },
                onInteraction = { navigatorInteractionKey++ },
                boxConstraints = this
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    uiState: HomeScreenUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    focusRequester: FocusRequester,
    onOpenDrawer: () -> Unit,
    onToggleNavigator: () -> Unit,
    onAction: (HomeScreenAction) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            SearchBar(
                textFieldState = uiState.textFiledState,
                onSearch = {
                    onAction(HomeScreenAction.SearchStatusChange(false))
                },
                onFilterClick = {},
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onAction(HomeScreenAction.SearchStatusChange(true))
                        }
                    }
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                onAction(HomeScreenAction.ActiveAccountDetails)
                onOpenDrawer()
            }) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Menu Icon"
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleNavigator) {
                    Icon(
                        imageVector = when (uiState.currentDestination) {
                            is NavOptions.ILLust -> ImageVector.vectorResource(R.drawable.illust_represention)
                            is NavOptions.Manga -> ImageVector.vectorResource(R.drawable.manga_represention)
                            is NavOptions.Novel -> Icons.Outlined.Book
                        },
                        contentDescription = "Toggle navigation mode"
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun HomeDestinationHost(
    currentDestination: NavOptions,
    activeArtwork: Artwork?,
    saveableStateHolder: SaveableStateHolder,
    iLLustHomeScreen: @Composable () -> Unit,
    mangaHomeScreen: @Composable () -> Unit,
    novelHomeScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        saveableStateHolder.SaveableStateProvider(key = currentDestination::class.java.simpleName) {
            when (currentDestination) {
                is NavOptions.Novel -> {
                    novelHomeScreen()
                }
                else -> {
                    when (currentDestination) {
                        is NavOptions.Manga -> mangaHomeScreen()
                        is NavOptions.ILLust -> iLLustHomeScreen()
                    }
                    FloatingImageInfoToolbar(
                        artwork = activeArtwork,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchScrimOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
        )
    }
}

@Composable
private fun FloatingNavigatorOverlay(
    isVisible: Boolean,
    currentDestination: NavOptions,
    availableWindows: List<NavOptions>,
    onWindowChange: (NavOptions) -> Unit,
    onInteraction: () -> Unit,
    boxConstraints: BoxWithConstraintsScope,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { -it }),
        exit = fadeOut(),
        modifier = modifier
            .padding(
                top = boxConstraints.maxHeight * 0.30f,
                start = 25.dp
            )
    ) {
        VerticalHomeNavigator(
            currentWindow = currentDestination,
            availableWindow = availableWindows,
            onWindowChange = onWindowChange,
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            onInteraction()
                        }
                    }
                }
            }
        )
    }
}