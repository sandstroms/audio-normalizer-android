package com.example.audionormalizer.data

import android.content.Context

interface AppContainer {
    val audioNormalizerRepository: AudioNormalizerRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val audioNormalizerRepository = WorkManagerNormalizerRepository(context)
}