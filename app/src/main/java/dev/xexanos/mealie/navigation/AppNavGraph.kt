package dev.xexanos.mealie.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.xexanos.mealie.core.ui.navigation.NavigationManager
import dev.xexanos.mealie.feature.auth.navigation.AuthGraph
import dev.xexanos.mealie.feature.auth.navigation.authGraph
import dev.xexanos.mealie.feature.auth.ui.StartupScreen
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable object StartupRoute
@Serializable object PostAuthRoute

@Composable
fun AppNavGraph(navController: NavHostController) {
    val navigationManager: NavigationManager = koinInject()

    LaunchedEffect(Unit) {
        navigationManager.commands.collect {
            // Navigation routing implemented in Stories 1.3+
        }
    }

    NavHost(
        navController = navController,
        startDestination = StartupRoute
    ) {
        composable<StartupRoute> {
            StartupScreen(
                onNavigateToMain = {
                    navController.navigate(PostAuthRoute) {
                        popUpTo(StartupRoute) { inclusive = true }
                    }
                },
                onNavigateToCredentials = {
                    navController.navigate(AuthGraph) {
                        popUpTo(StartupRoute) { inclusive = true }
                    }
                },
                onNavigateToSetup = {
                    navController.navigate(AuthGraph) {
                        popUpTo(StartupRoute) { inclusive = true }
                    }
                },
            )
        }
        authGraph(
            navController = navController,
            onAuthComplete = {
                navController.navigate(PostAuthRoute) {
                    popUpTo(AuthGraph) { inclusive = true }
                }
            },
        )
        composable<PostAuthRoute> {
            Box(modifier = Modifier.fillMaxSize())
        }
        // shoppingGraph(navController) added in Story 2.1
    }
}
