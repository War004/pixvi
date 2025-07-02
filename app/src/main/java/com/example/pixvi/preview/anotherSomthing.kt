package com.example.pixvi.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.AppBarColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*



@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Menu() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "Helllo!"
            )
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 30.dp)
                .fillMaxWidth(),
            expandedShadowElevation = 8.dp,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                toolbarContainerColor = Color(0xFFB3D9FF)
            ),
            contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // The text column is scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Title........",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.clickable { /* Handle title click */ }
                )
                Text(
                    text = "Author",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.clickable { /* Handle artist click */ }
                )
            }

            // A Spacer to create a gap between text and icons
            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info Icon",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { /* Toggle favorite state here */ }
            )

            // A Spacer to create a gap between the two icons
            Spacer(modifier = Modifier.width(8.dp))

            // --- HEART ICON ---
            // To make this icon switch between filled and outlined, you would:
            // 1. Create a state variable: `var isFavorite by remember { mutableStateOf(false) }`
            // 2. In the `clickable` modifier, toggle the state: `isFavorite = !isFavorite`
            // 3. Use an if/else to change the `imageVector`:
            //    `imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder`
            // 4. You might also want to change the tint for the filled state:
            //    `tint = if (isFavorite) Color.Red else Color.Black`
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder, // Currently outlined
                contentDescription = "Favorite",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { /* Toggle favorite state here */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Corrected TopAppBar Preview")
@Composable
fun TopAppBarPreview() {
    var showHomeDropdownMenu by remember { mutableStateOf(false) }
    val mockIconPainter = rememberVectorPainter(image = Icons.Outlined.PhotoAlbum)

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* In preview, does nothing */ }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            Box {
                                IconButton(onClick = { showHomeDropdownMenu = !showHomeDropdownMenu }) {
                                    Icon(painter = mockIconPainter, contentDescription = "Selected screen icon")
                                }
                                DropdownMenu(
                                    expanded = showHomeDropdownMenu,
                                    onDismissRequest = { showHomeDropdownMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Illustrations") },
                                        onClick = { showHomeDropdownMenu = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Manga") },
                                        onClick = { showHomeDropdownMenu = false }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* In preview, does nothing */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* In preview, does nothing */ }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Screen Content Goes Here", modifier = Modifier.padding(16.dp))
            }
        }
    }
}