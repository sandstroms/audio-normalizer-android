package com.example.audionormalizer.ui

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FeatureThatRequiresRecordAudioPermission(
    normalizerViewModel: NormalizerViewModel = viewModel(factory = NormalizerViewModel.Factory)
) {
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val postNotificationsPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val uiState by normalizerViewModel.normalizerUiState.collectAsStateWithLifecycle()

    if (recordAudioPermissionState.status.isGranted && postNotificationsPermissionState.status.isGranted) {
        when (uiState) {
            is NormalizerUiState.Default -> {
                Button(
                    onClick = normalizerViewModel::normalizeAudio,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("Record")
                }
            }
            is NormalizerUiState.Recording -> {
                Button(
                    onClick = normalizerViewModel::cancelWork,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("Stop recording")
                }
            }
            is NormalizerUiState.Complete -> {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("Recording finished")
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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
            Text(textToShow)
            Button(onClick = { recordAudioPermissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
            Button(onClick = { postNotificationsPermissionState.launchPermissionRequest() }) {
                Text("Delete this, post notification permission")
            }
        }
    }
}