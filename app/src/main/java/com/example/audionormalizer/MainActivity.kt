package com.example.audionormalizer

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.audionormalizer.ui.theme.AudioNormalizerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.parcelize.Parcelize

class MainActivity : ComponentActivity() {

    private val visualizer = Visualizer(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioNormalizerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeatureThatRequiresRecordAudioPermission(visualizer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer.release()
    }
}

sealed interface MeasurementPeakRmsStatus {
    @Parcelize
    data class Success(val peak: Int, val rms: Int) : MeasurementPeakRmsStatus, Parcelable
    @Parcelize
    data object NotRecording : MeasurementPeakRmsStatus, Parcelable
    @Parcelize
    data object Error : MeasurementPeakRmsStatus, Parcelable
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun FeatureThatRequiresRecordAudioPermission(visualizer: Visualizer) {
    val recordAudioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    var isRecording by rememberSaveable { mutableStateOf(false) }
    var measurementPeakRmsStatus: MeasurementPeakRmsStatus by rememberSaveable { mutableStateOf(MeasurementPeakRmsStatus.NotRecording) }
    var peakText by rememberSaveable { mutableStateOf("") }
    var rmsText by rememberSaveable { mutableStateOf("") }

    if (recordAudioPermissionState.status.isGranted) {
        val recordButtonText = if (isRecording) "Stop recording" else "Record"
        peakText = when (measurementPeakRmsStatus) {
            is MeasurementPeakRmsStatus.NotRecording -> "Not recording"
            is MeasurementPeakRmsStatus.Error -> "Error, try again."
            is MeasurementPeakRmsStatus.Success -> {
                val peak = (measurementPeakRmsStatus as MeasurementPeakRmsStatus.Success).peak
                peak.toString()
            }
        }
        rmsText = when (measurementPeakRmsStatus) {
            is MeasurementPeakRmsStatus.NotRecording -> "Not recording"
            is MeasurementPeakRmsStatus.Error -> "Error, try again."
            is MeasurementPeakRmsStatus.Success -> {
                val rms = (measurementPeakRmsStatus as MeasurementPeakRmsStatus.Success).rms
                rms.toString()
            }
        }

        // May want to use later
//        val mediaPlayer = MediaPlayer(LocalContext.current)
        if (isRecording) {
            visualizer.enabled = true
            val measurementPeakRms = Visualizer.MeasurementPeakRms()
            visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS

            val getMeasurementPeakRmsResult = visualizer.getMeasurementPeakRms(measurementPeakRms)
            measurementPeakRmsStatus = if (getMeasurementPeakRmsResult == Visualizer.SUCCESS) {
                val peak = measurementPeakRms.mPeak
                val rms = measurementPeakRms.mRms
                if (rms < -7000) {
                    val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE)
                    if (audioManager is AudioManager) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }
                }
                MeasurementPeakRmsStatus.Success(peak, rms)
            } else {
                MeasurementPeakRmsStatus.Error
            }
        } else {
            visualizer.enabled = false
            measurementPeakRmsStatus = MeasurementPeakRmsStatus.NotRecording
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button({ isRecording = !isRecording }) {
                Text(recordButtonText)
            }
            Row {
                Text("The peak is:")
                Text(peakText)
            }
            Row {
                Text("The rms is:")
                Text(rmsText)
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
        }
    }
}