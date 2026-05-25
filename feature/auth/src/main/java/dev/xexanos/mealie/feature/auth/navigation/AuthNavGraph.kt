package dev.xexanos.mealie.feature.auth.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import dev.xexanos.mealie.feature.auth.ui.ServerUrlScreen
import kotlinx.serialization.Serializable

@Serializable object AuthGraph
@Serializable object ServerUrlRoute
@Serializable object HttpWarningCheckRoute

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraph>(startDestination = ServerUrlRoute) {
        composable<ServerUrlRoute> {
            ServerUrlScreen(
                onNavigateToNext = {
                    navController.navigate(HttpWarningCheckRoute) {
                        popUpTo(ServerUrlRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<HttpWarningCheckRoute> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
