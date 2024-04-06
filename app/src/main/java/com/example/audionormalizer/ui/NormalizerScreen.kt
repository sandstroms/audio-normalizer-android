package com.example.audionormalizer.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audionormalizer.receivers.AudioSessionReceiver
import com.example.audionormalizer.ui.theme.AudioNormalizerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresRecordAudioPermission(
    normalizerViewModel: NormalizerViewModel = viewModel(factory = NormalizerViewModel.Factory)
) {
    val audioSessionReceiver: BroadcastReceiver = AudioSessionReceiver(normalizerViewModel)
    val filter = IntentFilter(android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
    // TODO: move this into a lifecycle callback method
    ContextCompat.registerReceiver(LocalContext.current, audioSessionReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val postNotificationsPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val uiState by normalizerViewModel.normalizerUiState.collectAsStateWithLifecycle()
    val audioSessionState by normalizerViewModel.audioSessionState.collectAsState()

    if (recordAudioPermissionState.status.isGranted) {
        val options = AudioLevel.entries.toTypedArray()

        if (audioSessionState.audioSessionId != null) {
            when (uiState) {
                is NormalizerUiState.Default -> {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(64.dp)
                    ) {
                        Column {
                            Text(
                                text = "Select an audio level and then press \"Start\""
                            )
                        }
                        Column(
                            modifier = Modifier
                                .selectableGroup()
                                .padding(top = 25.dp, bottom = 12.dp)
                        ) {
                            options.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .selectable(
                                            selected = (option == normalizerViewModel.selectedOption),
                                            onClick = {
                                                normalizerViewModel.updateSelectedOption(
                                                    option
                                                )
                                            },
                                            role = Role.RadioButton
                                        )
                                ) {
                                    RadioButton(
                                        selected = (option == normalizerViewModel.selectedOption),
                                        onClick = null // null recommended for accessibility with screen readers
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
                            onClick = {
                                // It doesn't necessarily matter whether this notification is enabled, but it is more user-friendly since it reminds the user that the app is running
                                postNotificationsPermissionState.launchPermissionRequest()
                                normalizerViewModel.normalizeAudio(
                                    audioSessionState.audioSessionId!!,
                                    normalizerViewModel.selectedOption
                                )
                            },
                            modifier = Modifier.widthIn(min = 128.dp)
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
            Text(
                text = "Please start or restart your music player",
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
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

@Preview(showBackground = true)
@Composable
fun FeatureThatRequiresRecordAudioPermissionPreview() {
    AudioNormalizerTheme {
        FeatureThatRequiresRecordAudioPermission()
    }
}