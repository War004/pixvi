package com.example.pixvi

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pixvi.login.AuthViewModel
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.repo.BatterySaverThemeRepository
import com.example.pixvi.screens.MainAppShell
import com.example.pixvi.screens.homeScreen.IllustrationsScreen
import com.example.pixvi.screens.homeScreen.MangaScreen
import com.example.pixvi.screens.homeScreen.NewestScreen
import com.example.pixvi.screens.homeScreen.NovelHomeScreen
import com.example.pixvi.screens.homeScreen.RankingScreen
import com.example.pixvi.settings.SettingsRepository
import com.example.pixvi.utils.ContentRoutes

@Composable
fun MainContentScreen(
    rootNavController: NavController,
    authViewModel: AuthViewModel,
    pixivApiService: PixivApiService,
    appViewModels: AppViewModels,
    onLogout: () -> Unit,
    settingRepo: SettingsRepository,
    batterySaverRepo: BatterySaverThemeRepository
) {
    // Create a NEW NavController for the nested graph
    val contentNavController = rememberNavController()

    MainAppShell(
        authViewModel = authViewModel,
        contentNavController = contentNavController,
        rootNavController = rootNavController,
        pixivApiService = pixivApiService,
        logoutEvent = onLogout,
        settingsRepository = settingRepo,
        batterySaverThemeRepository = batterySaverRepo
    ) { padding ->
        // NESTED NavHost for the actual content screens
        NavHost(
            navController = contentNavController,
            startDestination = ContentRoutes.ILLUSTRATIONS, // Default screen
            modifier = Modifier.padding(padding)
        ) {
            composable(ContentRoutes.ILLUSTRATIONS) {
                IllustrationsScreen(
                    navController = rootNavController,
                    homeIllustViewModel = appViewModels.homeIllustViewModel,
                    modifier = Modifier.padding(padding),
                )
            }
            composable(ContentRoutes.MANGA) {
                MangaScreen(
                    navController = rootNavController,
                    mangaViewModel = appViewModels.mangaViewModel,
                    modifier = Modifier.padding(padding),
                )
            }
            composable(ContentRoutes.NOVEL) {
                NovelHomeScreen(
                    navController = rootNavController,
                    homeINovelViewModel = appViewModels.homeINovelViewModel,
                    modifier = Modifier.padding(padding),
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
}