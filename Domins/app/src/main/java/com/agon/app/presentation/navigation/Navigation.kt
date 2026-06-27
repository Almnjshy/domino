package com.agon.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.agon.app.domain.model.GameMode
import com.agon.app.presentation.screens.DemoScreen
import com.agon.app.presentation.screens.GameScreen
import com.agon.app.presentation.screens.MainMenuScreen
import com.agon.app.presentation.screens.NetworkScreen
import com.agon.app.presentation.screens.SettingsScreen
import com.agon.app.presentation.screens.SplashScreen
import com.agon.app.presentation.screens.StatsScreen
import com.agon.app.presentation.screens.VerificationScreen
import com.agon.app.presentation.viewmodel.GameViewModel
import com.agon.app.presentation.viewmodel.MenuViewModel
import com.agon.app.presentation.viewmodel.NetworkViewModel
import com.agon.app.presentation.viewmodel.SettingsViewModel
import com.agon.app.presentation.viewmodel.SplashViewModel
import com.agon.app.presentation.viewmodel.StatsViewModel

/**
 * Navigation routes with type safety
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Menu : Screen("menu")
    data object Game : Screen("game/{mode}") {
        fun createRoute(mode: GameMode) = "game/${mode.name}"
    }
    data object Network : Screen("network")
    data object Settings : Screen("settings")
    data object Stats : Screen("stats")
    data object Verify : Screen("verify")
    data object Demo : Screen("demo")

    companion object {
        fun fromRoute(route: String?): Screen {
            return when {
                route == null -> Splash
                route.startsWith("game/") -> Game
                route == "menu" -> Menu
                route == "network" -> Network
                route == "settings" -> Settings
                route == "stats" -> Stats
                route == "verify" -> Verify
                route == "demo" -> Demo
                else -> Splash
            }
        }
    }
}

/**
 * Main navigation with NavHost
 * Supports deep linking and process recreation
 */
