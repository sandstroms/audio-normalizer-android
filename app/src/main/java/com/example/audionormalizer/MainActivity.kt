package com.example.audionormalizer

import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.audionormalizer.ui.theme.AudioNormalizerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.parcelize.Parcelize

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

sealed interface WaveformStatus {
    @Parcelize
    data object NotRecording : WaveformStatus, Parcelable
    @Parcelize
    data class Success(val waveformTextValue: String) : WaveformStatus, Parcelable
    @Parcelize
    data object Error : WaveformStatus, Parcelable
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FeatureThatRequiresRecordAudioPermission() {
    val recordAudioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    var isRecording by rememberSaveable { mutableStateOf(false) }
    var isFinished by rememberSaveable { mutableStateOf(false) }
    var waveformText by rememberSaveable { mutableStateOf("") }
    var waveformStatus: WaveformStatus by rememberSaveable { mutableStateOf(WaveformStatus.NotRecording) }

    if (recordAudioPermissionState.status.isGranted) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val recordButtonText = if (isRecording) "Stop recording" else "Record"
            Button({ isRecording = !isRecording }) {
                Text(recordButtonText)
            }
            waveformText = when (waveformStatus) {
                is WaveformStatus.NotRecording -> "Not recording"
                is WaveformStatus.Error -> "Error, please try again."
                is WaveformStatus.Success -> (waveformStatus as WaveformStatus.Success).waveformTextValue
            }
            Text(waveformText)
            Spacer(Modifier.size(128.dp))
            Button({ isFinished = true }) {
                Text("Finish")
            }
        }

        // May want to use later
//        val mediaPlayer = MediaPlayer(LocalContext.current)
        val visualizer = Visualizer(0)
        waveformStatus = if (isRecording) {
            visualizer.enabled = true
            val bytes = ByteArray(visualizer.captureSize)
            if (visualizer.getWaveForm(bytes) == 0) {
                WaveformStatus.Success(bytes[0].toString())
            } else {
                WaveformStatus.Error
            }
        } else {
            visualizer.enabled = false
            WaveformStatus.NotRecording
        }

        if (isFinished) {
            visualizer.release()
        }

        // Reference for later
//        val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE)
//        if (audioManager is AudioManager) {
//            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
//        }
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
                "Record audio permission required for this feature to be available. " +
                        "Please grant the permission"
            }
            Text(textToShow)
            Button(onClick = { recordAudioPermissionState.launchPermissionRequest() }) {
                Text("Request permission")
            }
        }
    }
}

@RequiresApi(34)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AudioNormalizerTheme {
        FeatureThatRequiresRecordAudioPermission()
    }
}