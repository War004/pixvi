package com.cryptic.piyek.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.cryptic.piyek.components.FloatingImageInfoToolbar
import com.cryptic.piyek.components.SearchBar
import com.cryptic.piyek.components.VerticalHomeNavigator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/*
Two parameters,
 -  viewModel factory
 - back stack
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    iLLustHomeScreen: @Composable () -> Unit,
    //mangaHomeVIewModel: @Composable () -> Unit,
    //novelHomeViewModel: @Composable () -> Unit,
    //backStack: Any,
){
    val activeArtwork by homeViewModel.activeArtwork.collectAsStateWithLifecycle()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    var isNavigatorVisible by rememberSaveable { mutableStateOf(false) }
    var navigatorInteractionKey by remember { mutableLongStateOf(0L) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = uiState.isSearchActive) {
        focusManager.clearFocus()
        homeViewModel.action(HomeScreenAction.SearchStatusChange(false))
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (!uiState.isSearchActive) {
            delay(100L.milliseconds)
            focusManager.clearFocus()
        }
    }

    //bring the keyboard back after configuration changes
    LaunchedEffect(Unit) {
        if (uiState.isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Auto-dismiss timer: 3 seconds of inactivity turns it back to false
    LaunchedEffect(isNavigatorVisible, navigatorInteractionKey) {
        if (isNavigatorVisible) {
            delay(3000L.milliseconds)
            isNavigatorVisible = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    SearchBar(
                        textFieldState = uiState.textFiledState,
                        onSearch = {
                            homeViewModel.action(HomeScreenAction.SearchStatusChange(false))
                        },
                        onFilterClick = {},
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    homeViewModel.action(HomeScreenAction.SearchStatusChange(true))
                                }
                            }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            /*TODO*/
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
                                onClick = {
                                    isNavigatorVisible = !isNavigatorVisible
                                    navigatorInteractionKey++
                                },
                            ) {
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
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            //decide which screen to show
            when (val destination = uiState.currentDestination) {
                is NavOptions.Novel -> {
                    Text("This should be the novel screen!")
                }
                else -> {
                    when (destination) {
                        is NavOptions.Manga -> Text("This should be the manga screen!")
                        is NavOptions.ILLust -> iLLustHomeScreen()
                    }
                    FloatingImageInfoToolbar(
                        artwork = activeArtwork,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            // Vertical Home Navigator floating panel
            AnimatedVisibility(
                visible = isNavigatorVisible,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = maxHeight * 0.30f, start = 25.dp)
            ) {
                VerticalHomeNavigator(
                    currentWindow = uiState.currentDestination,
                    availableWindow = homeViewModel.destinationWindows,
                    onWindowChange = { option ->
                        homeViewModel.action(HomeScreenAction.NewUiMode(option))
                        navigatorInteractionKey++
                    },
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.pressed }) {
                                    navigatorInteractionKey++
                                }
                            }
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = uiState.isSearchActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    // Tapping the dimmed area clears focus and dismisses the scrim
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            homeViewModel.action(HomeScreenAction.SearchStatusChange(false))
                        })
                    }
            )
        }
    }
}

@Composable
fun HomeScreenImpl(

){

}