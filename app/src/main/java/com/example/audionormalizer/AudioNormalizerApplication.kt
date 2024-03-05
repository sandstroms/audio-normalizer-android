package com.example.audionormalizer

import android.app.Application
import com.example.audionormalizer.data.AppContainer
import com.example.audionormalizer.data.DefaultAppContainer

class AudioNormalizerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}