package com.cryptic.piyek.feature.novel.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val PAGINATION_BUFFER = 5

@Composable
fun HomeNovelScreen(
    homeNovelViewModel: HomeNovelViewModel,
    modifier: Modifier = Modifier
){
    val uiState by homeNovelViewModel.uiState.collectAsStateWithLifecycle()

    HomeNovelFeed(
        uiState = uiState,
        onAction = homeNovelViewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun HomeNovelFeed(
    uiState: HomeNovelUiState,
    onAction:(HomeNovelScreenActions) -> Unit,
    modifier: Modifier = Modifier,
    rankingTitle: String = "Ranking"
){
    val listState = rememberLazyListState()

    LaunchedEffect(listState, uiState.status){
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            val isNearBottom =
                totalItemCount > 0 && lastVisibleItemIndex >= (totalItemCount - PAGINATION_BUFFER)
            val canLoadMore = uiState.status == ScreenState.Idle

            isNearBottom && canLoadMore
        }
            .distinctUntilChanged()
            .filter { shouldLoad -> shouldLoad }
            .collect {
                onAction(HomeNovelScreenActions.LoadMore)
            }
    }

    NovelHomeUI(
        uiState = uiState,
        listState = listState,
        onAction = onAction,
        modifier = modifier,
        rankingTitle = rankingTitle
    )
}

@Composable
private fun NovelHomeUI(
    uiState: HomeNovelUiState,
    listState: LazyListState,
    onAction: (HomeNovelScreenActions) -> Unit,
    modifier: Modifier = Modifier,
    rankingTitle: String = "Ranking",
){
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when{
            uiState.errorMessage != null && uiState.novelList.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = uiState.errorMessage,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {onAction(HomeNovelScreenActions.Retry)}) {
                        Text("Retry")
                    }
                }
            }

            uiState.status == ScreenState.Loading && uiState.novelList.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ){
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        all = 8.dp
                    )
                ) {
                    if(uiState.rankingList.isNotEmpty()){
                        item(key="ranking-carousel", contentType = "header"){
                            Text(rankingTitle)
                        }
                    }

                    items(
                        items = uiState.novelList,
                        key = { it.id },
                        contentType = { "novel_card" }
                    ) { novel ->
                        NovelCardItem(
                            novel = novel,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }
}
