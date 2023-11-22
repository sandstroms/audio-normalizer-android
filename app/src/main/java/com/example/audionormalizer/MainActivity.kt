package com.example.audionormalizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startForegroundService
import com.example.audionormalizer.ui.theme.AudioNormalizerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioNormalizerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeatureThatRequiresRecordAudioPermission()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FeatureThatRequiresRecordAudioPermission() {
    val recordAudioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    if (recordAudioPermissionState.status.isGranted) {
        var isRecording by rememberSaveable { mutableStateOf(false) }
        var isError by rememberSaveable { mutableStateOf(false) }
        var canErrorDialogOpen by rememberSaveable { mutableStateOf(true) }

        if (isError && canErrorDialogOpen) {
            AlertDialog(
                title = {
                    Text("Error")
                },
                text = {
                    Text("An error has occurred, please re-try recording.")
                },
                onDismissRequest = { canErrorDialogOpen = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isRecording = true
                            isError = false
                        }
                    ) {
                        Text("Retry")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            canErrorDialogOpen = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            )
        }

        val recordButtonText = if (isRecording && !isError) "Stop recording" else "Record"
        Button(
            onClick = { isRecording = !isRecording },
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
        ) {
            Text(recordButtonText)
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
        }
    }
}