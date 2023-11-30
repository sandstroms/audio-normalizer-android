package com.example.audionormalizer

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import com.example.audionormalizer.data.AppContainer
import com.example.audionormalizer.data.DefaultAppContainer

class AudioNormalizerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}