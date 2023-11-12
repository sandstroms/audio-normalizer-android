package com.example.audionormalizer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.widget.Toast

class AudioNormalizerService : Service() {

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    private val visualizer = Visualizer(0)
    private var audioManager: AudioManager? = null

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            visualizer.enabled = true
            val measurementPeakRms = Visualizer.MeasurementPeakRms()
            visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS

            while (true) {
                if (audioManager != null) {
                    visualizer.getMeasurementPeakRms(measurementPeakRms)
                    val rms = measurementPeakRms.mRms
                    if ((rms < MIN_RMS && rms > -9600) || rms > -7000) {
                        if (rms < MIN_RMS) {
                            if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) <
                                    audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            ) {
                                audioManager!!.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_RAISE,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        } else {
                            if (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) >
                                    audioManager!!.getStreamMinVolume(AudioManager.STREAM_MUSIC) + 1
                            ) {
                                audioManager!!.adjustStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.ADJUST_LOWER,
                                    AudioManager.FLAG_SHOW_UI
                                )
                            }
                        }
                    }

                    // Wait for 500 milliseconds before continuing loop to avoid too much volume adjustment
                    Thread.sleep(500)
                }
            }
        }
    }

    override fun onCreate() {
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Toast.makeText(this, "Audio Normalizing starting", Toast.LENGTH_SHORT).show()

        audioManager = this.getSystemService(Context.AUDIO_SERVICE)
                as AudioManager

        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        visualizer.enabled = false
        visualizer.release()
        Toast.makeText(this, "Audio Normalizing stopped", Toast.LENGTH_SHORT).show()
    }
}