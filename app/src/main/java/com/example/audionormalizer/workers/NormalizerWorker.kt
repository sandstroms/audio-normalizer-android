package com.example.audionormalizer.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.audiofx.Visualizer
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.audionormalizer.CHANNEL_ID
import com.example.audionormalizer.NOTIFICATION_ID
import com.example.audionormalizer.NOTIFICATION_TITLE
import com.example.audionormalizer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "NormalizerWorker"

class NormalizerWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val visualizer = Visualizer(0)
    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override suspend fun doWork(): Result {
        makeStatusNotification(
            "Normalizing Audio",
            applicationContext
        )

        return withContext(Dispatchers.IO) {
            visualizer.enabled = true
            val measurementPeakRms = Visualizer.MeasurementPeakRms()
            visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS

            visualizer.getMeasurementPeakRms(measurementPeakRms)
            val previousRms = measurementPeakRms.mRms

            delay(500)

            visualizer.getMeasurementPeakRms(measurementPeakRms)
            val currentRms = measurementPeakRms.mRms

            if ((currentRms < previousRms + 1000 && currentRms > -9600) || currentRms > previousRms - 1000) {
                if (currentRms < previousRms + 1000) {
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
            visualizer.enabled = false

            Result.success()
        }
    }
}

fun makeStatusNotification(message: String, context: Context) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(LongArray(0))

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
    }
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
}