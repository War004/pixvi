package com.cryptic.piyek.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptic.piyek.R
import com.cryptic.piyek.components.FloatingImageInfoToolbar
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo

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
    val uiState by homeViewModel.activeArtwork.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val textFieldState = rememberTextFieldState(initialText ="")
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isTextFieldFocused by interactionSource.collectIsFocusedAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                                onClick = {/*show the dropdown menu for selecting screen*/}) {
                                //Icon(painter = iconPainter, contentDescription = "Selected screen icon")
                                Icon(
                                    imageVector = Icons.Outlined.QuestionMark,
                                    contentDescription = "Test"
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            iLLustHomeScreen()

            FloatingImageInfoToolbar(
                artwork = uiState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth(),
            )
        }
    }
}