package dev.xexanos.mealie.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import dev.xexanos.mealie.core.ui.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun ServerUrlScreen(
    onNavigateToNext: () -> Unit,
    viewModel: ServerUrlViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel.events, lifecycleOwner) {
        viewModel.events.flowWithLifecycle(lifecycleOwner.lifecycle).collect { event ->
            when (event) {
                ServerUrlUiEvent.NavigateToNext -> onNavigateToNext()
            }
        }
    }

    if (uiState is ServerUrlUiState.Loading) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    var urlText by rememberSaveable { mutableStateOf("") }
    val probing = uiState as? ServerUrlUiState.Probing
    val isProbing = probing != null
    val inputError = uiState as? ServerUrlUiState.InputError

    LaunchedEffect(probing?.normalizedUrl, inputError?.lastUrl) {
        val stateUrl = probing?.normalizedUrl ?: inputError?.lastUrl
        if (stateUrl != null && stateUrl != urlText) {
            urlText = stateUrl
        }
    }

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

            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ServerUrlTestTags.URL_TEXT_FIELD),
                label = { Text(stringResource(R.string.setup_url_label)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { viewModel.onConnect(urlText) },
                ),
                singleLine = true,
                isError = inputError != null,
                enabled = !isProbing,
            )

            if (inputError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(inputError.messageResId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ServerUrlTestTags.ERROR_TEXT),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.onConnect(urlText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag(ServerUrlTestTags.CONNECT_BUTTON),
                enabled = !isProbing,
            ) {
                if (isProbing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag(ServerUrlTestTags.PROGRESS_INDICATOR),
                    )
                } else {
                    Text(stringResource(R.string.setup_button_connect))
                }
            }
        }
    }
}
