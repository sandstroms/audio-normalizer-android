package com.example.audionormalizer.ui

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresRecordAudioPermission(
    normalizerViewModel: NormalizerViewModel = viewModel(factory = NormalizerViewModel.Factory)
) {
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val uiState by normalizerViewModel.normalizerUiState.collectAsStateWithLifecycle()

    if (recordAudioPermissionState.status.isGranted) {
        when (uiState) {
            NormalizerUiState.Default, NormalizerUiState.Error -> {
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                // TODO: figure out how to show snackbar

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .padding(innerPadding)
                    ) {
                        Button(onClick = { normalizerViewModel.normalizeAudio(AudioLevel.LOW) }) {
                            Text("Low")
                        }
                        Button(onClick = { normalizerViewModel.normalizeAudio(AudioLevel.MEDIUM) }) {
                            Text("Medium")
                        }
                        Button(onClick = { normalizerViewModel.normalizeAudio(AudioLevel.HIGH) }) {
                            Text("High")
                        }
                        Button(
                            onClick = { normalizerViewModel.normalizeAudio(AudioLevel.DYNAMIC) }
                        ) {
                            Text("Dynamic")
                        }
                    }
                }
            }
            NormalizerUiState.Normalizing -> {
                Button(
                    onClick = normalizerViewModel::cancelWork,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("Stop normalizing")
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ){
            val textToShow = if (recordAudioPermissionState.status.shouldShowRationale) {
                // If the user has denied the permission but the rationale can be shown,
                // then gently explain why the app requires this permission
                "The ability to record audio is important for this app. Please grant permission."
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                "Record audio permission is required for this feature to be available. " +
                        "Please grant permission"
            }
            Text(
                text = textToShow,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = { recordAudioPermissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
        }
    }
}