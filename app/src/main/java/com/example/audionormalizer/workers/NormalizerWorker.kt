package com.example.audionormalizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.Visualizer
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.audionormalizer.CHANNEL_ID
import com.example.audionormalizer.NOTIFICATION_ID
import com.example.audionormalizer.NOTIFICATION_TITLE
import com.example.audionormalizer.R
import com.example.audionormalizer.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
import com.example.audionormalizer.VERBOSE_NOTIFICATION_CHANNEL_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class NormalizerWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = ctx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val visualizer: Visualizer

    private var currentMusicVolume: Int? = null
    private var currentRms: Int? = null
    private var averageRms: Double? = null
    private var noiseFactor: Double? = null
    private var totalRms = 0
    private var numMeasurements = 0

    init {
        val audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                super.onPlaybackConfigChanged(configs)
                val newMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                // Reset the values since the user adjusted the volume
                if (currentMusicVolume != null && currentMusicVolume != newMusicVolume) {
                    currentRms = null
                    averageRms = null
                    noiseFactor = null
                    totalRms = 0
                    numMeasurements = 0
                }
            }
        }
        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null)

        visualizer = Visualizer(0)
    }

    // See https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running#long-running-kotlin
    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo(averageRms ?: 0.0, noiseFactor ?: 0.0))
            normalizeAudio()
            Result.success()
        } catch (e: CancellationException) {
            visualizer.enabled = false
            visualizer.release()
            Result.failure()
        }
    }

    private suspend fun normalizeAudio() {
        visualizer.enabled = true
        val measurementPeakRms = Visualizer.MeasurementPeakRms()
        visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS

        return withContext(Dispatchers.IO) {
            while(true) {
                visualizer.getMeasurementPeakRms(measurementPeakRms)
                currentRms = measurementPeakRms.mRms
                totalRms += currentRms ?: 0
                numMeasurements++
                averageRms = totalRms.toDouble() / numMeasurements
                noiseFactor = -(1000 * (-9600 / (averageRms ?: 0.0)) + 500)
                currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                // Raise the volume if the current rms is below a certain threshold
                if (((currentRms ?: 0) < (averageRms ?: 0.0) + (noiseFactor ?: 0.0) && (currentRms
                        ?: 0) > -9600) &&
                    (currentMusicVolume
                        ?: 0) < audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                ) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    // Get the current music volume since it was adjusted
                    currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    // Lower the volume if the current rms is above a certain threshold
                } else if (((currentRms ?: 0) > (averageRms ?: 0.0) - (noiseFactor ?: 0.0)) &&
                    (currentMusicVolume
                        ?: 0) > audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) + 1
                ) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    // Set the current music volume since it was adjusted
                    currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }

                delay(250)
            }
        }
    }

    private fun createForegroundInfo(avgRms: Double, noiseF: Double): ForegroundInfo {
        val cancel = "Cancel Normalizing"
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        createChannel()

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setTicker(NOTIFICATION_TITLE)
            .setContentText("Average RMS: $avgRms Noise factor: $noiseF")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, builder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun createChannel() {
        val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
        val descriptionText = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        notificationManager.createNotificationChannel(mChannel)
    }
}