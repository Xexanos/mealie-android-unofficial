package dev.xexanos.mealie.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.xexanos.mealie.core.ui.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartupScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: StartupAuthViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                StartupAuthEvent.NavigateToMain -> onNavigateToMain()
                StartupAuthEvent.NavigateToCredentials -> onNavigateToCredentials()
                StartupAuthEvent.NavigateToSetup -> onNavigateToSetup()
            }
        }
    }

    val loadingDescription = stringResource(R.string.startup_loading)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier
                .testTag(StartupTestTags.PROGRESS_INDICATOR)
                .semantics { contentDescription = loadingDescription },
        )
    }
}
