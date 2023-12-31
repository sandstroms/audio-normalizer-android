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
    private val notificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        val visualizer = Visualizer(0)
        visualizer.enabled = true
        val measurementPeakRms = Visualizer.MeasurementPeakRms()
        visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS
        var currentRms: Int
        var totalRms = 0
        var numMeasurements = 0
        var averageRms: Double

        setForeground(createForegroundInfo())

        return withContext(Dispatchers.IO) {
            repeat(1800) {
                visualizer.getMeasurementPeakRms(measurementPeakRms)
                currentRms = measurementPeakRms.mRms
                totalRms += currentRms
                numMeasurements++
                averageRms = totalRms.toDouble() / numMeasurements
                val noiseFactor = -(1000 * (-9600 / averageRms) + 500)

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
            visualizer.release()

            Result.success()
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ForegroundInfo(NOTIFICATION_ID, builder, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else
            ForegroundInfo(NOTIFICATION_ID, builder)
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