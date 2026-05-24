package dev.xexanos.mealie.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dev.xexanos.mealie.core.ui.navigation.NavigationManager
import dev.xexanos.mealie.feature.auth.navigation.AuthGraph
import dev.xexanos.mealie.feature.auth.navigation.authGraph
import org.koin.compose.koinInject

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
        startDestination = AuthGraph
    ) {
        authGraph(navController)
        // shoppingGraph(navController) added in Story 2.1
    }
}
