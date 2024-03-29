package com.example.audionormalizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.audiofx.Visualizer
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.audionormalizer.CHANNEL_ID
import com.example.audionormalizer.NOTIFICATION_ID
import com.example.audionormalizer.NOTIFICATION_TITLE
import com.example.audionormalizer.R
import com.example.audionormalizer.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
import com.example.audionormalizer.VERBOSE_NOTIFICATION_CHANNEL_NAME
import com.example.audionormalizer.ui.AudioLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class NormalizerWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = ctx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val visualizer = Visualizer(0)

    private var currentMusicVolume: Int? = null
    private var currentRms: Int? = null
    private var averageRms: Double? = null
    private var noiseFactor: Double? = null
    private var totalRms = 0
    private var numMeasurements = 0

    // See https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running#long-running-kotlin
    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            val audioLevelInput = inputData.getString("AUDIO_LEVEL") ?: return Result.failure()
            normalizeAudio(audioLevelInput)
            Result.success()
        } catch (e: CancellationException) {
            visualizer.enabled = false
            visualizer.release()
            Result.failure()
        } catch(e: Exception) {
            // TODO: make sure the failure shows a snackbar
            visualizer.enabled = false
            visualizer.release()
            Result.failure(workDataOf("ERROR" to "INVALID_AUDIO_LEVEL"))
        }
    }

    private suspend fun normalizeAudio(audioLevel: String) {
        visualizer.enabled = true
        val measurementPeakRms = Visualizer.MeasurementPeakRms()
        visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS

        if (audioLevel == AudioLevel.DYNAMIC.textDescription) {
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
        } else {
            val lowerRange: Double
            val upperRange: Double
            when (audioLevel) {
                AudioLevel.LOW.textDescription -> {
                    lowerRange = -8700.0
                    upperRange = -6000.0
                }
                AudioLevel.MEDIUM.textDescription -> {
                    lowerRange = -6000.0
                    upperRange = -3000.0
                }
                AudioLevel.HIGH.textDescription -> {
                    lowerRange = -3000.0
                    upperRange = -1000.0
                }
                else -> {
                    throw Exception()
                }
            }

            return withContext(Dispatchers.IO) {
                while(true) {
                    visualizer.getMeasurementPeakRms(measurementPeakRms)
                    currentRms = measurementPeakRms.mRms

                    currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                    // Raise the volume if the current rms is below a certain threshold
                    if (((currentRms ?: 0) < lowerRange && (currentRms ?: 0) > -9600) &&
                        (currentMusicVolume ?: 0) < audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    ) {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_SHOW_UI
                        )
                        // Get the current music volume since it was adjusted
                        currentMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    // Lower the volume if the current rms is above a certain threshold
                    } else if (((currentRms ?: 0) > upperRange) &&
                        (currentMusicVolume ?: 0) > audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) + 1
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
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val cancel = "Cancel Normalizing"
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        createChannel()

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setTicker(NOTIFICATION_TITLE)
            .setContentText("currentRms: $currentRms")
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