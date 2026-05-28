package dev.xexanos.mealie.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import dev.xexanos.mealie.feature.auth.ui.CredentialScreen
import dev.xexanos.mealie.feature.auth.ui.HttpWarningCheckScreen
import dev.xexanos.mealie.feature.auth.ui.ServerUrlScreen
import kotlinx.serialization.Serializable

@Serializable object AuthGraph
@Serializable object ServerUrlRoute
@Serializable object HttpWarningCheckRoute
@Serializable object CredentialRoute

fun NavGraphBuilder.authGraph(navController: NavController, onAuthComplete: () -> Unit) {
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
            HttpWarningCheckScreen(
                onNavigateToCredentials = {
                    navController.navigate(CredentialRoute) {
                        popUpTo(HttpWarningCheckRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<CredentialRoute> {
            CredentialScreen(
                onNavigateToMain = onAuthComplete,
            )
        }
    }
}
