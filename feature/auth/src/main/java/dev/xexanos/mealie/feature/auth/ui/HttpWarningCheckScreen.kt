package dev.xexanos.mealie.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun HttpWarningCheckScreen(
    onNavigateToCredentials: () -> Unit,
    viewModel: HttpWarningCheckViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.events, lifecycleOwner) {
        viewModel.events.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) {
                HttpWarningCheckUiEvent.NavigateToCredentials -> onNavigateToCredentials()
            }
        }
    }

    when (val state = uiState) {
        is HttpWarningCheckUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize())
        }
        is HttpWarningCheckUiState.ShowWarning -> {
            HttpWarningContent(
                serverUrl = state.serverUrl,
                onContinue = viewModel::onContinue,
            )
        }
    }
}

@Composable
private fun HttpWarningContent(
    serverUrl: String,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = serverUrl,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HttpWarningCheckTestTags.SERVER_URL_TEXT),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Connecting over your local network - this is common for self-hosted setups.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HttpWarningCheckTestTags.WARNING_MESSAGE),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(HttpWarningCheckTestTags.CONTINUE_BUTTON),
            ) {
                Text("Continue")
            }
        }
    }
}
