package dev.xexanos.mealie.feature.auth.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable object AuthGraph
@Serializable object AuthPlaceholder

fun NavGraphBuilder.authGraph(navController: NavController) {
    navigation<AuthGraph>(startDestination = AuthPlaceholder) {
        composable<AuthPlaceholder> {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