@Composable
fun DominoNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    val context = LocalContext.current

    // Handle deep links on cold start
    LaunchedEffect(Unit) {
        val intent = (context as? android.app.Activity)?.intent
        val uri = intent?.data

        if (uri != null) {
            when (uri.path) {
                "/menu" -> navController.navigate(Screen.Menu.route) { popUpTo(0) }
                "/game" -> {
                    val mode = uri.getQueryParameter("mode")?.let { 
                        try { GameMode.valueOf(it.uppercase()) } catch (e: Exception) { null }
                    } ?: GameMode.HUMAN_VS_AI
                    navController.navigate(Screen.Game.createRoute(mode)) { popUpTo(0) }
                }
                "/network" -> navController.navigate(Screen.Network.route) { popUpTo(0) }
                "/settings" -> navController.navigate(Screen.Settings.route) { popUpTo(0) }
                "/stats" -> navController.navigate(Screen.Stats.route) { popUpTo(0) }
            }
            intent.data = null // Prevent re-processing
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        }
    ) {
        // Splash Screen
        composable(
            route = Screen.Splash.route,
            deepLinks = listOf(navDeepLink { uriPattern = "domino://splash" })
        ) {
            val splashViewModel: SplashViewModel = hiltViewModel()
            val state = splashViewModel.uiState.collectAsStateWithLifecycle()

            SplashScreen(
                progress = state.value.progress,
                isLoading = state.value.isLoading,
                error = state.value.error,
                onReady = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onRetry = { splashViewModel.retry() }
            )
        }

        // Main Menu
        composable(
            route = Screen.Menu.route,
            deepLinks = listOf(navDeepLink { uriPattern = "domino://menu" })
        ) {
            val menuViewModel: MenuViewModel = hiltViewModel()
            val state = menuViewModel.uiState.collectAsStateWithLifecycle()

            MainMenuScreen(
                selectedMode = state.value.selectedMode,
                isLoading = state.value.isLoading,
                error = state.value.error,
                onModeSelected = { menuViewModel.selectMode(it) },
                onNewGame = {
                    navController.navigate(Screen.Game.createRoute(state.value.selectedMode))
                },
                onNetwork = { navController.navigate(Screen.Network.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onStats = { navController.navigate(Screen.Stats.route) },
                onVerify = { navController.navigate(Screen.Verify.route) },
                onDemo = { navController.navigate(Screen.Demo.route) },
                onClearError = { menuViewModel.clearError() }
            )
        }

        // Game Screen
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = GameMode.HUMAN_VS_AI.name
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "domino://game?mode={mode}" }
            )
        ) { backStackEntry ->
            val modeName = backStackEntry.arguments?.getString("mode") ?: GameMode.HUMAN_VS_AI.name
            val gameMode = try {
                GameMode.valueOf(modeName)
            } catch (e: Exception) {
                GameMode.HUMAN_VS_AI
            }

            val gameViewModel: GameViewModel = hiltViewModel()
            val state = gameViewModel.uiState.collectAsStateWithLifecycle()

            // Auto-start game when entering
            LaunchedEffect(gameMode) {
                gameViewModel.newGame(gameMode)
            }

            GameScreen(
                gameState = state.value.gameState,
                isAiThinking = state.value.isAiThinking,
                showResult = state.value.showResult,
                error = state.value.error,
                onTileClick = { tile, side -> gameViewModel.playTile(tile, side) },
                onDrawOrPass = { gameViewModel.drawOrPass() },
                legalSides = { tile -> gameViewModel.getLegalSides(tile) },
                onNewGame = { gameViewModel.newGame(gameMode) },
                onBackToMenu = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Menu.route) { inclusive = false }
                    }
                },
                onDismissResult = { gameViewModel.dismissResult() },
                onClearError = { gameViewModel.clearError() }
            )
        }

        // Network Screen
        composable(
            route = Screen.Network.route,
            deepLinks = listOf(navDeepLink { uriPattern = "domino://network" })
        ) {
            val networkViewModel: NetworkViewModel = hiltViewModel()
            val state = networkViewModel.uiState.collectAsStateWithLifecycle()

            NetworkScreen(
                networkState = state.value.networkState,
                discoveredRooms = state.value.discoveredRooms,
                isLoading = state.value.isLoading,
                error = state.value.error,
                showCreateDialog = state.value.showCreateDialog,
                onCreateRoom = { networkViewModel.createRoom(it) },
                onDiscover = { networkViewModel.discoverRooms() },
                onJoinRoom = { room, name -> networkViewModel.joinRoom(room, name) },
                onLeaveRoom = { networkViewModel.leaveRoom() },
                onShowCreateDialog = { networkViewModel.showCreateDialog() },
                onDismissCreateDialog = { networkViewModel.dismissCreateDialog() },
                onBack = { navController.popBackStack() },
                onClearError = { networkViewModel.clearError() }
            )
        }

        // Settings Screen
        composable(
            route = Screen.Settings.route,
            deepLinks = listOf(navDeepLink { uriPattern = "domino://settings" })
        ) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val state = settingsViewModel.uiState.collectAsStateWithLifecycle()

            SettingsScreen(
                settings = state.value.settings,
                hasChanges = state.value.hasChanges,
                isLoading = state.value.isLoading,
                error = state.value.error,
                showResetConfirmation = state.value.showResetConfirmation,
                saveSuccess = state.value.saveSuccess,
                onVolumeChange = { settingsViewModel.updateVolume(it) },
                onEffectsToggle = { settingsViewModel.toggleEffects(it) },
                onVibrationToggle = { settingsViewModel.toggleVibration(it) },
                onLanguageChange = { settingsViewModel.setLanguage(it) },
                onModeChange = { settingsViewModel.setPreferredMode(it) },
                onSave = { settingsViewModel.saveSettings() },
                onReset = { settingsViewModel.resetSettings() },
                onShowResetConfirmation = { settingsViewModel.showResetConfirmation() },
                onDismissResetConfirmation = { settingsViewModel.dismissResetConfirmation() },
                onDismissSaveSuccess = { settingsViewModel.dismissSaveSuccess() },
                onBack = { navController.popBackStack() },
                onClearError = { settingsViewModel.clearError() }
            )
        }

        // Stats Screen
        composable(
            route = Screen.Stats.route,
            deepLinks = listOf(navDeepLink { uriPattern = "domino://stats" })
        ) {
            val statsViewModel: StatsViewModel = hiltViewModel()
            val state = statsViewModel.uiState.collectAsStateWithLifecycle()

            StatsScreen(
                stats = state.value.stats,
                achievements = state.value.achievements,
                isLoading = state.value.isLoading,
                error = state.value.error,
                showClearConfirmation = state.value.showClearConfirmation,
                exportedJson = state.value.exportedJson,
                showExportDialog = state.value.showExportDialog,
                onClearStats = { statsViewModel.clearStats() },
                onExport = { statsViewModel.exportStats() },
                onShowClearConfirmation = { statsViewModel.showClearConfirmation() },
                onDismissClearConfirmation = { statsViewModel.dismissClearConfirmation() },
                onDismissExportDialog = { statsViewModel.dismissExportDialog() },
                onRefresh = { statsViewModel.loadStats() },
                onBack = { navController.popBackStack() },
                onClearError = { statsViewModel.clearError() }
            )
        }

        // Demo Screen
        composable(route = Screen.Demo.route) {
            DemoScreen(
                onStartAi = {
                    navController.navigate(Screen.Game.createRoute(GameMode.HUMAN_VS_AI))
                },
                onCreateNetworkRoom = { navController.navigate(Screen.Network.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // Verification Screen
        composable(route = Screen.Verify.route) {
            VerificationScreen(
                onStartAi = {
                    navController.navigate(Screen.Game.createRoute(GameMode.HUMAN_VS_AI))
                },
                onNetwork = { navController.navigate(Screen.Network.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onStats = { navController.navigate(Screen.Stats.route) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
