package com.example.audionormalizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Build
import androidx.annotation.RequiresApi
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

class NormalizerWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val visualizer = Visualizer(0)
    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        return withContext(Dispatchers.IO) {
            visualizer.enabled = true
            val measurementPeakRms = Visualizer.MeasurementPeakRms()
            visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
            var currentRms: Int
            var totalRms = 0
            var numMeasurements = 0
            var averageRms: Double

            repeat(1800) { times ->
                visualizer.getMeasurementPeakRms(measurementPeakRms)
                currentRms = measurementPeakRms.mRms
                totalRms += currentRms
                numMeasurements++
                averageRms = totalRms.toDouble() / numMeasurements
                val noiseFactor = -(400 * (-9600 / averageRms) + 500)

                if (times % 5 == 0) {
                    WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

                    val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
                    val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(CHANNEL_ID, name, importance)
                    channel.description = description

                    notificationManager.createNotificationChannel(channel)

                    val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setContentTitle(NOTIFICATION_TITLE)
                        .setTicker(NOTIFICATION_TITLE)
                        .setContentText("Average RMS: $averageRms, Noise factor: $noiseFactor")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setOngoing(true)
                        .build()

                    setForeground(ForegroundInfo(NOTIFICATION_ID, builder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK))

                }

                if ((currentRms < averageRms + noiseFactor && currentRms > -9600) || currentRms > averageRms - noiseFactor) {
                    if (currentRms < averageRms + noiseFactor) {
                        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) <
                            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        ) {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                    } else {
                        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) >
                            audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) + 1
                        ) {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI
                            )
                        }
                    }
                }

                delay(500)
            }
            visualizer.enabled = false
            Result.success()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createForegroundInfo(): ForegroundInfo {
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
        val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description

        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setTicker(NOTIFICATION_TITLE)
            .setContentText("Normalizing Audio")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, builder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }
}