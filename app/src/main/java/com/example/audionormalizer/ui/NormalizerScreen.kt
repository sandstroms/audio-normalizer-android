package com.example.audionormalizer.ui

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
        val options = AudioLevel.entries.toTypedArray()

        when (uiState) {
            is NormalizerUiState.Default -> {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column {
                        Text("Select an audio level and then press \"Start\"")
                    }
                    Column(Modifier.selectableGroup()) {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = (option == normalizerViewModel.selectedOption),
                                        onClick = { normalizerViewModel.updateSelectedOption(option) },
                                        role = Role.RadioButton
                                    )
                            ) {
                                RadioButton(
                                    selected = (option == normalizerViewModel.selectedOption),
                                    onClick = null // null recommended for accessibility with screenreaders
                                )
                                Text(
                                    text = option.textDescription,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { normalizerViewModel.normalizeAudio(normalizerViewModel.selectedOption) }
                    ) {
                        Text("Start")
                    }
                }
            }
            is NormalizerUiState.Normalizing -> {
                Button(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    onClick = normalizerViewModel::cancelWork
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
        ) {
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